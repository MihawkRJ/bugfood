package com.bugfood.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DeliveryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: DeliveryEntity): Long

    @Update
    suspend fun update(delivery: DeliveryEntity)

    @Delete
    suspend fun delete(delivery: DeliveryEntity)

    @Query("SELECT * FROM deliveries ORDER BY lastUsedAt DESC")
    fun getAllLive(): LiveData<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries ORDER BY lastUsedAt DESC")
    suspend fun getAll(): List<DeliveryEntity>

    /** Busca por nome exato (case-insensitive) */
    @Query("SELECT * FROM deliveries WHERE LOWER(customerName) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): DeliveryEntity?

    /** Busca parcial por nome */
    @Query("SELECT * FROM deliveries WHERE LOWER(customerName) LIKE '%' || LOWER(:query) || '%' ORDER BY lastUsedAt DESC")
    suspend fun searchByName(query: String): List<DeliveryEntity>

    @Query("SELECT COUNT(*) FROM deliveries")
    suspend fun count(): Int

    /** Incrementa contador de uso e atualiza timestamp */
    @Query("UPDATE deliveries SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long = System.currentTimeMillis())

    /** Atualiza código de entrega */
    @Query("UPDATE deliveries SET deliveryCode = :code WHERE id = :id")
    suspend fun updateDeliveryCode(id: Long, code: String)

    @Query("DELETE FROM deliveries")
    suspend fun deleteAll()
}
