package com.mybrickhub.brickpicker.utility

import android.annotation.SuppressLint
import android.util.Log
import com.mybrickhub.brickpicker.MyApplication
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

data class CategoryItem(
    val categoryId: Int,
    val categoryName: String,
    val parentId: Int,
)

object Categories {

    private var categoryItems: List<CategoryItem> = emptyList()
    private const val maxApiCalls: Int = 1
    private var apiCalls: Int = 0

    private const val KEY_CATEGORIES_COUNT_API_CALLS = "categories_count_api_calls"
    private const val KEY_CATEGORIES_COUNT_LAST_RESET = "categories_count_last_reset"
    private const val KEY_CATEGORIES_API = "categories_api"

    val sharedPreferences = MyApplication.sharedPreferences

    @SuppressLint("SimpleDateFormat")
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date1 = sdf.format(Date(time1))
        val date2 = sdf.format(Date(time2))
        return date1 == date2
    }

    private fun getCategoriesApi(callback: CategoriesApiCallback) {
        val apiUrl = "https://api.bricklink.com/api/store/v1/categories"
        Api. apiCall("GET", apiUrl, null, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                try {
                    val items = JSONArray(data.toString())
                    val itemList = mutableListOf<CategoryItem>()

                    for (i in 0 until items.length()) {
                        val jsonObject = items.getJSONObject(i)

                        val categoryId = jsonObject.optInt("category_id", 0)
                        val categoryName = jsonObject.optString("category_name", "")
                        val parentId = jsonObject.optInt("parent_id", 0)

                        // Erstelle ein Order-Objekt und füge es zur Liste hinzu
                        val item = CategoryItem(
                            categoryId,
                            categoryName,
                            parentId
                        )
                        itemList.add(item)
                    }
                    callback.onSuccess(itemList)
                } catch (e: Exception) {
                    callback.onFailure("Fehler beim Verarbeiten der API-Antwort")
                }
            }

            override fun onFailure(error: String) {
                callback.onFailure("API-Aufruf fehlgeschlagen: $error")
                Log.e("MeineKlasse", "API-Aufruf fehlgeschlagen: $error")
            }
        })
    }

    // API-Aufruf, um Farben abzurufen und zu speichern
    fun fetchCategories() {
        val lastResetDate = sharedPreferences.getLong(KEY_CATEGORIES_COUNT_LAST_RESET, 0)
        if (!isSameDay(lastResetDate, System.currentTimeMillis())) {
            // Wenn das letzte Reset nicht heute war, setzen Sie den Counter zurück
            sharedPreferences.edit()
                .putInt(KEY_CATEGORIES_COUNT_API_CALLS, apiCalls)
                .putLong(KEY_CATEGORIES_COUNT_LAST_RESET, System.currentTimeMillis())
                .apply()
        } else {
            apiCalls = sharedPreferences.getInt(KEY_CATEGORIES_COUNT_API_CALLS, 0)
        }

        if (apiCalls < maxApiCalls) {
            apiCalls++
            getCategoriesApi(object : CategoriesApiCallback {
                override fun onSuccess(data: List<CategoryItem>) {
                    categoryItems = data
                    sharedPreferences.edit()
                        .putInt(KEY_CATEGORIES_COUNT_API_CALLS, apiCalls)
                        .putString(KEY_CATEGORIES_API, Gson().toJson(categoryItems))
                        .apply()
                }

                override fun onFailure(error: String) {
                    // Hier können Sie mit dem Fehler umgehen
                }
            })
        }
    }

    // Funktion, um den categoryName basierend auf der categoryId zu ermitteln
    fun getCategoryName(categoryId: Int): String {

        categoryItems = Gson().fromJson(
            sharedPreferences.getString(KEY_CATEGORIES_API, ""),
            object : TypeToken<List<CategoryItem>>() {}.type
        ) ?: emptyList()

        return categoryItems.find { it.categoryId == categoryId }?.categoryName
            ?: "Unknown Category"
    }


}

interface CategoriesApiCallback {
    fun onSuccess(data: List<CategoryItem>)
    fun onFailure(error: String)
}


