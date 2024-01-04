package com.mybrickhub.brickpicker.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mybrickhub.brickpicker.R
import com.mybrickhub.brickpicker.databinding.FragmentHomeBinding
import com.mybrickhub.brickpicker.utility.Api
import com.mybrickhub.brickpicker.utility.Categories
import com.mybrickhub.brickpicker.utility.Colors
import com.mybrickhub.brickpicker.utility.Format
import com.mybrickhub.brickpicker.utility.Loading
import com.mybrickhub.brickpicker.utility.OrderStatus
import com.mybrickhub.brickpicker.utility.Settings
import kotlinx.coroutines.CoroutineScope
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.HashSet


class HomeFragment : Fragment(), OnItemCountChangeListener {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private val statusOptions = listOf(
        "Pending", "Updated", "Processing", "Ready",
        "Paid", "Packed", "Shipped", "Completed",
        "OCR", "NPB", "NPX", "NRS", "NSS", "Cancelled"
    )
    private var currentFilters = statusOptions

    data class SortOption(val id: Int, val displayName: String)

    private val sortOptions = listOf(
        SortOption(0, "Date (newest first)"),
        SortOption(1, "Date (oldest first)"),
        SortOption(2, "Price (highest first)"),
        SortOption(3, "Price (lowest first)"),
        SortOption(4, "Items (highest first)"),
        SortOption(5, "Items (lowest first)"),
        SortOption(6, "Lots (highest first)"),
        SortOption(7, "Lots (lowest first)"),
    )
    private var sortOption = sortOptions[0].id

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedPreferences = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        if (!sharedPreferences.contains(Settings.KEY_API_CONNECTED)) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.setupConnection))
                .setMessage(getString(R.string.firstSetupMessage))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.menu_setup)) { _, _ ->
                    findNavController().navigate(R.id.action_from_home_to_setup)
                }
                .show()
        }

        sortOption = sharedPreferences.getInt(KEY_HOME_SORT_MODE, -1)

        val swipeRefreshLayout: SwipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        homeViewModel.orders.observe(viewLifecycleOwner) { orders ->
            updateRecyclerView(orders)
        }

        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)

        val savedFilters =
            sharedPreferences.getStringSet(KEY_HOME_FILTER_MODE, null)?.toMutableSet()
                ?: mutableSetOf()
        val newFilters = HashSet(savedFilters)
        if (newFilters.isEmpty()) {
            newFilters.addAll(statusOptions.map { it })
            sharedPreferences.edit().putStringSet(KEY_HOME_FILTER_MODE, newFilters).apply()
        }
        applyFilters(newFilters.toList())
        currentFilters = newFilters.toList()

        Colors.fetchColors()
        Categories.fetchCategories()

        // Api exceeded warning
        val loadedInt = sharedPreferences.getInt(Settings.KEY_API_COUNTS, -1)
        val apiLimit = sharedPreferences.getInt(
            Settings.KEY_CUSTOM_API_LIMIT,
            getString(R.string.standardApiLimit).toInt()
        )
        val warnedToday =
            sharedPreferences.getBoolean(Settings.KEY_API_LIMIT_WARNING_NOTICED, false)
        if (loadedInt >= apiLimit && !warnedToday) {
            requireActivity().runOnUiThread {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.apiLimitExceeded))
                    .setNegativeButton(getString(R.string.ok), null)
                    .show()
            }
            sharedPreferences.edit()
                .putBoolean(Settings.KEY_API_LIMIT_WARNING_NOTICED, true).apply()
        }

        binding.root.post {
            val existingOrders = homeViewModel.getOrders()
            if (existingOrders != null) {
                sort(sortOption)
                updateRecyclerView(existingOrders)
            } else {
                refreshData()
            }
        }

        return root
    }

    override fun onItemCountChanged(itemCount: Int) {
        val recyclerView: RecyclerView = binding.recyclerView
        val orderCountNull: TextView? = view?.findViewById(R.id.orderCountNull)
        if (itemCount == 0) {
            orderCountNull?.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            orderCountNull?.text = getString(
                R.string.homeLastCheck, Format.convertTimestamp(sharedPreferences.getLong(KEY_HOME_LAST_CHECK, -1)
                    , "MMM d, yyyy HH:mm:ss"
                )
            )
        } else {
            orderCountNull?.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun sort(sortOptionId: Int) {
        val orderAdapter = binding.recyclerView.adapter as? OrderAdapter
        orderAdapter?.sort(sortOptionId)
    }

    private fun refreshData() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                getOrdersApi()
            }
            sort(sortOption)
            sharedPreferences.edit().putLong(KEY_HOME_LAST_CHECK, System.currentTimeMillis()).apply()

        }
    }


    private fun onActivityResult(resultCode: Int, data: Intent?, currentFilters: List<String>) {
        val orderAdapter = binding.recyclerView.adapter as? OrderAdapter
        orderAdapter?.onActivityResult(resultCode, data, currentFilters)
        orderAdapter?.sort(sortOption)
    }

    private fun updateRecyclerView(orders: List<Order>) {
        requireActivity().runOnUiThread {
            val recyclerView: RecyclerView = binding.recyclerView

            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Hide the refresh indicator after data is loaded
            binding.swipeRefreshLayout.isRefreshing = false

            val orderAdapter = OrderAdapter(orders, this, object : OnOrderItemClickListener {
                override fun onOrderItemClick(order: Order) {
                    val intent = Intent(requireContext(), OrderDetailsActivity::class.java)
                    intent.putExtra("ORDER_ID", order.orderId)
                    intent.putExtra("ORDER_STATUS_INT", OrderStatus.translate(order.orderStatus))
                    intent.putExtra("ORDER_IS_FILED", order.isFiled)
                    intent.putExtra("ORDER_PAYMENT_STATUS_INT", order.paymentStatusInt)
                    getResult.launch(intent)
                }
            }, recyclerView, sortOption)
            recyclerView.adapter = orderAdapter

            val savedFilters = sharedPreferences.getStringSet(KEY_HOME_FILTER_MODE, null)
            savedFilters?.let { applyFilters(it.toList()) }
            currentFilters = savedFilters!!.toList()

            sort(sortOption)
        }
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            onActivityResult(it.resultCode, it.data, currentFilters)
        }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        @Suppress("DEPRECATION")
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshData()
                true
            }

            R.id.action_sort -> {

                val subMenu = item.subMenu

                subMenu?.clear()

                val savedStatus =
                    sharedPreferences.getInt(KEY_HOME_SORT_MODE, 0)

                for (status in sortOptions) {
                    val menuItem = subMenu?.add(0, Menu.NONE, 0, status.displayName)
                    menuItem?.isCheckable = true

                    menuItem?.isChecked = savedStatus == status.id

                    menuItem?.setOnMenuItemClickListener { _ ->
                        sharedPreferences.edit().putInt(KEY_HOME_SORT_MODE, status.id).apply()
                        sortOption = status.id
                        sort(status.id)

                        true
                    }
                }



                subMenu?.setGroupCheckable(0, true, true)

                true
            }

            R.id.action_filter -> {
                val subMenu = item.subMenu

                subMenu?.clear()

                // Lade die gespeicherten Filteroptionen
                val savedFilters =
                    sharedPreferences.getStringSet(KEY_HOME_FILTER_MODE, mutableSetOf())
                        ?: mutableSetOf()

                for (status in statusOptions) {
                    val menuItem = subMenu?.add(0, Menu.NONE, 0, status)
                    menuItem?.isCheckable = true

                    // Überprüfe, ob der Status bereits ausgewählt ist, und setze das Häkchen entsprechend
                    menuItem?.isChecked = savedFilters.contains(status)

                    menuItem?.setOnMenuItemClickListener { _ ->
                        // Aktualisiere die Liste der ausgewählten Filter
                        val newFilters = HashSet(savedFilters)

                        if (newFilters.contains(status) && newFilters.size > 1) {
                            newFilters.remove(status)
                        } else if (!newFilters.contains(status)) {
                            newFilters.add(status)
                        } else if (newFilters.size <= 1) {
                            AlertDialog.Builder(requireContext())
                                .setTitle(getString(R.string.notice))
                                .setMessage(getString(R.string.selectMinAlert))
                                .setPositiveButton(getString(R.string.ok)) { _, _ -> }
                                .create()
                                .show()
                        }

                        // Speichere die aktualisierten Filteroptionen in den SharedPreferences
                        sharedPreferences.edit().putStringSet(KEY_HOME_FILTER_MODE, newFilters)
                            .apply()

                        // Wende die Filter an
                        if (newFilters.isNotEmpty()) {
                            applyFilters(newFilters.toList())
                            currentFilters = newFilters.toList()
                        }
                        true
                    }
                }

                subMenu?.setGroupCheckable(0, true, false)

                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home, menu)
        @Suppress("DEPRECATION")
        super.onCreateOptionsMenu(menu, inflater)
    }


    private fun applyFilters(selectedFilters: List<String>) {
        val orderAdapter = binding.recyclerView.adapter as? OrderAdapter
        orderAdapter?.filterByStatus(selectedFilters)
        sort(sortOption)
    }

    private fun getOrdersApi() {
        Loading.showLoading(true, layoutInflater)
        var apiUrl =
            "https://api.bricklink.com/api/store/v1/orders?direction=in&filed=false&status=-purged"
        Api.apiCall("GET", apiUrl, null, object : Api.ApiCallback {
            override fun onSuccess(data: Any?) {
                val orders = JSONArray(data.toString())

                if (sharedPreferences.getBoolean(Settings.KEY_SETTING_USE_FILED, false)) {
                    apiUrl =
                        "https://api.bricklink.com/api/store/v1/orders?direction=in&filed=true&status=-purged"
                    Api.apiCall("GET", apiUrl, null, object : Api.ApiCallback {
                        override fun onSuccess(data: Any?) {
                            val filedOrders = JSONArray(data.toString())
                            for (i in 0 until filedOrders.length()) {
                                orders.put(filedOrders.getJSONObject(i))
                            }
                            prepareOrderList(orders)
                            Loading.showLoading(false, layoutInflater)
                        }

                        override fun onFailure(error: String) {
                            Loading.showLoading(false, layoutInflater)
                        }
                    })
                } else {
                    prepareOrderList(orders)
                    Loading.showLoading(false, layoutInflater)
                }
            }

            override fun onFailure(error: String) {
                Loading.showLoading(false, layoutInflater)
            }
        })
    }


    private fun prepareOrderList(orders: JSONArray) {
        val orderList = mutableListOf<Order>()
        for (i in 0 until orders.length()) {
            val jsonObject = orders.getJSONObject(i)

            val orderId = jsonObject.optInt("order_id", 0)
            val dateOrdered = jsonObject.optString("date_ordered", "")
            val orderStatus = jsonObject.optString("status", "")
            val convertedDate = Format.convertUtcToLocal(dateOrdered, "MMM d")

            val costObject = jsonObject.optJSONObject("cost")
            val costTotal = costObject?.optDouble("grand_total", 0.0) ?: 0.0
            val costCurrency = costObject?.optString("currency_code", "") ?: ""

            val totalCount = jsonObject.optInt("total_count", 0)
            val uniqueCount = jsonObject.optInt("unique_count", 0)
            val isFiled = jsonObject.optBoolean("is_filed", false)

            val paymentObject = jsonObject.optJSONObject("payment")
            val paymentMethod = paymentObject?.optString("method", "") ?: ""
            val paymentStatusInt =
                if (paymentMethod == "PayPal (Onsite)" || paymentMethod == "Credit/Debit (Powered by Stripe)") {
                    1
                } else {
                    0
                }

            val order = Order(
                orderId,
                dateOrdered,
                orderStatus,
                convertedDate,
                costTotal,
                totalCount,
                uniqueCount,
                costCurrency,
                isFiled,
                paymentStatusInt
            )
            orderList.add(order)
        }

        // Beispiel: Speichere die Bestellungen im HomeViewModel
        homeViewModel.setOrders(orderList)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_HOME_SORT_MODE = "home_sort_mode"
        private const val KEY_HOME_FILTER_MODE = "home_filter_mode"
        private const val KEY_HOME_LAST_CHECK = "home_last_check"
    }
}