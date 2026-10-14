package com.qvendor.momo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vendor_profiles")
data class VendorProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ispName: String,
    val iconIndex: Int = 0,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val actionsJson: String = "",
    val defaultAmount: String = "",
    val defaultPhone: String = "",
    val defaultReference: String = ""
) {
    val isp: ISP get() = ISP.fromName(ispName)
    val profileActions: ProfileActions get() = ProfileActions.fromJson(actionsJson)
}
