package dev.jmcerezo.grabadoralegal.model

import java.io.File

// Esta clase guardará la información de cada audio, incluyendo su contexto geográfico
data class GrabacionDato(
    val nombre: String,
    val archivo: File,
    val fecha: String,
    val hash: String,
    val ubicacion: String = "Ubicación no disponible" // Campo nuevo para el GPS
)