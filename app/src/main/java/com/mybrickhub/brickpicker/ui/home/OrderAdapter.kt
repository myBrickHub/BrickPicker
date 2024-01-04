package com.mybrickhub.brickpicker.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mybrickhub.brickpicker.MyApplication
import com.mybrickhub.brickpicker.MyApplication.Companion.sharedPreferences
import com.mybrickhub.brickpicker.R
import com.mybrickhub.brickpicker.utility.Format
import com.mybrickhub.brickpicker.utility.OrderStatus
import com.mybrickhub.brickpicker.utility.Settings


class OrderAdapter(private var allOrders: List<Order>,
                   private val onItemCountChangeListener : OnItemCountChangeListener,
                   private val onOrderItemClickListener: OnOrderItemClickListener,
                   private val recyclerView: RecyclerView,
                   private var sortOption: Int
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {
        private var displayedOrders: List<Order> = allOrders

        data class SortSetting(val sortField: (Order) -> Comparable<*>?, val sortOrder: String)
        private val sortFieldLists = listOf(
                /*0*/   SortSetting(Order::dateOrdered, "Descending"),
                /*1*/   SortSetting(Order::dateOrdered, "Ascending"),
                /*2*/   SortSetting(Order::costTotal, "Descending"),
                /*3*/   SortSetting(Order::costTotal, "Ascending"),
                /*4*/   SortSetting(Order::uniqueCount, "Descending"),
                /*5*/   SortSetting(Order::uniqueCount, "Ascending"),
                /*6*/   SortSetting(Order::totalCount, "Descending"),
                /*7*/   SortSetting(Order::totalCount, "Ascending"),
        )

        class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val orderIdTextView: TextView = itemView.findViewById(R.id.orderIdTextView)
                val dateOrderedTextView: TextView = itemView.findViewById(R.id.orderDateTextView)
                val orderStatusTextView: TextView = itemView.findViewById(R.id.orderStatusTextView)
                val costTotalTextView: TextView = itemView.findViewById(R.id.costTotalTextView)
                val totalCountTextView: TextView = itemView.findViewById(R.id.totalCountTextView)
                val uniqueCountTextView: TextView = itemView.findViewById(R.id.uniqueCountTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_order, parent, false)
                return OrderViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
                val order = displayedOrders[position]
                holder.orderIdTextView.text = MyApplication.getString(R.string.orderIdWithLabel, order.orderId)

                holder.orderStatusTextView.text = order.orderStatus
                holder.orderStatusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                        OrderStatus.color(order.orderStatus)!!
                ))
                holder.dateOrderedTextView.text = order.convertedDate
                holder.costTotalTextView.text = MyApplication.getString(R.string.dispCostGrandTotalWithLabel, order.costCurrency,
                        Format.toDecimal(order.costTotal.toString())!!
                )
                holder.totalCountTextView.text = MyApplication.getString(R.string.totalCountWithLabel, order.totalCount)
                holder.uniqueCountTextView.text = MyApplication.getString(R.string.uniqueCountWithLabel, order.uniqueCount)

                holder.itemView.setOnClickListener {
                        onOrderItemClickListener.onOrderItemClick(order)
                }

        }

        fun onActivityResult(resultCode: Int, data: Intent?, currentFilters: List<String>) {
                if(resultCode == Activity.RESULT_OK) {
                        val orderId = data?.getIntExtra("ORDER_ID", -1)!!
                        val orderStatusInt = data.getIntExtra("ORDER_STATUS_INT", -1)
                        val orderStatusString = OrderStatus.translate(orderStatusInt)
                        val orderIsFiled = data.getBooleanExtra("ORDER_IS_FILED", false)
                        val orderToUpdate = allOrders.find { it.orderId == orderId }
                        orderToUpdate?.orderStatus = orderStatusString.toString()
                        Log.d("MeineKlasse", orderStatusString.toString())
                        orderToUpdate?.isFiled = orderIsFiled
                        filterByStatus(currentFilters)
                        sort(sortOption)
                }
        }

        override fun getItemCount(): Int {
                return displayedOrders.size
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun updateData(sortedAndFilteredOrders: List<Order>) {
                displayedOrders  = sortedAndFilteredOrders
                notifyDataSetChanged()
                onItemCountChangeListener.onItemCountChanged(itemCount)
        }

        fun sort(sortOptionId: Int) {
                val sortedList = when (sortOptionId) {
                        in sortFieldLists.indices -> {
                                val (sortField, sortOrder) = sortFieldLists[sortOptionId]
                                val comparator: Comparator<Order> = when(sortOrder) {
                                        "Ascending" -> compareBy(sortField)
                                        "Descending" -> compareByDescending(sortField)
                                        else -> compareBy (sortField)
                                }
                                displayedOrders.sortedWith(comparator)
                        }
                        else -> displayedOrders
                }
                sortOption = sortOptionId
                recyclerView.scrollToPosition(0)
                updateData(sortedList)
        }

        fun filterByStatus(selectedStatuses: List<String>) {
                val normalizedSelectedStatuses = selectedStatuses.map { it.lowercase() }

                val filteredOrders = if (sharedPreferences.getBoolean(Settings.KEY_SETTING_USE_FILED, false)) {
                        allOrders.filter {
                                it.orderStatus.lowercase() in normalizedSelectedStatuses
                        }
                } else {
                        allOrders.filter {
                                !it.isFiled && it.orderStatus.lowercase() in normalizedSelectedStatuses
                        }
                }
                updateData(filteredOrders)
        }

}

interface OnItemCountChangeListener {
        fun onItemCountChanged(itemCount: Int)
}

interface OnOrderItemClickListener {
        fun onOrderItemClick(order: Order)
}


