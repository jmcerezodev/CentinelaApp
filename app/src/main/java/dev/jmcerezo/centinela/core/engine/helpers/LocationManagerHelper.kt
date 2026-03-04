package dev.jmcerezo.centinela.core.engine.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Gestiona la captura de ubicación GPS y la traducción a direcciones físicas.
 */
class LocationManagerHelper(private val contexto: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(contexto)

    /**
     * Captura la ubicación actual del dispositivo y la traduce a una dirección legible.
     */
    @SuppressLint("MissingPermission")
    fun capturarUbicacionActual(onResultado: (String) -> Unit) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    obtenerDireccion(location.latitude, location.longitude, onResultado)
                } else {
                    onResultado("Ubicación no disponible")
                }
            }
            .addOnFailureListener {
                onResultado("Error al obtener ubicación")
            }
    }

    /**
     * Traduce coordenadas a una dirección física usando Geocoding.
     * Actualizado para evitar APIs deprecadas en Android 13+.
     */
    private fun obtenerDireccion(lat: Double, lon: Double, onResultado: (String) -> Unit) {
        val geocoder = Geocoder(contexto, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    val address = addresses.firstOrNull()
                    val text = address?.let { "${it.getAddressLine(0)}" } ?: "Cerca de ($lat, $lon)"
                    onResultado(text)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val address = addresses?.firstOrNull()
                val text = address?.let { "${it.getAddressLine(0)}" } ?: "Cerca de ($lat, $lon)"
                onResultado(text)
            }
        } catch (e: Exception) {
            onResultado("Coordenadas: $lat, $lon")
        }
    }
}
