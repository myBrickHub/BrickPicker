package com.mybrickhub.brickpicker.ui.home

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mybrickhub.brickpicker.R
import com.bumptech.glide.Glide
import com.mybrickhub.brickpicker.MyApplication
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale


class ItemAdapter(
    private val activity: OrderItemsActivity,
    private var allItems: List<OrderItem>,
    private val recyclerView: RecyclerView,
    private var displayLevels: List<String>,
    private val buttonFinishPickText : String
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private fun translateSortField(orderItem: OrderItem, sortLevel: String): String {
        return when (sortLevel) {
            "Category Name" -> orderItem.itemCategoryName
            "Color Name" -> orderItem.colorName
            "Comments" -> orderItem.description
            "Condition" -> orderItem.completeness
            "Item Name" -> orderItem.itemName
            "Item No" -> orderItem.itemNo
            "Remarks" -> orderItem.remarks
            else -> ""
        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        val thumbImageView: ImageView = itemView.findViewById(R.id.thumbImageView)
        val itemArrayPositionTextView: TextView =
            itemView.findViewById(R.id.itemArrayPositionTextView)
        val itemSort0TextView: TextView = itemView.findViewById(R.id.itemSort0TextView)
        val itemSort1TextView: TextView = itemView.findViewById(R.id.itemSort1TextView)
        val itemSort2TextView: TextView = itemView.findViewById(R.id.itemSort2TextView)
        val itemDetailsListLayoutView: View = itemView.findViewById(R.id.itemDetailsListLayoutView)

        val itemDetailsInventoryId: TextView = itemView.findViewById(R.id.itemDetailsInventoryId)
        val itemDetailsRemarks: TextView = itemView.findViewById(R.id.itemDetailsRemarks)
        val itemDetailsDescription: TextView = itemView.findViewById(R.id.itemDetailsDescription)
        val itemDetailsItemNo: TextView = itemView.findViewById(R.id.itemDetailsItemNo)
        val itemDetailsItemName: TextView = itemView.findViewById(R.id.itemDetailsItemName)
        val itemDetailsItemType: TextView = itemView.findViewById(R.id.itemDetailsItemType)
        val itemDetailsColorName: TextView = itemView.findViewById(R.id.itemDetailsColorName)
        val itemDetailsItemCategoryName: TextView = itemView.findViewById(R.id.itemDetailsItemCategoryName)
        val itemDetailsCondition: TextView = itemView.findViewById(R.id.itemDetailsCondition)
        val itemDetailsQuantity: TextView = itemView.findViewById(R.id.itemDetailsQuantity)
        val itemDetailsUnitPrice: TextView = itemView.findViewById(R.id.itemDetailsUnitPrice)
        val itemDetailsPriceTotal: TextView = itemView.findViewById(R.id.itemDetailsPriceTotal)
        val buttonFinishPick: Button = itemView.findViewById(R.id.buttonFinishPick)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_item, parent, false)
                 return ItemViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return allItems.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = allItems[position]

        val thumbType = when (item.itemType[0].toString()) {
            "P" -> "PN"
            "M" -> "MN"
            "S" -> "SN"
            "B" -> "BN"
            "G" -> "GN"
            "C" -> "CN"
            "I" -> "IN"
            "O" -> "ON"
            else -> item.itemType
        }
        //if type == set and setting.active show ON instead of SN
        val thumbUrl = "https://img.bricklink.com/ItemImage/${thumbType}/${item.colorId}/${item.itemNo}.png"
        Glide.with(holder.itemView.context)
            .load(thumbUrl)
            .into(holder.thumbImageView)

        holder.itemArrayPositionTextView.text = MyApplication.getString(R.string.item_position, allItems.indexOf(item) + 1, allItems.size)
        holder.quantityTextView.text = MyApplication.getString(R.string.quantity_format, item.quantity)

        var condition = when (item.newOrUsed) {
            "N" -> "New"
            "U" -> "Used"
            else -> item.newOrUsed
        }
        if (item.itemType == "S") {
            val completenessName = when (item.completeness) {
                "C" -> "Complete"
                "B" -> "Incomplete"
                "S" -> "Sealed"
                else -> item.completeness
            }
            condition += " - $completenessName"
        }

        //Front
        val maxFields = minOf(3, displayLevels.size)
        holder.itemSort0TextView.text = ""
        holder.itemSort1TextView.text = ""
        holder.itemSort2TextView.text = ""
        holder.itemSort0TextView.visibility = View.GONE
        holder.itemSort1TextView.visibility = View.GONE
        holder.itemSort2TextView.visibility = View.GONE
        for (i in 0 until maxFields) {
            val sortField = translateSortField(item ,displayLevels[i])
            val textView = when (i) {
                0 -> holder.itemSort0TextView
                1 -> holder.itemSort1TextView
                2 -> holder.itemSort2TextView
                else -> null
            }

            textView?.text = when (sortField) {
                "newOrUsed" -> condition
                else -> sortField
            }

            textView?.post {
                textView.maxLines = 1
                textView.ellipsize = TextUtils.TruncateAt.END
            }

            textView?.visibility = View.VISIBLE
        }


        //Details
        holder.thumbImageView.setOnClickListener {
            holder.thumbImageView.visibility = View.GONE
            holder.itemDetailsListLayoutView.visibility = View.VISIBLE
        }

        holder.itemDetailsListLayoutView.setOnClickListener {
            holder.itemDetailsListLayoutView.visibility = View.GONE
            holder.thumbImageView.visibility = View.VISIBLE
        }

        holder.itemDetailsInventoryId.text = item.inventoryId.toString()
        holder.itemDetailsRemarks.text = item.remarks
        holder.itemDetailsDescription.text = item.description
        holder.itemDetailsItemNo.text = item.itemNo
        holder.itemDetailsItemName.text = item.itemName
        holder.itemDetailsItemType.text = item.itemType.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        holder.itemDetailsColorName.text = item.colorName
        holder.itemDetailsItemCategoryName.text = item.itemCategoryName
        holder.itemDetailsCondition.text = condition
        holder.itemDetailsQuantity.text = item.quantity.toString()
        holder.itemDetailsUnitPrice.text = MyApplication.getString(R.string.unit_price_format, item.currencyCode, BigDecimal(item.unitPrice).setScale(2, RoundingMode.HALF_EVEN))
        holder.itemDetailsPriceTotal.text = MyApplication.getString(R.string.unit_price_total_format, item.currencyCode, BigDecimal(item.unitPrice.toDouble() * item.quantity).setScale(2, RoundingMode.HALF_EVEN))


        // Finish
        if (position == itemCount - 1) {
            holder.buttonFinishPick.text = buttonFinishPickText
            holder.buttonFinishPick.visibility = View.VISIBLE
            holder.buttonFinishPick.setOnClickListener {
                activity.finishActivity()
            }
        }

    }

    fun sort(sortLevels: List<String>, displayLevelsList: List<String>) {

        val sortedList = allItems.sortedBy { orderItem ->
            sortLevels.map { translateSortField(orderItem, it) }.toList().joinToString(",")
        }

        recyclerView.scrollToPosition(0)
        allItems = sortedList
        displayLevels = displayLevelsList
        notifyItemRangeChanged(0, itemCount)
    }

}


