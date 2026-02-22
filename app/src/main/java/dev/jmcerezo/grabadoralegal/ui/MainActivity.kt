package dev.jmcerezo.grabadoralegal.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jmcerezo.grabadoralegal.core.GrabadoraMotor
import dev.jmcerezo.grabadoralegal.model.GrabacionDato
import dev.jmcerezo.grabadoralegal.ui.componentes.*
import dev.jmcerezo.grabadoralegal.ui.componentes.dialogos.*

class MainActivity : ComponentActivity() {

    private val solicitudPermisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val microConcedido = resultados[Manifest.permission.RECORD_AUDIO] ?: false
        val gpsConcedido = resultados[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!microConcedido || !gpsConcedido) {
            Toast.makeText(this, "Se requieren permisos de micro y GPS para la validez legal", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        solicitudPermisosLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            val contexto = LocalContext.current
            val motor = remember { GrabadoraMotor(contexto) }

            // ESTADOS DE LA UI
            var listaGrabaciones by remember { mutableStateOf(motor.obtenerGrabaciones()) }
            var mostrarInfoTecnica by remember { mutableStateOf(false) }
            var archivoParaEliminar by remember { mutableStateOf<GrabacionDato?>(null) }
            var archivoParaRenombrar by remember { mutableStateOf<GrabacionDato?>(null) }

            // Función para refrescar la lista
            val actualizarLista = { listaGrabaciones = motor.obtenerGrabaciones() }

            // --- ESCUCHADOR PARA GRABACIONES EXTERNAS (BOTONES) ---
            LaunchedEffect(motor) {
                motor.onActualizarLista = {
                    actualizarLista()
                }
            }

            // RECEPTOR DE BROADCAST (Radio interna para el servicio)
            DisposableEffect(contexto) {
                val receptor = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        actualizarLista()
                    }
                }

                val filtro = IntentFilter("dev.jmcerezo.ACTUALIZAR_LISTA")
                val appContext = contexto.applicationContext

                // Registro blindado para evitar advertencias de Lint y errores de compilación
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(receptor, filtro, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    // En versiones anteriores, registramos de forma estándar.
                    // El IDE podría seguir quejándose visualmente sin el @SuppressLint arriba.
                    appContext.registerReceiver(receptor, filtro)
                }

                onDispose {
                    try {
                        appContext.unregisterReceiver(receptor)
                    } catch (e: Exception) { }
                }
            }
            // ------------------------------------------------------------

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(0xFF0F111A))
                    .navigationBarsPadding()
                    .statusBarsPadding()
            ) {
                // 1. CABECERA PRINCIPAL (Identidad: CENTINELA + Botón Info Técnica)
                TopBarApp(onInfoClick = { mostrarInfoTecnica = true })

                // 2. TARJETA DE CONTROL DE GRABACIÓN
                Box(modifier = Modifier.wrapContentHeight()) {
                    TarjetaGrabacion(
                        gestorAudio = motor,
                        alVerArchivos = { actualizarLista() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. SECCIÓN DE HISTORIAL (Título limpio)
                TopBarHistorial()

                // 4. LISTA DE EVIDENCIAS
                Box(modifier = Modifier.weight(1f)) {
                    ListaEvidencias(
                        lista = listaGrabaciones,
                        onPlay = { grabacion ->
                            motor.reproducirAudio(grabacion.archivo)
                            Toast.makeText(contexto, "Reproduciendo...", Toast.LENGTH_SHORT).show()
                        },
                        onRename = { grabacion -> archivoParaRenombrar = grabacion },
                        onShare = { grabacion -> motor.compartirArchivo(grabacion) },
                        onDelete = { grabacion -> archivoParaEliminar = grabacion }
                    )
                }

                // 5. PIE DE PÁGINA
                FooterApp()
            }

            // --- GESTIÓN DE DIÁLOGOS ---

            if (mostrarInfoTecnica) {
                DialogoInfoTecnica(onDismiss = { mostrarInfoTecnica = false })
            }

            archivoParaEliminar?.let { grabacion ->
                DialogoEliminar(
                    nombreArchivo = grabacion.archivo.nameWithoutExtension,
                    onConfirm = {
                        motor.eliminarGrabacion(grabacion.archivo)
                        actualizarLista()
                        archivoParaEliminar = null
                    },
                    onDismiss = { archivoParaEliminar = null }
                )
            }

            archivoParaRenombrar?.let { grabacion ->
                DialogoRenombrar(
                    nombreActual = grabacion.archivo.nameWithoutExtension,
                    onConfirm = { nuevoNombre ->
                        motor.renombrarGrabacion(grabacion.archivo, nuevoNombre)
                        actualizarLista()
                        archivoParaRenombrar = null
                    },
                    onDismiss = { archivoParaRenombrar = null }
                )
            }
        }
    }
}