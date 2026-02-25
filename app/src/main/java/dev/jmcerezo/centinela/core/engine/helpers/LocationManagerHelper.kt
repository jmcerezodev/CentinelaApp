package dev.jmcerezo.centinela.core.engine.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Gestor especializado en la captura de coordenadas GPS y dirección física.
 * Soluciona la obtención de ubicación de forma asíncrona y en un hilo de trabajo.
 */
class LocationManagerHelper(private val contexto: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(contexto)

    /**
     * Inicia la captura asíncrona de la ubicación actual.
     * Ejecuta el Geocoder en un hilo separado para no bloquear el principal.
     * @param onLocationReady Callback que se invoca con la dirección formateada o un mensaje de error.
     */
    @SuppressLint("MissingPermission")
    fun capturarUbicacionActual(onLocationReady: (String) -> Unit) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    onLocationReady("Ubicación no disponible")
                    return@addOnSuccessListener
                }

                // Geocoder es bloqueante, ejecutar en hilo de trabajo
                Thread {
                    val resultado = try {
                        val geocoder = Geocoder(contexto, Locale.getDefault())
                        val direcciones = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val coordenadas = "%.6f, %.6f".format(Locale.US, location.latitude, location.longitude)
                        
                        if (!direcciones.isNullOrEmpty()) {
                            "${direcciones[0].getAddressLine(0)} | GPS: $coordenadas"
                        } else {
                            "GPS: $coordenadas"
                        }
                    } catch (e: Exception) {
                        val coordenadas = "%.6f, %.6f".format(Locale.US, location.latitude, location.longitude)
                        "GPS: $coordenadas (dirección no encontrada)"
                    }
                    onLocationReady(resultado)
                }.start()
            }
            .addOnFailureListener {
                onLocationReady("Ubicación no disponible (error de GPS)")
            }
    }
}
