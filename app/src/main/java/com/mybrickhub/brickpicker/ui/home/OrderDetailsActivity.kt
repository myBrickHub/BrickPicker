package com.mybrickhub.brickpicker.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mybrickhub.brickpicker.R
import com.mybrickhub.brickpicker.databinding.ActivityOrderDetailsBinding
import com.mybrickhub.brickpicker.utility.Api
import com.mybrickhub.brickpicker.utility.Country
import com.mybrickhub.brickpicker.utility.Format
import com.mybrickhub.brickpicker.utility.OrderStatus
import com.mybrickhub.brickpicker.utility.Settings
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.mybrickhub.brickpicker.utility.Loading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale


class OrderDetailsActivity : AppCompatActivity() {

    private var orderId: Int = -1
    private var orderStatusInt: Int = -1
    private var paymentStatusInt: Int = -1
    private var orderIsFiled: Boolean = false
    private lateinit var orderDetails: OrderDetails
    private lateinit var binding: ActivityOrderDetailsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var inflater: LayoutInflater
    private var messageItems: List<MessageItem> = emptyList()
    private var messages: String = ""
    private var translatePaymentStatus = ""
    private var textBuyerEmail = ""
    private lateinit var checkboxSetStatusShipped: CheckBox
    private lateinit var checkboxSendDriveThru: CheckBox
    private lateinit var checkboxPostFeedback: CheckBox
    private lateinit var checkboxFile: CheckBox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order_details)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        inflater = LayoutInflater.from(this)

        orderId = intent.getIntExtra("ORDER_ID", -1)
        orderStatusInt = intent.getIntExtra("ORDER_STATUS_INT", -1)
        orderIsFiled = intent.getBooleanExtra("ORDER_IS_FILED", false)
        paymentStatusInt = intent.getIntExtra("ORDER_PAYMENT_STATUS_INT", -1)


        val ta = obtainStyledAttributes(R.style.BackgroundPrimary, intArrayOf(android.R.attr.background))
        val backgroundColor = ta.getColor(0, Color.BLACK)
        ta.recycle()
        supportActionBar?.setBackgroundDrawable(ColorDrawable(backgroundColor))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Details"

        val swipeRefreshLayout: SwipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        // Lade die gespeicherten Filteroptionen
        if (!sharedPreferences.contains(Settings.KEY_DETAILS_CARD_FILTER_MODE)) {
            val allFilters = mutableSetOf("Order", "Cost", "User", "Payment", "Shipping", "Remarks", "Messages")
            if (!sharedPreferences.getBoolean(Settings.KEY_SETTING_DETAILS_MESSAGES, false)) {
                allFilters.remove("Messages")
            }
            sharedPreferences.edit().putStringSet(Settings.KEY_DETAILS_CARD_FILTER_MODE, allFilters).apply()

        } else if (!sharedPreferences.contains(Settings.KEY_DETAILS_CARD_FILTER_MODE_FINISH)) {
            val customFilters = setOf("Order", "Shipping", "Remarks")
            sharedPreferences.edit().putStringSet(Settings.KEY_DETAILS_CARD_FILTER_MODE_FINISH, customFilters).apply()
        }

        // Order
        val orderDetailsChooseView =
            findViewById<ImageView>(R.id.orderDetailsChooseView)
        val orderStatusTextView = findViewById<TextView>(R.id.textStatus)
        orderDetailsChooseView.setOnClickListener {
            createPopupChooseView(
                orderStatusTextView,
                if (paymentStatusInt == 1) {
                    arrayOf("PAID", "PACKED", "SHIPPED", "COMPLETED")}
                else {
                    arrayOf("PEDNING", "UPDATED", "PROCESSING", "READY",
                    "PAID", "PACKED", "SHIPPED", "COMPLETED")},
                ::putStatusApi
            )
        }
        // Cost
        val costDetailsArrowView = findViewById<ImageView>(R.id.costDetailsArrowView)
        var costDetailsIsExpanded = false
        costDetailsArrowView.setOnClickListener {
            costDetailsIsExpanded = !costDetailsIsExpanded
            val costDetailsTextViews: Array<TextView> = arrayOf(
                findViewById(R.id.textDispCostGrandtotalInCost),
                findViewById(R.id.textDispCostEtc1),
                findViewById(R.id.textDispCostEtc2),
                findViewById(R.id.textDispCostInsurance),
                findViewById(R.id.textDispCostCredit),
                findViewById(R.id.textDispCostCoupon),
                findViewById(R.id.textDispCostVatRate),
                findViewById(R.id.textDispCostVatAmount),
            )
            toggleCardExpansion(
                costDetailsTextViews,
                costDetailsArrowView,
                costDetailsIsExpanded
            )
        }
        // User
        val userDetailsIconView = findViewById<ImageView>(R.id.userDetailsIconView)
        userDetailsIconView.setOnClickListener {
            try {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(textBuyerEmail))
                }
                startActivity(emailIntent)
            } catch (e: Exception) {
                this.runOnUiThread {
                    Toast.makeText(this, "Keine E-Mail-Anwendung gefunden", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // Payment
        val paymentDetailsChooseView =
            findViewById<ImageView>(R.id.paymentDetailsChooseView)
        if (paymentStatusInt == 0) {
            paymentDetailsChooseView.visibility = View.VISIBLE
            val paymentStatusTextView = findViewById<TextView>(R.id.textPaymentStatus)
            paymentDetailsChooseView.setOnClickListener {
                createPopupChooseView(
                    paymentStatusTextView,
                    arrayOf(getString(R.string.paid), getString(R.string.not_paid)),
                    ::putPaymentApi
                )
            }
        } else {
            paymentDetailsChooseView.visibility = View.GONE
        }
        // Shipping
        val shippingDetailsArrowView = findViewById<ImageView>(R.id.shippingDetailsArrowView)
        var shippingDetailsIsExpanded = false
        shippingDetailsArrowView.setOnClickListener {
            shippingDetailsIsExpanded = !shippingDetailsIsExpanded
            val shippingDetailsTextViews: Array<TextView> = arrayOf(
                findViewById(R.id.textShippingAdressName)
            )
            toggleCardExpansion(
                shippingDetailsTextViews,
                shippingDetailsArrowView,
                shippingDetailsIsExpanded
            )
            val shippingCopyView: ImageView = findViewById(R.id.shippingCopyView)
            if (shippingDetailsIsExpanded) { shippingCopyView.visibility = View.VISIBLE } else { shippingCopyView.visibility = View.GONE }
        }



        // Remarks
        val remarksDetailsEditView = findViewById<ImageView>(R.id.remarksDetailsEditView)
        val textRemarks = findViewById<TextView>(R.id.textRemarks)
        remarksDetailsEditView.setOnClickListener {
            createPopupEditView(textRemarks, "My Remarks", ::putRemarksApi)
        }

        // Messages
        if (sharedPreferences.getBoolean(Settings.KEY_SETTING_DETAILS_MESSAGES, false)) {
            val cardMessages = findViewById<View>(R.id.cardMessages)
            cardMessages.visibility = View.VISIBLE

            getMessagesApi(object : MessagesApiCallback {
                override fun onSuccess(data: List<MessageItem>) {
                    messageItems = data
                    val messagesStringBuilder = StringBuilder()
                    for ((index, messageItem) in messageItems.withIndex()) {
                        val convertedDate =
                            Format.convertUtcToLocal(messageItem.messageDateSent, "MMM d, yyyy HH:mm")
                        val messageContent = """
From: ${messageItem.messageFrom}
Subject: ${messageItem.messageSubject}
Date: $convertedDate
${messageItem.messageBody}
""".trimIndent()
                        messagesStringBuilder.append(messageContent)
                        if (index < messageItems.size - 1) {
                            messagesStringBuilder.append("\n\n")
                        }
                    }
                    messages = messagesStringBuilder.toString()
                }

                override fun onFailure(error: String) {
                    // Hier können Sie mit dem Fehler umgehen
                }
            })

        }

        // Pick Order
        val buttonPickOrder = findViewById<Button>(R.id.buttonPickOrder)
        buttonPickOrder.setOnClickListener {
            val intent = Intent(this, OrderItemsActivity::class.java)
            intent.putExtra("ORDER_ID", orderId)
            intent.putExtra("ORDER_STATUS_INT", orderStatusInt)
            intent.putExtra("ORDER_IS_FILED", orderIsFiled)
            getResult.launch(intent)
        }


        val buttonFinishOrder = findViewById<Button>(R.id.buttonFinishOrder)
        buttonFinishOrder.setOnClickListener {
            if (translatePaymentStatus == getString(R.string.not_paid)) {
                this.runOnUiThread {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.notPaidConfirmTitle))
                        .setMessage(getString(R.string.continueConfirm))
                        .setPositiveButton(getString(R.string.yes)) { _, _ ->
                            finishOrder()
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                }
            } else {
                finishOrder()
            }
        }
        //Finish
        checkboxSetStatusShipped = findViewById(R.id.checkboxSetStatusShipped)
        checkboxSendDriveThru = findViewById(R.id.checkboxSendDriveThru)
        checkboxPostFeedback = findViewById(R.id.checkboxPostFeedback)
        checkboxFile = findViewById(R.id.checkboxFile)
        checkboxSetStatusShipped.isChecked = sharedPreferences.getBoolean(KEY_SET_STATUS_SHIPPED, true)
        checkboxSendDriveThru.isChecked = sharedPreferences.getBoolean(KEY_SEND_DRIVE_THRU, true)
        checkboxPostFeedback.isChecked = sharedPreferences.getBoolean(KEY_POST_FEEDBACK, true)
        checkboxFile.isChecked = sharedPreferences.getBoolean(KEY_FILE, true)


        updateSavedFilter(orderStatusInt)

        // Api exceeded warning
        val loadedInt = sharedPreferences.getInt(Settings.KEY_API_COUNTS, -1)
        val apiLimit = sharedPreferences.getInt(Settings.KEY_CUSTOM_API_LIMIT, getString(R.string.standardApiLimit).toInt())
        val warnedToday = sharedPreferences.getBoolean(Settings.KEY_API_LIMIT_WARNING_NOTICED, false)
        if (loadedInt >= apiLimit && !warnedToday) {
            runOnUiThread {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.apiLimitExceeded))
                    .setNegativeButton(getString(R.string.ok), null)
                    .show()
            }
            sharedPreferences.edit()
                .putBoolean(Settings.KEY_API_LIMIT_WARNING_NOTICED, true).apply()
        }

        binding.root.post {
            getOrderApi(orderId)
        }

    }

    private fun finishOrder() {
        if (checkboxSetStatusShipped.isChecked) {
            orderStatusInt = 7
            putStatusApi(orderId, "SHIPPED")
        }
        if (checkboxSendDriveThru.isChecked) {
            postDriveThruApi()
        }
        if (checkboxPostFeedback.isChecked) {
            postFeedbackApi()
        }
        if (checkboxFile.isChecked) {
            val isFiled = true
            putOrderCustomApi(isFiled)
            orderIsFiled = isFiled
        }

        sharedPreferences.edit()
            .putBoolean(KEY_SET_STATUS_SHIPPED, checkboxSetStatusShipped.isChecked).apply()
        sharedPreferences.edit()
            .putBoolean(KEY_SEND_DRIVE_THRU, checkboxSendDriveThru.isChecked).apply()
        sharedPreferences.edit()
            .putBoolean(KEY_POST_FEEDBACK, checkboxPostFeedback.isChecked).apply()
        sharedPreferences.edit().putBoolean(KEY_FILE, checkboxFile.isChecked).apply()

        val resultIntent = Intent()
        resultIntent.putExtra("ORDER_ID", orderId)
        resultIntent.putExtra("ORDER_STATUS_INT", orderStatusInt)
        resultIntent.putExtra("ORDER_IS_FILED", orderIsFiled)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK && orderStatusInt < 6) {
                orderStatusInt = it.data?.getIntExtra("ORDER_STATUS", -1)!!
                statusChange(orderStatusInt)
            }
        }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra("ORDER_ID", orderId)
        resultIntent.putExtra("ORDER_STATUS_INT", orderStatusInt)
        resultIntent.putExtra("ORDER_IS_FILED", orderIsFiled)
        setResult(Activity.RESULT_OK, resultIntent)
        super.onBackPressed()
    }

    private fun putStatusApi(orderId: Int, orderStatus: String) {
        Loading.showLoading(true, inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}/status"
        val apiBody = JSONObject()
            .put("field", "status")
            .put("value", orderStatus)

        Api.apiCall("PUT", apiUrl, apiBody, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                statusChange(OrderStatus.translate(orderStatus)!!)
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })
    }

    private fun putOrderCustomApi(isFiled: Boolean) {
        Loading.showLoading(true, inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}"
        val apiBody = JSONObject()

        if (isFiled) {
            apiBody.put("is_filed", isFiled)
        }

        Api.apiCall("PUT", apiUrl, apiBody, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })
    }

    private fun postFeedbackApi() {
        Loading.showLoading(true, inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/feedback"
        val feedbackText = sharedPreferences.getString(Settings.KEY_CUSTOM_FEEDBACK,
            getString(R.string.standardFeedback))
        val apiBody = JSONObject()
            .put("order_id", orderId)
            .put("rating", 0)
            .put("comment", feedbackText)

        Api.apiCall("POST", apiUrl, apiBody, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })
    }

    private fun postDriveThruApi() {
        Loading.showLoading(true, inflater)
        var apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}/drive_thru"
        val apiBody = JSONObject()

        if (sharedPreferences.getBoolean(Settings.KEY_SETTING_FILE_CC, false)) {
            apiUrl += "?mail_me=true"
        }

        Api.apiCall("POST", apiUrl, apiBody, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })
    }

    private fun updateSavedFilter(orderStatusInt: Int) {
        val statusBasedFilter = if (orderStatusInt == 6) {
            Settings.KEY_DETAILS_CARD_FILTER_MODE_FINISH
        } else {
            Settings.KEY_DETAILS_CARD_FILTER_MODE
        }

        val savedFilters =
            sharedPreferences.getStringSet(statusBasedFilter, null)?.toMutableSet()
                ?: mutableSetOf()

        if (!sharedPreferences.getBoolean(Settings.KEY_SETTING_DETAILS_MESSAGES, false)) {
            savedFilters.remove("Messages")
        }

        applyCardFilters(savedFilters.toList())
    }

    private fun statusChange(orderStatusInteger: Int) {
        runOnUiThread{
            val cardOnFinish = findViewById<View>(R.id.cardOnFinish)
            if (orderStatusInteger == 6) {
                supportActionBar?.title = "Packed"
                cardOnFinish.visibility = View.VISIBLE
            } else {
                supportActionBar?.title = "Details"
                cardOnFinish.visibility = View.GONE
            }
            orderStatusInt = orderStatusInteger
            updateSavedFilter(orderStatusInteger)
            val orderStatusString = OrderStatus.translate(orderStatusInteger)
            val textStatus: TextView = this.findViewById(R.id.textStatus)
            textStatus.text = orderStatusString
            textStatus.setTextColor(
                ContextCompat.getColor(
                    this,
                    OrderStatus.color(orderStatusInteger)!!
                )
            )

            val buttonPickOrder = findViewById<Button>(R.id.buttonPickOrder)
            val buttonFinishOrder = findViewById<Button>(R.id.buttonFinishOrder)
            if (orderStatusInteger < 6 ){
                //ready to pick
                buttonPickOrder.visibility = View.VISIBLE
                buttonFinishOrder.visibility = View.GONE
                //correct shipping
                val shippingDetailsArrowView = findViewById<ImageView>(R.id.shippingDetailsArrowView)
                val shippingDetailsIsExpanded = false
                val shippingDetailsTextViews: Array<TextView> = arrayOf(
                    findViewById(R.id.textShippingAdressName)
                )
                toggleCardExpansion(
                    shippingDetailsTextViews,
                    shippingDetailsArrowView,
                    shippingDetailsIsExpanded
                )
                val shippingCopyView: ImageView = findViewById(R.id.shippingCopyView)
                if (shippingDetailsIsExpanded) { shippingCopyView.visibility = View.VISIBLE } else { shippingCopyView.visibility = View.GONE }
            } else if (orderStatusInteger == 6) {
                //packed -> finish
                buttonPickOrder.visibility = View.GONE
                buttonFinishOrder.visibility = View.VISIBLE
                //expand shipping
                val shippingDetailsArrowView = findViewById<ImageView>(R.id.shippingDetailsArrowView)
                val shippingDetailsIsExpanded = true
                val shippingDetailsTextViews: Array<TextView> = arrayOf(
                    findViewById(R.id.textShippingAdressName)
                    )
                toggleCardExpansion(
                    shippingDetailsTextViews,
                    shippingDetailsArrowView,
                    shippingDetailsIsExpanded
                )
            } else {
                buttonPickOrder.visibility = View.VISIBLE
                buttonFinishOrder.visibility = View.GONE
                buttonPickOrder.text = getString(R.string.viewItems)
                //correct shipping
                val shippingDetailsArrowView = findViewById<ImageView>(R.id.shippingDetailsArrowView)
                val shippingDetailsIsExpanded = false
                val shippingDetailsTextViews: Array<TextView> = arrayOf(
                    findViewById(R.id.textShippingAdressName)
                )
                toggleCardExpansion(
                    shippingDetailsTextViews,
                    shippingDetailsArrowView,
                    shippingDetailsIsExpanded
                )
                val shippingCopyView: ImageView = findViewById(R.id.shippingCopyView)
                if (shippingDetailsIsExpanded) { shippingCopyView.visibility = View.VISIBLE } else { shippingCopyView.visibility = View.GONE }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun createPopupChooseView(
        textContentView: TextView,
        statusOptions: Array<String>,
        apiCall: (Int, String) -> Unit
    ) {
        val popupView = inflater.inflate(R.layout.popup_choose, null)

        val statusRadioGroup = popupView.findViewById<RadioGroup>(R.id.statusRadioGroup)
        for ((index, status) in statusOptions.withIndex()) {
            val radioButton = RadioButton(this)
            radioButton.text = status.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            radioButton.id = index
            statusRadioGroup.addView(radioButton)

            if (status.equals(textContentView.text.toString(), true)) {
                radioButton.isChecked = true
            }
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
                val selectedRadioButtonId = statusRadioGroup.checkedRadioButtonId
                val selectedRadioButton = popupView.findViewById<RadioButton>(selectedRadioButtonId)
                val selectedStatus = selectedRadioButton.text.toString()

                if (!selectedStatus.equals(textContentView.text.toString(), true)) {
                    apiCall(orderId, selectedStatus)
                    runOnUiThread {
                        if (statusOptions.firstOrNull()?.all { it.isUpperCase() } == true) {
                            textContentView.text = selectedStatus.uppercase()
                        } else {
                            textContentView.text = selectedStatus
                        }
                    }
                }

                popupWindow.dismiss()
            } catch (e: Exception) {
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


    @SuppressLint("InflateParams")
    private fun createPopupEditView(
        textContentView: TextView,
        popupEditTitle: String,
        apiCall: (Int, String) -> Unit
    ) {
        val popupView = inflater.inflate(R.layout.popup_edit, null)

        val popupEditTextInputLayout = popupView.findViewById<TextInputLayout>(R.id.popupEditTextInputLayout)
        popupEditTextInputLayout.hint = popupEditTitle

        val popupDetailsEditText = popupView.findViewById<EditText>(R.id.popupEditTextContent)
        popupDetailsEditText.setText(textContentView.text.toString())
        popupDetailsEditText.requestFocus()

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        popupWindow.isFocusable = true
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        var newTextContent: String

        val popupButtonSave = popupView.findViewById<Button>(R.id.popupButtonSave)
        popupButtonSave.setOnClickListener {
            newTextContent = popupDetailsEditText.text.toString()

            try {
                if (textContentView.text.toString() != newTextContent) {
                    apiCall(orderId, newTextContent)
                    runOnUiThread {
                        textContentView.text = newTextContent
                    }
                }

                popupWindow.dismiss()
            } catch (e: Exception) {
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

    private fun toggleCardExpansion(
        userDetailsTextViews: Array<TextView>,
        arrowImageView: ImageView,
        expand: Boolean
    ) {
        if (expand) {
            userDetailsTextViews.forEach {
                it.visibility = if (it.text.isEmpty()) View.GONE else View.VISIBLE
            }
            animateArrow(arrowImageView, 0f, 90f)
        } else {
            userDetailsTextViews.forEach { it.visibility = View.GONE }
            animateArrow(arrowImageView, 90f, 0f)
        }
    }

    private fun animateArrow(arrowImageView: ImageView, startDegree: Float, endDegree: Float) {
        val rotate = RotateAnimation(
            startDegree,
            endDegree,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 300
        rotate.fillAfter = true
        arrowImageView.startAnimation(rotate)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.order_details, menu)
        return true
    }


    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // Hier wird die Standard-Aktion für den Zurück-Button ausgeführt

                true
            }

            R.id.action_filter -> {
                val subMenu = item.subMenu

                subMenu?.clear()

                // Lade die gespeicherten Filteroptionen für Cards

                val statusBasedFilter = if (orderStatusInt == 6) {
                    Settings.KEY_DETAILS_CARD_FILTER_MODE_FINISH
                } else {
                    Settings.KEY_DETAILS_CARD_FILTER_MODE
                }

                val savedCardFilters =
                    sharedPreferences.getStringSet(statusBasedFilter, mutableSetOf())
                        ?: mutableSetOf()

                val cardOptions =
                    mutableListOf("Order", "Cost", "User", "Payment", "Shipping", "Remarks", "Messages")

                if (!sharedPreferences.getBoolean(Settings.KEY_SETTING_DETAILS_MESSAGES, false)) {
                    cardOptions.remove("Messages")
                }

                for (cardId in cardOptions) {
                    val menuItem = subMenu?.add(0, Menu.NONE, 0, cardId)
                    menuItem?.isCheckable = true

                    // Überprüfe, ob die Card bereits ausgewählt ist, und setze das Häkchen entsprechend
                    menuItem?.isChecked = savedCardFilters.contains(cardId)

                    menuItem?.setOnMenuItemClickListener { _ ->
                        // Aktualisiere die Liste der ausgewählten Card-Filter
                        val newCardFilters = HashSet(savedCardFilters)

                        if (newCardFilters.contains(cardId)) {
                            newCardFilters.remove(cardId)
                        } else {
                            newCardFilters.add(cardId)
                        }

                        sharedPreferences.edit()
                            .putStringSet(statusBasedFilter, newCardFilters).apply()
                        applyCardFilters(newCardFilters.toList())
                        true
                    }
                }

                subMenu?.setGroupCheckable(0, true, false)

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun applyCardFilters(filters: List<String>) {
        val cardMap = mapOf(
            "Order" to R.id.cardOrder,
            "Cost" to R.id.costOrder,
            "User" to R.id.cardUser,
            "Payment" to R.id.cardPayment,
            "Shipping" to R.id.cardShipping,
            "Remarks" to R.id.cardRemarks,
            "Messages" to R.id.cardMessages
        )

        cardMap.forEach { (filter, cardId) ->
            val cardView: CardView = findViewById(cardId)
            cardView.visibility = if (filters.contains(filter)) View.VISIBLE else View.GONE
        }
    }


    private fun refreshData() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                getOrderApi(orderId)
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun putPaymentApi(orderId: Int, paymentStatus: String) {
        Loading.showLoading(true, inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}/payment_status"
        val translateStatus = if (paymentStatus == "Paid") { "Received"} else { "None" }
        val apiBody = JSONObject()
            .put("field", "payment_status")
            .put("value", translateStatus)

        Api.apiCall("PUT", apiUrl, apiBody, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                paymentChange(paymentStatus)
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })

    }

    private fun putRemarksApi(orderId: Int, remarksText: String) {
        Loading.showLoading(true, inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}"
        val apiBody = JSONObject()
            .put("remarks", remarksText)

        Api.apiCall("PUT", apiUrl, apiBody, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })

    }

    private fun getOrderApi(orderId: Int) {
       Loading.showLoading(true, this.inflater)
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}"
        Api.apiCall("GET", apiUrl, null, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                val order = JSONObject(data.toString())

                val dateOrdered = order.optString("date_ordered", "")
                val dateStatusChanged = order.optString("date_status_changed", "")
                val sellerName = order.optString("seller_name", "")
                val storeName = order.optString("store_name", "")
                val buyerName = order.optString("buyer_name", "")
                val buyerEmail = order.optString("buyer_email", "")
                val requireInsurance = order.optBoolean("require_insurance", false)
                val status = order.optString("status", "")
                val isInvoiced = order.optBoolean("is_invoiced", false)
                val remarks = order.optString("remarks", "")
                val totalCount = order.optInt("total_count", -1)
                val uniqueCount = order.optInt("unique_count", -1)
                val totalWeight = order.optString("total_weight", "")
                val buyerOrderCount = order.optInt("buyer_order_count", -1)
                val isFiled = order.optBoolean("is_filed", false)
                val driveThruSent = order.optBoolean("drive_thru_sent", false)

                val paymentMethod = order.optJSONObject("payment")?.optString("method", "") ?: ""
                val paymentCurrencyCode =
                    order.optJSONObject("payment")?.optString("currency_code", "") ?: ""
                val paymentDatePaid =
                    order.optJSONObject("payment")?.optString("date_paid", "") ?: ""
                val paymentStatus = order.optJSONObject("payment")?.optString("status", "") ?: ""

                val shippingMethodId =
                    order.optJSONObject("shipping")?.optInt("method_id", -1) ?: -1
                val shippingMethod = order.optJSONObject("shipping")?.optString("method", "") ?: ""
                val shippingAdressName =
                    order.optJSONObject("shipping")?.optJSONObject("address")?.optJSONObject("name")
                        ?.optString("full", "") ?: ""
                val shippingFull =
                    order.optJSONObject("shipping")?.optJSONObject("address")?.optString("full", "")
                        ?: ""
                val shippingCountryCode = order.optJSONObject("shipping")?.optJSONObject("address")
                    ?.optString("country_code", "") ?: ""

                val costCurrencyCode =
                    order.optJSONObject("cost")?.optString("currency_code", "") ?: ""
                val costSubtotal = order.optJSONObject("cost")?.optString("subtotal", "") ?: ""
                val costGrandTotal = order.optJSONObject("cost")?.optString("grand_total", "") ?: ""
                val costEtc1 = order.optJSONObject("cost")?.optString("etc1", "") ?: ""
                val costEtc2 = order.optJSONObject("cost")?.optString("etc2", "") ?: ""
                val costInsurance = order.optJSONObject("cost")?.optString("insurance", "") ?: ""
                val costShipping = order.optJSONObject("cost")?.optString("shipping", "") ?: ""
                val costCredit = order.optJSONObject("cost")?.optString("credit", "") ?: ""
                val costCoupon = order.optJSONObject("cost")?.optString("coupon", "") ?: ""
                val costVatRate = order.optJSONObject("cost")?.optString("vat_rate", "") ?: ""
                val costVatAmount = order.optJSONObject("cost")?.optString("vat_amount", "") ?: ""

                val dispCostCurrencyCode =
                    order.optJSONObject("disp_cost")?.optString("currency_code", "") ?: ""
                val dispCostSubtotal =
                    order.optJSONObject("disp_cost")?.optString("subtotal", "") ?: ""
                val dispCostGrandTotal =
                    order.optJSONObject("disp_cost")?.optString("grand_total", "") ?: ""
                val dispCostEtc1 = order.optJSONObject("disp_cost")?.optString("etc1", "") ?: ""
                val dispCostEtc2 = order.optJSONObject("disp_cost")?.optString("etc2", "") ?: ""
                val dispCostInsurance =
                    order.optJSONObject("disp_cost")?.optString("insurance", "") ?: ""
                val dispCostShipping =
                    order.optJSONObject("disp_cost")?.optString("shipping", "") ?: ""
                val dispCostCredit = order.optJSONObject("disp_cost")?.optString("credit", "") ?: ""
                val dispCostCoupon = order.optJSONObject("disp_cost")?.optString("coupon", "") ?: ""
                val dispCostVatRate =
                    order.optJSONObject("disp_cost")?.optString("vat_rate", "") ?: ""
                val dispCostVatAmount =
                    order.optJSONObject("disp_cost")?.optString("vat_amount", "") ?: ""

                runOnUiThread {
                    orderDetails = OrderDetails(
                        orderId,
                        dateOrdered,
                        dateStatusChanged,
                        sellerName,
                        storeName,
                        buyerName,
                        buyerEmail,
                        requireInsurance,
                        status,
                        isInvoiced,
                        remarks,
                        totalCount,
                        uniqueCount,
                        totalWeight,
                        buyerOrderCount,
                        isFiled,
                        driveThruSent,
                        paymentMethod,
                        paymentCurrencyCode,
                        paymentDatePaid,
                        paymentStatus,
                        shippingMethodId,
                        shippingMethod,
                        shippingAdressName,
                        shippingFull,
                        shippingCountryCode,
                        costCurrencyCode,
                        costSubtotal,
                        costGrandTotal,
                        costEtc1,
                        costEtc2,
                        costInsurance,
                        costShipping,
                        costCredit,
                        costCoupon,
                        costVatRate,
                        costVatAmount,
                        dispCostCurrencyCode,
                        dispCostSubtotal,
                        dispCostGrandTotal,
                        dispCostEtc1,
                        dispCostEtc2,
                        dispCostInsurance,
                        dispCostShipping,
                        dispCostCredit,
                        dispCostCoupon,
                        dispCostVatRate,
                        dispCostVatAmount
                    )
                    updateTextViews()
                    orderStatusInt = OrderStatus.translate(status)!!
                    statusChange(orderStatusInt)
                }
                Loading.showLoading(false, inflater)
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, inflater)
            }

        })
    }

    data class MessageItem(
        val messageSubject: String,
        val messageBody: String,
        val messageFrom: String,
        val messageTo: String,
        val messageDateSent: String
    )

    private fun getMessagesApi(callback: MessagesApiCallback) {
        val apiUrl = "https://api.bricklink.com/api/store/v1/orders/${orderId}/messages"
        Api.apiCall("GET", apiUrl, null, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                try {
                    val items = JSONArray(data.toString())
                    val itemList = mutableListOf<MessageItem>()

                    for (i in 0 until items.length()) {
                        val jsonObject = items.getJSONObject(i)

                        val messageSubject = jsonObject.optString("subject", "")
                        val messageBody = jsonObject.optString("body", "")
                        val messageFrom = jsonObject.optString("from", "")
                        val messageTo = jsonObject.optString("to", "")
                        val messageDateSent = jsonObject.optString("dateSent", "")

                        // Erstelle ein Order-Objekt und füge es zur Liste hinzu
                        val item = MessageItem(
                            messageSubject,
                            messageBody,
                            messageFrom,
                            messageTo,
                            messageDateSent
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

    private fun updateTextViews() {
        runOnUiThread {
            val textOrderId: TextView = findViewById(R.id.textOrderId)
            val textDateOrdered: TextView = findViewById(R.id.textDateOrdered)
            val textStatus: TextView = findViewById(R.id.textStatus)

            val textBuyerName: TextView = findViewById(R.id.textBuyerName)

            val textRemarks: TextView = findViewById(R.id.textRemarks)

            val textTotalCount: TextView = findViewById(R.id.textTotalCount)
            val textUniqueCount: TextView = findViewById(R.id.textUniqueCount)
            val textTotalWeight: TextView = findViewById(R.id.textTotalWeight)

            val textPaymentMethod: TextView = findViewById(R.id.textPaymentMethod)

            val textDispCostSubtotal: TextView = findViewById(R.id.textDispCostSubtotal)
            val textDispCostGrandtotal: TextView = findViewById(R.id.textDispCostGrandtotal)
            val textDispCostGrandtotalInCost: TextView = findViewById(R.id.textDispCostGrandtotalInCost)
            val textDispCostEtc1: TextView = findViewById(R.id.textDispCostEtc1)
            val textDispCostEtc2: TextView = findViewById(R.id.textDispCostEtc2)
            val textDispCostInsurance: TextView = findViewById(R.id.textDispCostInsurance)
            val textDispCostShipping: TextView = findViewById(R.id.textDispCostShipping)
            val textDispCostCredit: TextView = findViewById(R.id.textDispCostCredit)
            val textDispCostCoupon: TextView = findViewById(R.id.textDispCostCoupon)
            val textDispCostVatRate: TextView = findViewById(R.id.textDispCostVatRate)
            val textDispCostVatAmount: TextView = findViewById(R.id.textDispCostVatAmount)
            val textBuyerOrderCount: TextView = findViewById(R.id.textBuyerOrderCount)
            val textShippingMethod: TextView = findViewById(R.id.textShippingMethod)
            val textShippingAdressName: TextView = findViewById(R.id.textShippingAdressName)
            val textPaymentStatus: TextView = findViewById(R.id.textPaymentStatus)

            textOrderId.text = getString(R.string.orderIdWithLabel, orderDetails.orderId)

            val convertedDate =
                Format.convertUtcToLocal(orderDetails.dateOrdered, "MMM d, yyyy HH:mm")
            textDateOrdered.text = convertedDate
            textStatus.text = orderDetails.status
            statusChange(OrderStatus.translate(orderDetails.status)!!)

            val countryName = Country.name(orderDetails.shippingCountryCode)
            val addressFull = getString(R.string.shipping_address_full, orderDetails.shippingAdressName, orderDetails.shippingFull, countryName)
            textShippingAdressName.text = addressFull

            val shippingCopyView: ImageView = findViewById(R.id.shippingCopyView)
            shippingCopyView.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.address), addressFull)
                clipboard.setPrimaryClip(clip)
            }

            textBuyerName.text = orderDetails.buyerName
            textBuyerOrderCount.text = getString(R.string.buyerOrderCountWithLabel, orderDetails.buyerOrderCount)

            textRemarks.text = orderDetails.remarks

            textTotalCount.text = getString(R.string.totalCountWithLabel, orderDetails.totalCount)
            textUniqueCount.text = getString(R.string.uniqueCountWithLabel, orderDetails.uniqueCount)
            textTotalWeight.text = getString(R.string.totalWeightWithUnit, orderDetails.totalWeight)

            textPaymentMethod.text = orderDetails.paymentMethod

            translatePaymentStatus = when (orderDetails.paymentStatus) {
                "Received", "Sent", "Completed" -> {
                    getString(
                        R.string.paid
                    )}
                "None" -> {getString(
                    R.string.not_paid
                )}
                else -> { orderDetails.paymentStatus }
            }
            textPaymentStatus.text = translatePaymentStatus
            paymentChange(translatePaymentStatus)


            textShippingMethod.text = orderDetails.shippingMethod

            textDispCostSubtotal.text = getString(R.string.dispCostSubtotalWithLabel, Format.toDecimal(orderDetails.dispCostSubtotal))
            textDispCostShipping.text = getString(R.string.dispCostShippingWithLabel, Format.toDecimal(orderDetails.dispCostShipping))
            textDispCostGrandtotal.text = getString(R.string.dispCostGrandTotalWithLabel, orderDetails.dispCostCurrencyCode, Format.toDecimal(orderDetails.dispCostGrandTotal))
            textDispCostGrandtotalInCost.text = getString(R.string.dispCostGrandTotalWithLabel, orderDetails.dispCostCurrencyCode, Format.toDecimal(orderDetails.dispCostGrandTotal))
            textDispCostEtc1.text = getString(R.string.dispCostEtc1WithLabel, Format.toDecimal(orderDetails.dispCostEtc1))
            textDispCostEtc2.text = getString(R.string.dispCostEtc2WithLabel, Format.toDecimal(orderDetails.dispCostEtc2))
            textDispCostInsurance.text = getString(R.string.dispCostInsuranceWithLabel, Format.toDecimal(orderDetails.dispCostInsurance))
            textDispCostCredit.text = getString(R.string.dispCostCreditWithLabel, Format.toDecimal(orderDetails.dispCostCredit))
            textDispCostCoupon.text = getString(R.string.dispCostCouponWithLabel, Format.toDecimal(orderDetails.dispCostCoupon))
            textDispCostVatRate.text = getString(R.string.dispCostVatRateWithLabel, Format.toDecimal(orderDetails.dispCostVatRate))
            textDispCostVatAmount.text = getString(R.string.dispCostVatAmountWithLabel, Format.toDecimal(orderDetails.dispCostVatAmount))

            val textMessages: TextView = findViewById(R.id.textMessages)
            textMessages.text = messages

            textBuyerEmail = orderDetails.buyerEmail
        }
    }

    private fun paymentChange(paymentStatus: String) {
        val textPayment: TextView = this.findViewById(R.id.textPaymentStatus)
        if (paymentStatus == getString(R.string.not_paid)) {
            textPayment.setTextColor(ContextCompat.getColor(this, R.color.danger))
        } else {
            textPayment.setTextColor(ContextCompat.getColor(this, R.color.good))
        }
    }

    companion object {
        private const val KEY_SET_STATUS_SHIPPED = "set_staus_shipped"
        private const val KEY_SEND_DRIVE_THRU = "send_drive_thru"
        private const val KEY_POST_FEEDBACK = "post_feedback"
        private const val KEY_FILE = "file"
    }

    interface MessagesApiCallback {
        fun onSuccess(data: List<MessageItem>)
        fun onFailure(error: String)
    }
}
