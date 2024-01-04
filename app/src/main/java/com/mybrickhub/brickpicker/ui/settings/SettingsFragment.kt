package com.mybrickhub.brickpicker.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.mybrickhub.brickpicker.R
import com.mybrickhub.brickpicker.databinding.FragmentSettingsBinding
import com.mybrickhub.brickpicker.utility.Settings
import com.google.android.material.textfield.TextInputLayout
import com.mybrickhub.brickpicker.utility.Theme
import java.util.Locale


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private lateinit var sharedPreferences: SharedPreferences

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedPreferences = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // Theme
        val editSettingTheme =
            root.findViewById<ImageView>(R.id.editSettingTheme)
        editSettingTheme.setOnClickListener {
            createPopupChooseView(
                    arrayOf("System default", "Light", "Dark"),
                sharedPreferences.getString(Settings.KEY_THEME, "System default").toString(),
                Theme()::setAppTheme
            )
        }
        // Checkboxes
        data class Setting(val id: Int, val key: String)
        val checkBoxIds = listOf(
            Setting(R.id.switchSettingFileCC, Settings.KEY_SETTING_FILE_CC),
            Setting(R.id.switchSettingDetailsMessages, Settings.KEY_SETTING_DETAILS_MESSAGES),
            Setting(R.id.switchSettingUseFiled, Settings.KEY_SETTING_USE_FILED),
        )
        checkBoxIds.forEach { setting ->
            val checkBox = root.findViewById<SwitchCompat>(setting.id)
            checkBox?.isChecked = sharedPreferences.getBoolean(setting.key, false)
            checkBox?.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean(setting.key, isChecked).apply()
                if (setting.key == Settings.KEY_SETTING_DETAILS_MESSAGES) {
                    val savedCardFilters = sharedPreferences.getStringSet(
                        Settings.KEY_DETAILS_CARD_FILTER_MODE,
                        mutableSetOf()
                    ) ?: mutableSetOf("Order", "Cost", "User", "Payment", "Shipping", "Remarks")
                    val newCardFilters = HashSet(savedCardFilters)
                    if (isChecked) {
                        newCardFilters.add("Messages")
                    } else {
                        newCardFilters.remove("Messages")
                    }
                    sharedPreferences.edit()
                        .putStringSet(Settings.KEY_DETAILS_CARD_FILTER_MODE, newCardFilters).apply()
                }
            }
        }
        //Feedback
        val editSettingDetailsFeedback =
            root.findViewById<ImageView>(R.id.editSettingDetailsFeedback)
        editSettingDetailsFeedback.setOnClickListener {
            createPopupEditView(
                "Feedback",
                Settings.KEY_CUSTOM_FEEDBACK,
                getString(R.string.standardFeedback)
            )
        }
        //Api Limit
        val editSettingDetailsApiLimit =
            root.findViewById<ImageView>(R.id.editSettingDetailsApiLimit)
        editSettingDetailsApiLimit.setOnClickListener {
            createPopupEditViewInt(
                "API Limit",
                Settings.KEY_CUSTOM_API_LIMIT,
                getString(R.string.standardApiLimit).toInt()
            )
        }
        val apiLimitTextView =
            root.findViewById<TextView>(R.id.apiLimitTextView)
        val apiCount = sharedPreferences.getInt(Settings.KEY_API_COUNTS, -1)
        apiLimitTextView.text = getString(R.string.settingTextLimitApi, apiCount)

        return root
    }

    @SuppressLint("InflateParams")
    private fun createPopupChooseView(
        chooseOptions: Array<String>,
        loadedChoose: String,
        performFunction: (String) -> Unit
    ) {
        val popupView = layoutInflater.inflate(R.layout.popup_choose, null)

        val statusRadioGroup = popupView.findViewById<RadioGroup>(R.id.statusRadioGroup)
        for ((index, status) in chooseOptions.withIndex()) {
            val radioButton = RadioButton(requireContext())
            radioButton.text = status.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()) else it.toString() }
            radioButton.id = index
            statusRadioGroup.addView(radioButton)

            if (status.equals(loadedChoose, true)) {
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

                if (!selectedStatus.equals(loadedChoose, true)) {
                    performFunction(selectedStatus)
                }

                popupWindow.dismiss()
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
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
        popupEditTitle: String,
        sharedPreferenceName: String,
        sharedPreferenceFallback: String,
    ) {
        val loadedText = sharedPreferences.getString(sharedPreferenceName, sharedPreferenceFallback)

        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_edit, null)

        val popupEditTextInputLayout =
            popupView.findViewById<TextInputLayout>(R.id.popupEditTextInputLayout)
        popupEditTextInputLayout.hint = popupEditTitle

        val popupDetailsEditText = popupView.findViewById<EditText>(R.id.popupEditTextContent)
        popupDetailsEditText.setText(loadedText)
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
            if (loadedText != newTextContent) {
                sharedPreferences.edit().putString(sharedPreferenceName, newTextContent).apply()
            }
            popupWindow.dismiss()
        }
        val popupButtonCancel = popupView.findViewById<Button>(R.id.popupButtonCancel)
        popupButtonCancel.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    @SuppressLint("InflateParams")
    private fun createPopupEditViewInt(
        popupEditTitle: String,
        sharedPreferenceName: String,
        sharedPreferenceFallback: Int
    ) {
        val loadedInt = sharedPreferences.getInt(sharedPreferenceName, sharedPreferenceFallback)

        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_edit, null)

        val popupEditTextInputLayout =
            popupView.findViewById<TextInputLayout>(R.id.popupEditTextInputLayout)
        popupEditTextInputLayout.hint = popupEditTitle

        val popupDetailsEditText = popupView.findViewById<EditText>(R.id.popupEditTextContent)
        popupDetailsEditText.setText(loadedInt.toString())
        popupDetailsEditText.inputType = InputType.TYPE_CLASS_NUMBER
        popupDetailsEditText.requestFocus()

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        popupWindow.isFocusable = true
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        val popupButtonSave = popupView.findViewById<Button>(R.id.popupButtonSave)
        popupButtonSave.setOnClickListener {
            try {
                val newTextContent = popupDetailsEditText.text.toString().toInt()

                if (newTextContent <= 0) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(),
                            getString(R.string.please_enter_a_positive_value), Toast.LENGTH_SHORT).show()
                    }
                    return@setOnClickListener
                }

                if (newTextContent != loadedInt) {
                    sharedPreferences.edit().putInt(sharedPreferenceName, newTextContent).apply()
                }

                popupWindow.dismiss()
            } catch (e: NumberFormatException) {
                // Handle the case when the text cannot be converted to an integer
                Log.e("ConversionError", "Error converting text to integer: ${e.message}")
                // Show a message or handle the conversion error as needed
            }
        }

        val popupButtonCancel = popupView.findViewById<Button>(R.id.popupButtonCancel)
        popupButtonCancel.setOnClickListener {
            popupWindow.dismiss()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}