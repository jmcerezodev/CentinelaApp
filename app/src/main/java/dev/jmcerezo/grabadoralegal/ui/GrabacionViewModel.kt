package dev.jmcerezo.grabadoralegal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jmcerezo.grabadoralegal.model.AppDatabase
import dev.jmcerezo.grabadoralegal.model.GrabacionDato
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GrabacionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.grabacionDao()

    val todasLasGrabaciones: StateFlow<List<GrabacionDato>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertar(grabacion: GrabacionDato) {
        viewModelScope.launch {
            dao.insert(grabacion)
        }
    }

    fun eliminar(grabacion: GrabacionDato) {
        viewModelScope.launch {
            dao.delete(grabacion)
        }
    }

    fun actualizar(grabacion: GrabacionDato) {
        viewModelScope.launch {
            dao.update(grabacion)
        }
    }
}
