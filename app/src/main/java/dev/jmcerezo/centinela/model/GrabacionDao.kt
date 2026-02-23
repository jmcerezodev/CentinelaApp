package dev.jmcerezo.centinela.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GrabacionDao {
    @Query("SELECT * FROM grabaciones ORDER BY fecha DESC")
    fun getAll(): Flow<List<GrabacionDato>>

    @Insert
    suspend fun insert(grabacion: GrabacionDato)

    @Delete
    suspend fun delete(grabacion: GrabacionDato)

    @Update
    suspend fun update(grabacion: GrabacionDato)
}
