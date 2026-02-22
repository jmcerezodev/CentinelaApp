package dev.jmcerezo.grabadoralegal.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
