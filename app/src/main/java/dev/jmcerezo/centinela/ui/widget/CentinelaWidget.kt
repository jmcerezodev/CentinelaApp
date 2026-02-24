package dev.jmcerezo.centinela.ui.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.core.service.CentinelaService
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * LÓGICA DEL WIDGET CENTINELA
 * 
 * Gestiona el estado reactivo y las acciones del usuario (toggles y grabación directa).
 * La interfaz visual se delega a CentinelaWidgetUI.kt.
 */
class CentinelaWidget : GlanceAppWidget() {

    companion object {
        val KEY_GRABANDO = booleanPreferencesKey("esta_grabando")
        val KEY_BOTONES = booleanPreferencesKey("botones_habilitados")
        val KEY_PERMANENTE = booleanPreferencesKey("servicio_permanente")
        val KEY_SILENCIOSO = booleanPreferencesKey("modo_silencioso")

        // Clave para identificar qué acción se ejecuta desde el widget
        val PARAM_ACCION = ActionParameters.Key<String>("accion")
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Obtenemos el estado reactivo de Glance
            val state = currentState<androidx.datastore.preferences.core.Preferences>()

            GlanceTheme {
                // Llamamos a la interfaz visual pasándole los estados actuales
                CentinelaWidgetContent(
                    grabando = state[KEY_GRABANDO] ?: false,
                    botones = state[KEY_BOTONES] ?: true,
                    permanente = state[KEY_PERMANENTE] ?: false,
                    silencioso = state[KEY_SILENCIOSO] ?: false
                )
            }
        }
    }
}

/**
 * Gestor de acciones del Widget.
 * Procesa los clics en los iconos y sincroniza tanto la base de datos como los servicios.
 */
class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val accion = parameters[CentinelaWidget.PARAM_ACCION] ?: return
        val prefsApp = Preferencias(context)
        val motor = GrabadoraMotor.getInstance(context)

        // Actualizamos el estado interno del widget para que el redibujado sea inmediato
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { currentPrefs ->
            val mutablePrefs = currentPrefs.toMutablePreferences()
            
            when (accion) {
                "grabar" -> {
                    if (motor.estaGrabando) motor.detenerGrabacion() else motor.iniciarGrabacion()
                    mutablePrefs[CentinelaWidget.KEY_GRABANDO] = motor.estaGrabando
                }
                "botones" -> {
                    val nuevoValor = !(currentPrefs[CentinelaWidget.KEY_BOTONES] ?: true)
                    mutablePrefs[CentinelaWidget.KEY_BOTONES] = nuevoValor
                    prefsApp.botonesHabilitados = nuevoValor
                }
                "permanente" -> {
                    val nuevoValor = !(currentPrefs[CentinelaWidget.KEY_PERMANENTE] ?: false)
                    mutablePrefs[CentinelaWidget.KEY_PERMANENTE] = nuevoValor
                    prefsApp.servicioPermanente = nuevoValor
                }
                "suspension" -> {
                    val nuevoValor = !(currentPrefs[CentinelaWidget.KEY_SILENCIOSO] ?: false)
                    mutablePrefs[CentinelaWidget.KEY_SILENCIOSO] = nuevoValor
                    prefsApp.modoSilencioso = nuevoValor
                }
            }
            mutablePrefs
        }

        // Sincronizamos con la aplicación y servicios en segundo plano
        context.startService(Intent(context, CentinelaService::class.java))
        context.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(context.packageName))

        // Forzamos el redibujado de la interfaz
        CentinelaWidget().update(context, glanceId)
    }
}

class CentinelaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CentinelaWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "dev.jmcerezo.ACTUALIZAR_CONFIGURACION") {
            val motor = GrabadoraMotor.getInstance(context)
            val prefsApp = Preferencias(context)

            MainScope().launch {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(CentinelaWidget::class.java)
                ids.forEach { id ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        val mutable = prefs.toMutablePreferences()
                        mutable[CentinelaWidget.KEY_GRABANDO] = motor.estaGrabando
                        mutable[CentinelaWidget.KEY_BOTONES] = prefsApp.botonesHabilitados
                        mutable[CentinelaWidget.KEY_PERMANENTE] = prefsApp.servicioPermanente
                        mutable[CentinelaWidget.KEY_SILENCIOSO] = prefsApp.modoSilencioso
                        mutable
                    }
                    CentinelaWidget().update(context, id)
                }
            }
        }
    }
}
