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

data class ColorItem(
    val colorId: Int,
    val colorName: String,
    val colorCode: String,
    val colorType: String
)

object Colors {

    private var colorItems: List<ColorItem> = emptyList()
    private const val maxApiCalls: Int = 1
    private var apiCalls: Int = 0

    //private lateinit var sharedPreferences: SharedPreferences

    private const val KEY_COLORS_COUNT_API_CALLS = "colors_count_api_calls"
    private const val KEY_COLORS_COUNT_LAST_RESET = "colors_count_last_reset"
    private const val KEY_COLORS_API = "colors_api"

    val sharedPreferences = MyApplication.sharedPreferences

    @SuppressLint("SimpleDateFormat")
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date1 = sdf.format(Date(time1))
        val date2 = sdf.format(Date(time2))
        return date1 == date2
    }

    private fun getColorsApi(callback: ColorsApiCallback) {
        val apiUrl = "https://api.bricklink.com/api/store/v1/colors"
        Api.apiCall("GET", apiUrl, null, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                try {
                    val items = JSONArray(data.toString())
                    val itemList = mutableListOf<ColorItem>()

                    for (i in 0 until items.length()) {
                        val jsonObject = items.getJSONObject(i)

                        val colorId = jsonObject.optInt("color_id", 0)
                        val colorName = jsonObject.optString("color_name", "")
                        val colorCode = jsonObject.optString("color_code", "")
                        val colorType = jsonObject.optString("color_type", "")

                        // Erstelle ein Order-Objekt und füge es zur Liste hinzu
                        val item = ColorItem(
                            colorId,
                            colorName,
                            colorCode,
                            colorType
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
    fun fetchColors() {
        val lastResetDate = sharedPreferences.getLong(KEY_COLORS_COUNT_LAST_RESET, 0)
        if (!isSameDay(lastResetDate, System.currentTimeMillis())) {
            // Wenn das letzte Reset nicht heute war, setzen Sie den Counter zurück
            sharedPreferences.edit()
                .putInt(KEY_COLORS_COUNT_API_CALLS, apiCalls)
                .putLong(KEY_COLORS_COUNT_LAST_RESET, System.currentTimeMillis())
                .apply()
        } else {
            apiCalls = sharedPreferences.getInt(KEY_COLORS_COUNT_API_CALLS, 0)
        }

        if (apiCalls < maxApiCalls) {
            apiCalls++
            getColorsApi(object : ColorsApiCallback {
                override fun onSuccess(data: List<ColorItem>) {
                    colorItems = data
                    sharedPreferences.edit()
                        .putInt(KEY_COLORS_COUNT_API_CALLS, apiCalls)
                        .putString(KEY_COLORS_API, Gson().toJson(colorItems))
                        .apply()
                }

                override fun onFailure(error: String) {
                    // Hier können Sie mit dem Fehler umgehen
                }
            })
        }
    }

    // Funktion, um den colorName basierend auf der colorId zu ermitteln
    fun getColorName(colorId: Int): String {

        colorItems = Gson().fromJson(
            sharedPreferences.getString(KEY_COLORS_API, ""),
            object : TypeToken<List<ColorItem>>() {}.type
        ) ?: emptyList()

        return colorItems.find { it.colorId == colorId }?.colorName ?: ""
    }


}

interface ColorsApiCallback {
    fun onSuccess(data: List<ColorItem>)
    fun onFailure(error: String)
}


