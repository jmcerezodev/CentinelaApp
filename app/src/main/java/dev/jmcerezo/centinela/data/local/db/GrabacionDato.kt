package dev.jmcerezo.centinela.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una grabación de audio en la base de datos.
 * 
 * @property id Identificador único autogenerado por Room.
 * @property nombre Nombre descriptivo de la evidencia.
 * @property rutaArchivo Ruta absoluta del archivo .m4a en el sistema de archivos.
 * @property fecha Fecha de creación formateada (dd/MM/yyyy HH:mm).
 * @property hash Firma digital SHA-256 para garantizar la integridad legal del audio.
 * @property ubicacion Texto descriptivo de la localización (Dirección + GPS).
 * @property esFavorito Indica si la grabación ha sido marcada como destacada por el usuario.
 */
@Entity(tableName = "grabaciones")
data class GrabacionDato(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val rutaArchivo: String,
    val fecha: String,
    val hash: String,
    val ubicacion: String = "Ubicación no disponible",
    val esFavorito: Boolean = false
)
