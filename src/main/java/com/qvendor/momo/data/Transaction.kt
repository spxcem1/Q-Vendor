package com.qvendor.momo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionStatus {
    PENDING,
    SUCCESSFUL,
    FAILED
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val actionName: String,
    val ispName: String,
    val phone: String,
    val amount: Double,
    val status: TransactionStatus,
    val createdAt: Long = System.currentTimeMillis()
)
