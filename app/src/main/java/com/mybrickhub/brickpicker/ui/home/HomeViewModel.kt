package com.mybrickhub.brickpicker.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> get() = _orders

    fun setOrders(orders: List<Order>) {
        _orders.postValue(orders)
    }

    fun getOrders(): List<Order>? {
        return _orders.value
    }

    fun clearOrders() {
        _orders.value = emptyList()
    }


}

