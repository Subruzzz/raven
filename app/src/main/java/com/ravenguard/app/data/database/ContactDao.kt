package com.ravenguard.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY id ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun countContacts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)
}
