package com.mybrickhub.brickpicker.ui.home

import com.mybrickhub.brickpicker.utility.Categories
import com.mybrickhub.brickpicker.utility.Colors

data class Order(
    val orderId: Int,
    val dateOrdered: String,
    var orderStatus: String,
    val convertedDate: String,
    val costTotal: Double,
    val totalCount: Int,
    val uniqueCount: Int,
    val costCurrency: String,
    var isFiled: Boolean,
    val paymentStatusInt: Int
)

data class OrderDetails(
    val orderId: Int,
    val dateOrdered: String,
    val dateStatusChanged: String,
    val sellerName: String,
    val storeName: String,
    val buyerName: String,
    val buyerEmail: String,
    val requireInsurance: Boolean,
    val status: String,
    val isInvoiced: Boolean,
    val remarks: String,
    val totalCount: Int,
    val uniqueCount: Int,
    val totalWeight: String,
    val buyerOrderCount: Int,
    val isFiled: Boolean,
    val driveThruSent: Boolean,
    val paymentMethod: String,
    val paymentCurrencyCode: String,
    val paymentDatePaid: String,
    val paymentStatus: String,
    val shippingMethodId: Int,
    val shippingMethod: String,
    val shippingAdressName: String,
    val shippingFull: String,
    val shippingCountryCode: String,
    val costCurrencyCode: String,
    val costSubtotal: String,
    val costGrandTotal: String,
    val costEtc1: String,
    val costEtc2: String,
    val costInsurance: String,
    val costShipping: String,
    val costCredit: String,
    val costCoupon: String,
    val costVatRate: String,
    val costVatAmount: String,
    val dispCostCurrencyCode: String,
    val dispCostSubtotal: String,
    val dispCostGrandTotal: String,
    val dispCostEtc1: String,
    val dispCostEtc2: String,
    val dispCostInsurance: String,
    val dispCostShipping: String,
    val dispCostCredit: String,
    val dispCostCoupon: String,
    val dispCostVatRate: String,
    val dispCostVatAmount: String
)

data class OrderItem(
    val inventoryId: Int,
    val itemNo: String,
    val itemName: String,
    val itemType: String,
    val itemCategoryId: Int,
    val colorId: Int,
    val quantity: Int,
    val newOrUsed: String,
    val completeness: String,
    val unitPrice: String,
    val unitPriceFinal: String,
    val dispUnitPrice: String,
    val dispUnitPriceFinal: String,
    val currencyCode: String,
    val dispCurrencyCode: String,
    val description: String,
    val remarks: String,
) {
    var colorName: String = Colors.getColorName(colorId)
    var itemCategoryName: String = Categories.getCategoryName(itemCategoryId)
}