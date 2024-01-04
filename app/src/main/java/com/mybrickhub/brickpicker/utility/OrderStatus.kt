package com.mybrickhub.brickpicker.utility

import com.mybrickhub.brickpicker.R

enum class OrderStatus(val statusNumber: Int, val statusName: String, val statusColor: Int) {
    PENDING(1, "PENDING", R.color.pendingStatus),
    UPDATED(2, "UPDATED", R.color.updatedStatus),
    PROCESSING(3, "PROCESSING", R.color.processingStatus),
    READY(4, "READY", R.color.readyStatus),
    PAID(5, "PAID", R.color.paidStatus),
    PACKED(6, "PACKED", R.color.packedStatus),
    SHIPPED(7, "SHIPPED", R.color.shippedStatus),
    RECEIVED(8, "RECEIVED", R.color.receivedStatus),
    COMPLETED(9, "COMPLETED", R.color.completedStatus),
    OCR(10, "OCR", R.color.ocrStatus),
    NPB(11, "NPB", R.color.npbStatus),
    NPX(12, "NPX", R.color.npxStatus),
    NRS(13, "NRS", R.color.nrsStatus),
    NSS(14, "NSS", R.color.nssStatus),
    CANCELLED(15, "CANCELLED", R.color.cancelledStatus),
    PURGED(16, "PURGED", R.color.purgedStatus);

    companion object {
        fun translate(input: String): Int? {
            return entries.find {
                it.statusName == input || it.statusName.equals(input, true)
            }?.statusNumber
        }
        fun translate(input: Int): String? {
            return entries.find { it.statusNumber == input }?.statusName
        }

        fun color(input: String): Int? {
            return entries.find {
                it.statusName == input || it.statusName.equals(input, true)
            }?.statusColor
        }
        fun color(input: Int): Int? {
            return entries.find { it.statusNumber == input }?.statusColor
        }
    }
}

