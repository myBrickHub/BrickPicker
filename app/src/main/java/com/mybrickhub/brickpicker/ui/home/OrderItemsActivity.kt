package com.mybrickhub.brickpicker.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.mybrickhub.brickpicker.databinding.ActivityOrderItemsBinding
import com.mybrickhub.brickpicker.R
import com.mybrickhub.brickpicker.utility.Api
import com.mybrickhub.brickpicker.utility.Loading
import org.json.JSONArray
import org.json.JSONObject

class OrderItemsActivity : AppCompatActivity() {
    private var orderId: Int = -1
    private var orderStatusInt: Int = -1
    private lateinit var binding: ActivityOrderItemsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var inflater: LayoutInflater
    private lateinit var recyclerView: RecyclerView

    private var sortLevels = mutableListOf<String>()
    private var displayLevels = mutableListOf<String>()
    private val placeholder = "-"
    private val sortOptions2 = listOf(
        placeholder,
        "Category Name",
        "Color Name",
        "Comments",
        "Condition",
        "Item Name",
        "Item No",
        "Remarks"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order_items)
        binding = ActivityOrderItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.recyclerViewOrderItems)

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        inflater = LayoutInflater.from(this)

        orderId = intent.getIntExtra("ORDER_ID", -1)
        orderStatusInt = intent.getIntExtra("ORDER_STATUS_INT", -1)

        val ta =
            obtainStyledAttributes(R.style.BackgroundPrimary, intArrayOf(android.R.attr.background))
        val backgroundColor = ta.getColor(0, Color.BLACK)
        ta.recycle()
        supportActionBar?.setBackgroundDrawable(ColorDrawable(backgroundColor))
        supportActionBar?.title = "Items"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sortLevels = Gson().fromJson(sharedPreferences.getString(
            KEY_ITEMS_SORT_MODE,
            Gson().toJson(listOf("Item Name", "Color Name"))
        ), object : TypeToken<List<String>>() {}.type
        ) ?: mutableListOf()
        displayLevels = Gson().fromJson(sharedPreferences.getString(
            KEY_ITEMS_DISPLAY_MODE,
            Gson().toJson(listOf("Item Name", "Color Name"))
        ), object : TypeToken<List<String>>() {}.type
        ) ?: mutableListOf()

        binding.root.post {
            getItemsApi(orderId)

        }

    }

    private fun getItemsApi(orderId: Int) {
        Loading.showLoading(true, inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}/items"
        Api.apiCall("GET", apiUrl, null, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                val items = JSONArray(JSONArray(data.toString()).getJSONArray(0).toString())
                val itemList = mutableListOf<OrderItem>()

                for (i in 0 until items.length()) {
                    val jsonObject = items.getJSONObject(i)

                    val inventoryId = jsonObject.optInt("inventory_id", 0)
                    val itemNo = jsonObject.optJSONObject("item")?.optString("no", "") ?: ""
                    val itemName = jsonObject.optJSONObject("item")?.optString("name", "") ?: ""
                    val itemType = jsonObject.optJSONObject("item")?.optString("type", "") ?: ""
                    val itemCategoryId =
                        jsonObject.optJSONObject("item")?.optInt("category_id", 0) ?: 0
                    val colorId = jsonObject.optInt("color_id", 0)
                    val quantity = jsonObject.optInt("quantity", 0)
                    val newOrUsed = jsonObject.optString("new_or_used", "")
                    val completeness = jsonObject.optString("completeness", "")
                    val unitPrice = jsonObject.optString("unit_price", "")
                    val unitPriceFinal = jsonObject.optString("unit_price_final", "")
                    val dispUnitPrice = jsonObject.optString("disp_unit_price", "")
                    val dispUnitPriceFinal = jsonObject.optString("disp_unit_price_final", "")
                    val currencyCode = jsonObject.optString("currency_code", "")
                    val dispCurrencyCode = jsonObject.optString("disp_currency_code", "")
                    val description = jsonObject.optString("description", "")
                    val remarks = jsonObject.optString("remarks", "")

                    // Erstelle ein Order-Objekt und füge es zur Liste hinzu
                    val item = OrderItem(
                        inventoryId,
                        itemNo,
                        itemName,
                        itemType,
                        itemCategoryId,
                        colorId,
                        quantity,
                        newOrUsed,
                        completeness,
                        unitPrice,
                        unitPriceFinal,
                        dispUnitPrice,
                        dispUnitPriceFinal,
                        currencyCode,
                        dispCurrencyCode,
                        description,
                        remarks
                    )
                    itemList.add(item)
                }
                updateRecyclerView(itemList)
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }
        })
    }

    private fun updateRecyclerView(items: List<OrderItem>) {
        this.runOnUiThread {
            recyclerView.layoutManager = LinearLayoutManager(this)

            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(recyclerView)

            val buttonFinishPickText = if (orderStatusInt >= 6) {
                getString(R.string.closeItems)
            } else {
                getString(R.string.packed)
            }

            val itemAdapter =
                ItemAdapter(this, items, recyclerView, displayLevels, buttonFinishPickText)
            recyclerView.adapter = itemAdapter

            sort(sortLevels, displayLevels)

        }
    }

    private fun sort(sortLevels: List<String>, displayLevels: List<String>) {
        val itemAdapter = recyclerView.adapter as? ItemAdapter
        itemAdapter?.sort(sortLevels, displayLevels)
    }

    private fun putStatusApi(orderId: Int, orderStatus: String) {
        Loading.showLoading(true, inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}/status"
        val apiBody = JSONObject()
            .put("field", "status")
            .put("value", orderStatus)

        Api.apiCall("PUT", apiUrl, apiBody, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })
    }

    fun finishActivity() {
        if (orderStatusInt < 6) {
            putStatusApi(orderId, "PACKED")
            val resultIntent = Intent()
            resultIntent.putExtra("ORDER_STATUS", 6)
            setResult(Activity.RESULT_OK, resultIntent)
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.order_items, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                @Suppress("DEPRECATION")
                onBackPressed()
                true
            }

            R.id.action_finish -> {
                val saveSetting = sharedPreferences.getBoolean(KEY_ITEMS_PACKED_SAVE_SETTING, false)
                if (!saveSetting) {
                    this.runOnUiThread {
                        val builder = AlertDialog.Builder(this)
                        val inflater = layoutInflater
                        val dialogView = inflater.inflate(R.layout.check_box_remember_choice, null)
                        val checkBox =
                            dialogView.findViewById<CheckBox>(R.id.checkBoxRememberChoice)
                        builder.setView(dialogView)
                        builder.setTitle(getString(R.string.packedConfirmTitle))
                            .setMessage(getString(R.string.packedConfirm))
                            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                if (checkBox.isChecked) {
                                    sharedPreferences.edit().putBoolean(
                                        KEY_ITEMS_PACKED_SAVE_SETTING, true
                                    ).apply()
                                }
                                finishActivity()
                            }
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show()
                    }
                } else {
                    finishActivity()
                }

                true
            }

            R.id.action_sort -> {
                itemSortPopupView()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("InflateParams")
    private fun itemSortPopupView() {
        val popupView = inflater.inflate(R.layout.item_sort_popup, null)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions2)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinners = mutableListOf<Spinner>()
        val checkboxes = mutableListOf<CheckBox>()

        val spinnerIds = listOf(R.id.spinner1, R.id.spinner2, R.id.spinner3, R.id.spinner4, R.id.spinner5, R.id.spinner6)
        val checkboxIds = listOf(R.id.checkbox1, R.id.checkbox2, R.id.checkbox3, R.id.checkbox4, R.id.checkbox5, R.id.checkbox6)

        for (i in spinnerIds.indices) {
            val spinner = popupView.findViewById<Spinner>(spinnerIds[i])
            spinner.adapter = adapter

            if (i < sortLevels.size) {
                val selectedPosition = adapter.getPosition(sortLevels[i])
                if (selectedPosition != AdapterView.INVALID_POSITION) {
                    spinner.setSelection(selectedPosition)
                }
            }

            var selectedSpinner: Spinner? = null
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedSpinner = spinner
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

            val checkbox = popupView.findViewById<CheckBox>(checkboxIds[i])
            checkboxes.add(checkbox)

            if (spinner.selectedItem.toString() in displayLevels) {
                checkbox.isChecked = true
            }

            checkbox.setOnClickListener {
                if (selectedSpinner != null && selectedSpinner!!.selectedItem.toString() == placeholder) {
                    checkbox.isChecked = false
                } else {
                    if (checkboxes.count { it.isChecked } > 3) {
                        checkbox.isChecked = false
                        runOnUiThread {
                            Toast.makeText(this, "Max. 3", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            spinners.add(spinner)
        }


        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        val popupButtonSave = popupView.findViewById<Button>(R.id.popupButtonSave)
        popupButtonSave.setOnClickListener {

            try {
                val newSortLevels = mutableListOf<String>()
                val newDisplayLevels = mutableListOf<String>()

                for (i in spinnerIds.indices) {
                    val spinner = popupView.findViewById<Spinner>(spinnerIds[i])
                    val selectedText = spinner.selectedItem.toString()
                    if (selectedText != placeholder) {
                        newSortLevels.add(selectedText)
                    }

                    val checkbox = popupView.findViewById<CheckBox>(checkboxIds[i])
                    if (selectedText != placeholder && checkbox.isChecked) {
                        newDisplayLevels.add(selectedText)
                    }
                }

                sharedPreferences.edit()
                    .putString(KEY_ITEMS_SORT_MODE, Gson().toJson(newSortLevels)).apply()
                sortLevels = newSortLevels
                sharedPreferences.edit()
                    .putString(KEY_ITEMS_DISPLAY_MODE, Gson().toJson(newDisplayLevels)).apply()
                displayLevels = newDisplayLevels
                sort(newSortLevels, newDisplayLevels)

                popupWindow.dismiss()
            } catch (e: Exception) {
                runOnUiThread {
                    Snackbar.make(popupView, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
            catch (e: Exception) {
                runOnUiThread {
                    Snackbar.make(popupView, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
        val popupButtonCancel = popupView.findViewById<Button>(R.id.popupButtonCancel)
        popupButtonCancel.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    companion object {
        private const val KEY_ITEMS_SORT_MODE = "items_sort_mode"
        private const val KEY_ITEMS_DISPLAY_MODE = "items_display_mode"
        private const val KEY_ITEMS_PACKED_SAVE_SETTING = "items_packed_save_setting"
    }


}