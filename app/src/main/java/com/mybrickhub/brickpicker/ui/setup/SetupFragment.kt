package com.mybrickhub.brickpicker.ui.setup

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mybrickhub.brickpicker.R
import com.mybrickhub.brickpicker.databinding.FragmentSetupBinding
import com.mybrickhub.brickpicker.utility.Api
import com.mybrickhub.brickpicker.utility.Loading
import com.mybrickhub.brickpicker.utility.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var apiConsumerKeyEditText: EditText
    private lateinit var apiConsumerSecretEditText: EditText
    private lateinit var apiTokenEditText: EditText
    private lateinit var apiSecretEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root

        apiConsumerKeyEditText = root.findViewById(R.id.apiConsumerKeyEditText)
        apiConsumerSecretEditText = root.findViewById(R.id.apiConsumerSecretEditText)
        apiTokenEditText = root.findViewById(R.id.apiTokenEditText)
        apiSecretEditText = root.findViewById(R.id.apiSecretEditText)
        saveButton = root.findViewById(R.id.saveButton)
        resetButton = root.findViewById(R.id.resetButton)

        // Initialisiere SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // Überprüfe den gespeicherten Zustand der Eingabefelder
        val inputFieldsDisabled = sharedPreferences.getBoolean(Settings.KEY_API_CONNECTED, false)
        if (inputFieldsDisabled) {
            disableInputFields()
            apiConsumerKeyEditText.setText(
                sharedPreferences.getString(
                    Settings.KEY_API_CONSUMER_KEY,
                    ""
                )
            )
            apiConsumerSecretEditText.setText(
                sharedPreferences.getString(
                    Settings.KEY_API_CONSUMER_SECRET,
                    ""
                )
            )
            apiTokenEditText.setText(sharedPreferences.getString(Settings.KEY_API_TOKEN, ""))
            apiSecretEditText.setText(sharedPreferences.getString(Settings.KEY_API_SECRET, ""))
        } else {
            enableInputFields()
        }

        saveButton.setOnClickListener {
            Loading.showLoading(true, layoutInflater)
            val apiConsumerKey = apiConsumerKeyEditText.text.toString().replace("\\s".toRegex(), "")
            val apiConsumerSecret = apiConsumerSecretEditText.text.toString().replace("\\s".toRegex(), "")
            val apiToken = apiTokenEditText.text.toString().replace("\\s".toRegex(), "")
            val apiSecret = apiSecretEditText.text.toString().replace("\\s".toRegex(), "")
            with(sharedPreferences.edit()) {
                putString(Settings.KEY_API_CONSUMER_KEY, apiConsumerKey)
                putString(Settings.KEY_API_CONSUMER_SECRET, apiConsumerSecret)
                putString(Settings.KEY_API_TOKEN, apiToken)
                putString(Settings.KEY_API_SECRET, apiSecret)
                apply()
            }

            val apiUrl = "https://api.bricklink.com/api/store/v1/colors"
            Api.apiCall("GET", apiUrl, null, object : Api.ApiCallback {
                override fun onSuccess(data: Any?) {
                    Loading.showLoading(false, layoutInflater)
                    requireActivity().runOnUiThread {
                        saveInputFieldsState(true)
                        disableInputFields()
                        makeText(
                            requireContext(),
                            getString(R.string.api_successfully_connected), Toast.LENGTH_SHORT
                        ).show()
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        findNavController().navigate(R.id.action_from_setup_to_home)
                    }
                }

                override fun onFailure(error: String) {
                    Loading.showLoading(false, layoutInflater)
                    requireActivity().runOnUiThread {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle(getString(R.string.err))
                            .setMessage(getString(R.string.apiTestFailed))
                            .setNegativeButton(getString(R.string.ok), null)
                            .show()
                    }
                }
            })
        }

        resetButton.setOnClickListener {
            showResetConfirmationDialog()
        }

        return root
    }

    private fun disableInputFields() {
        apiConsumerKeyEditText.isEnabled = false
        apiConsumerSecretEditText.isEnabled = false
        apiTokenEditText.isEnabled = false
        apiSecretEditText.isEnabled = false
        apiConsumerKeyEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        apiConsumerSecretEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        apiTokenEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        apiSecretEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        saveButton.visibility = View.GONE
        resetButton.visibility = View.VISIBLE
    }

    private fun enableInputFields() {
        apiConsumerKeyEditText.isEnabled = true
        apiConsumerSecretEditText.isEnabled = true
        apiTokenEditText.isEnabled = true
        apiSecretEditText.isEnabled = true
        apiConsumerKeyEditText.inputType = InputType.TYPE_CLASS_TEXT
        apiConsumerSecretEditText.inputType = InputType.TYPE_CLASS_TEXT
        apiTokenEditText.inputType = InputType.TYPE_CLASS_TEXT
        apiSecretEditText.inputType = InputType.TYPE_CLASS_TEXT
        apiConsumerKeyEditText.text.clear()
        apiConsumerSecretEditText.text.clear()
        apiTokenEditText.text.clear()
        apiSecretEditText.text.clear()
        saveButton.visibility = View.VISIBLE
        resetButton.visibility = View.GONE
    }

    private fun saveInputFieldsState(status: Boolean) {
        sharedPreferences.edit().putBoolean(Settings.KEY_API_CONNECTED, status).apply()
    }

    private fun showResetConfirmationDialog() {
        requireActivity().runOnUiThread {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.resetConfirmTitle))
                .setMessage(getString(R.string.resetConfirm))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    enableInputFields()
                    saveInputFieldsState(false)
                    apiConsumerKeyEditText.requestFocus()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
