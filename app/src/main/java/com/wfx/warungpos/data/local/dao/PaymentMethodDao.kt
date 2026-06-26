package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Upsert
    suspend fun upsert(entity: PaymentMethodEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(entities: List<PaymentMethodEntity>)

    @Query("SELECT * FROM payment_methods WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun observeActive(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE id = :id")
    suspend fun getById(id: String): PaymentMethodEntity?

    @Query("SELECT * FROM payment_methods WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<PaymentMethodEntity>
}
