package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.VariantGroupEntity
import com.wfx.warungpos.data.local.entity.VariantOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantDao {
    @Upsert
    suspend fun upsertGroup(entity: VariantGroupEntity)

    @Upsert
    suspend fun upsertOption(entity: VariantOptionEntity)

    @Upsert
    suspend fun upsertGroups(entities: List<VariantGroupEntity>)

    @Upsert
    suspend fun upsertOptions(entities: List<VariantOptionEntity>)

    @Query("SELECT * FROM variant_groups WHERE menuItemId = :menuItemId ORDER BY name ASC")
    fun observeGroupsForItem(menuItemId: String): Flow<List<VariantGroupEntity>>

    @Query("SELECT * FROM variant_options WHERE variantGroupId = :groupId ORDER BY name ASC")
    fun observeOptionsForGroup(groupId: String): Flow<List<VariantOptionEntity>>

    @Query("SELECT * FROM variant_groups WHERE syncStatus = 'PENDING'")
    suspend fun getPendingGroups(): List<VariantGroupEntity>

    @Query("SELECT * FROM variant_options WHERE syncStatus = 'PENDING'")
    suspend fun getPendingOptions(): List<VariantOptionEntity>

    // Cascade deletes options via FK
    @Query("DELETE FROM variant_groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Query("DELETE FROM variant_options WHERE id = :id")
    suspend fun deleteOption(id: String)
}
