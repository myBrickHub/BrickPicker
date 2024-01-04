package com.mybrickhub.brickpicker.utility

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.mybrickhub.brickpicker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object Loading {

    private var loadingPopup: PopupWindow? = null
    private var popupView: View? = null

    fun showLoading(status: Boolean, inflater: LayoutInflater) {
        CoroutineScope(Dispatchers.Main).launch {
            if (status) {
                val (createdPopup, createdPopupView) = createLoadingPopup(inflater)
                loadingPopup = createdPopup
                popupView = createdPopupView
                loadingPopup?.showAtLocation(popupView, Gravity.CENTER, 0, 0)

            } else {
                loadingPopup?.dismiss()
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun createLoadingPopup(inflater: LayoutInflater): Pair<PopupWindow, View> {
        val popupView = inflater.inflate(R.layout.loading, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        return Pair(popupWindow, popupView)
    }
}