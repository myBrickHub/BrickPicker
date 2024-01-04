package com.mybrickhub.brickpicker

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set

        val sharedPreferences: SharedPreferences
            get() = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)

        fun getString(resourceId: Int, vararg formatArgs: Any): String {
            return context.getString(resourceId, *formatArgs)
        }
    }
}