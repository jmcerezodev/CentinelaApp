package dev.jmcerezo.centinela.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz de acceso a datos (DAO) para la tabla de grabaciones.
 */
@Dao
interface GrabacionDao {
    /**
     * Recupera todas las grabaciones ordenadas por fecha (las más recientes primero).
     */
    @Query("SELECT * FROM grabaciones ORDER BY fecha DESC")
    fun getAll(): Flow<List<GrabacionDato>>

    /**
     * Inserta una nueva grabación en la base de datos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grabacion: GrabacionDato)

    /**
     * Elimina una grabación específica.
     */
    @Delete
    suspend fun delete(grabacion: GrabacionDato)

    /**
     * Actualiza los datos de una grabación (ej: renombrado o cambio de favorito).
     */
    @Update
    suspend fun update(grabacion: GrabacionDato)
}
