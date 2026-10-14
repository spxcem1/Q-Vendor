package com.qvendor.momo.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getForProfile(profileId: Long): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Query("DELETE FROM transactions WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: Long)
}
