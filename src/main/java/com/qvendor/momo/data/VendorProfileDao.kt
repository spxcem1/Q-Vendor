package com.qvendor.momo.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface VendorProfileDao {

    @Query("SELECT * FROM vendor_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): LiveData<List<VendorProfile>>

    @Query("SELECT * FROM vendor_profiles ORDER BY createdAt ASC")
    suspend fun getAllProfilesSync(): List<VendorProfile>

    @Query("SELECT * FROM vendor_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): VendorProfile?

    @Query("SELECT * FROM vendor_profiles WHERE id = :id")
    suspend fun getProfile(id: Long): VendorProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: VendorProfile): Long

    @Update
    suspend fun update(profile: VendorProfile)

    @Query("UPDATE vendor_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE vendor_profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Delete
    suspend fun delete(profile: VendorProfile)
}
