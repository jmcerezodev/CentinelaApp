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
import dev.jmcerezo.centinela.core.service.CentinelaService
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * LÓGICA DEL WIDGET CENTINELA
 * 
 * Gestiona el estado reactivo y la sincronización con la aplicación.
 * La interfaz visual se delega a CentinelaWidgetUI.kt.
 */
class CentinelaWidget : GlanceAppWidget() {

    companion object {
        val KEY_BOTONES = booleanPreferencesKey("botones_habilitados")
        val KEY_PERMANENTE = booleanPreferencesKey("servicio_permanente")
        val KEY_SILENCIOSO = booleanPreferencesKey("modo_silencioso")
        
        // Clave para identificar qué preferencia se cambia desde el widget
        val PARAM_KEY = ActionParameters.Key<String>("pref")
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Obtenemos el estado reactivo de Glance
            val state = currentState<androidx.datastore.preferences.core.Preferences>()
            
            GlanceTheme {
                // Llamamos a la interfaz visual pasándole los estados actuales
                CentinelaWidgetContent(
                    botones = state[KEY_BOTONES] ?: true,
                    permanente = state[KEY_PERMANENTE] ?: false,
                    silencioso = state[KEY_SILENCIOSO] ?: false
                )
            }
        }
    }
}

/**
 * Procesa las pulsaciones en los iconos del widget.
 */
class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefKey = parameters[CentinelaWidget.PARAM_KEY] ?: return
        val prefsApp = Preferencias(context)

        // 1. Actualización Atómica del Estado del Widget (Fuerza el redibujado visual)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { currentPrefs ->
            val mutablePrefs = currentPrefs.toMutablePreferences()
            when (prefKey) {
                "botones" -> {
                    val newVal = !(currentPrefs[CentinelaWidget.KEY_BOTONES] ?: true)
                    mutablePrefs[CentinelaWidget.KEY_BOTONES] = newVal
                    prefsApp.botonesHabilitados = newVal
                }
                "permanente" -> {
                    val newVal = !(currentPrefs[CentinelaWidget.KEY_PERMANENTE] ?: false)
                    mutablePrefs[CentinelaWidget.KEY_PERMANENTE] = newVal
                    prefsApp.servicioPermanente = newVal
                }
                "suspension" -> {
                    val newVal = !(currentPrefs[CentinelaWidget.KEY_SILENCIOSO] ?: false)
                    mutablePrefs[CentinelaWidget.KEY_SILENCIOSO] = newVal
                    prefsApp.modoSilencioso = newVal
                }
            }
            mutablePrefs
        }

        // 2. Notificación a servicios
        context.startService(Intent(context, CentinelaService::class.java))
        context.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(context.packageName))

        // 3. Forzamos el refresco inmediato de la vista
        CentinelaWidget().update(context, glanceId)
    }
}

/**
 * Receptor encargado de actualizar el widget cuando la App cambia los valores.
 */
class CentinelaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CentinelaWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "dev.jmcerezo.ACTUALIZAR_CONFIGURACION") {
            val prefsApp = Preferencias(context)
            MainScope().launch {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(CentinelaWidget::class.java)
                glanceIds.forEach { id ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        val mutable = prefs.toMutablePreferences()
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
