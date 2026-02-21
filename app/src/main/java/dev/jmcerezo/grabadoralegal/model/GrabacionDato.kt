package dev.jmcerezo.grabadoralegal.model

import java.io.File

// Esta clase guardará la información de cada audio
data class GrabacionDato(
    val nombre: String,
    val archivo: File,
    val fecha: String,
    val hash: String
)