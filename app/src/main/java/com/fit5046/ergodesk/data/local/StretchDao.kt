package com.fit5046.ergodesk.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fit5046.ergodesk.data.model.StretchItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for stretch routines in Room database.
 */
@Dao
interface StretchDao {

    @Query("SELECT * FROM stretches ORDER BY id DESC")
    fun getAllStretches(): Flow<List<StretchItemEntity>>

    @Query("SELECT * FROM stretches WHERE targetArea = :targetArea ORDER BY id DESC")
    fun getStretchesByArea(targetArea: String): Flow<List<StretchItemEntity>>

    @Query("SELECT * FROM stretches WHERE id = :id LIMIT 1")
    fun getStretchById(id: Long): Flow<StretchItemEntity?>

    @Query("SELECT COUNT(*) FROM stretches")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStretch(stretch: StretchItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stretches: List<StretchItemEntity>)

    @Update
    suspend fun updateStretch(stretch: StretchItemEntity)

    @Delete
    suspend fun deleteStretch(stretch: StretchItemEntity)

    @Query("DELETE FROM stretches WHERE id = :id")
    suspend fun deleteById(id: Long)
}
