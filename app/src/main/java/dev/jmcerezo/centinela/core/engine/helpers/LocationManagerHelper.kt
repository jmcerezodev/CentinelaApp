package dev.jmcerezo.centinela.core.engine.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Gestor especializado en la captura de coordenadas GPS y dirección física.
 */
class LocationManagerHelper(private val contexto: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(contexto)
    private var ubicacionCapturada: String = "Ubicación no disponible"

    /**
     * Inicia la captura asíncrona de la ubicación actual.
     */
    @SuppressLint("MissingPermission")
    fun capturarUbicacionActual() {
        ubicacionCapturada = "Ubicación no disponible"
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(contexto, Locale.getDefault())
                        val direcciones = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val coordenadas = "${location.latitude}, ${location.longitude}"
                        
                        ubicacionCapturada = if (!direcciones.isNullOrEmpty()) {
                            "${direcciones[0].getAddressLine(0)} | GPS: $coordenadas"
                        } else {
                            "GPS: $coordenadas"
                        }
                    } catch (e: Exception) {
                        ubicacionCapturada = "GPS: ${location.latitude}, ${location.longitude}"
                    }
                }
            }
    }

    /**
     * Devuelve el último dato de ubicación obtenido.
     */
    fun obtenerUbicacionCapturada(): String = ubicacionCapturada
}
