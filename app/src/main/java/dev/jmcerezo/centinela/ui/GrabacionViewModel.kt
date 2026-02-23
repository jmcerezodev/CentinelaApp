package dev.jmcerezo.centinela.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jmcerezo.centinela.data.local.db.AppDatabase
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de la lógica de negocio para la gestión de grabaciones.
 * Actúa como puente entre la interfaz de usuario y la base de datos persistente.
 */
class GrabacionViewModel(application: Application) : AndroidViewModel(application) {
    
    // Conexión a la base de datos a través del patrón Singleton
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.grabacionDao()

    /**
     * Flujo de datos que expone todas las grabaciones almacenadas.
     * Se actualiza automáticamente cuando la base de datos cambia.
     */
    val todasLasGrabaciones: StateFlow<List<GrabacionDato>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Inserta un nuevo registro de grabación en la base de datos de forma asíncrona.
     */
    fun insertar(grabacion: GrabacionDato) {
        viewModelScope.launch {
            dao.insert(grabacion)
        }
    }

    /**
     * Elimina un registro de grabación específico.
     */
    fun eliminar(grabacion: GrabacionDato) {
        viewModelScope.launch {
            dao.delete(grabacion)
        }
    }

    /**
     * Actualiza un registro existente (ej: renombrar o favorito).
     */
    fun actualizar(grabacion: GrabacionDato) {
        viewModelScope.launch {
            dao.update(grabacion)
        }
    }
}
