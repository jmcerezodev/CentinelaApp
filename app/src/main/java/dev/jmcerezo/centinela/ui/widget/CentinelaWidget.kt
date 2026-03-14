package dev.jmcerezo.centinela.ui.widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
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
import dev.jmcerezo.centinela.core.service.ServicioBotones
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.util.SystemUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class CentinelaWidget : GlanceAppWidget() {

    companion object {
        val KEY_GRABANDO = booleanPreferencesKey("esta_grabando")
        val KEY_BOTONES = booleanPreferencesKey("botones_habilitados")
        val KEY_PERMANENTE = booleanPreferencesKey("servicio_permanente")
        val KEY_SILENCIOSO = booleanPreferencesKey("modo_silencioso")
        val PARAM_ACCION = ActionParameters.Key<String>("accion")
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<androidx.datastore.preferences.core.Preferences>()
            GlanceTheme {
                CentinelaWidgetContent(
                    grabando = state[KEY_GRABANDO] ?: false,
                    botones = state[KEY_BOTONES] ?: false,
                    permanente = state[KEY_PERMANENTE] ?: false,
                    silencioso = state[KEY_SILENCIOSO] ?: false
                )
            }
        }
    }
}

class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val accion = parameters[CentinelaWidget.PARAM_ACCION] ?: return
        val prefsApp = Preferencias(context)
        val motor = GrabadoraMotor.getInstance(context)

        // 1. Verificación universal del micrófono (Requerido por todos los botones)
        val tieneMicro = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        
        // 2. Determinación del permiso faltante según la lógica solicitada
        val permisoFaltante = when (accion) {
            "grabar" -> if (!tieneMicro) "MICROFONO" else null
            
            "botones" -> {
                if (!tieneMicro) "MICROFONO"
                else if (!SystemUtils.isAccessibilityServiceEnabled(context, ServicioBotones::class.java)) "ACCESIBILIDAD"
                else null
            }
            
            "permanente", "suspension" -> {
                if (!tieneMicro) "MICROFONO"
                else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) "NOTIFICACIONES"
                else null
            }
            
            else -> null
        }

        // Si falta algún permiso, abrimos la app para solicitarlo
        if (permisoFaltante != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("SOLICITAR_PERMISO", permisoFaltante)
            }
            if (intent != null) context.startActivity(intent)
            return
        }

        // 3. Ejecutar la acción lógica y Sincronizar el estado visual
        when (accion) {
            "grabar" -> {
                if (motor.estaGrabando) motor.detenerGrabacion() else motor.iniciarGrabacion()
                
                // FORZAR EL CAMBIO VISUAL AQUÍ
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    val mutable = prefs.toMutablePreferences()
                    mutable[CentinelaWidget.KEY_GRABANDO] = motor.estaGrabando
                    mutable
                }
            }
            "botones" -> {
                prefsApp.botonesHabilitados = !prefsApp.botonesHabilitados
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    val mutable = prefs.toMutablePreferences()
                    mutable[CentinelaWidget.KEY_BOTONES] = prefsApp.botonesHabilitados
                    mutable
                }
            }
            "permanente" -> {
                prefsApp.servicioPermanente = !prefsApp.servicioPermanente
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    val mutable = prefs.toMutablePreferences()
                    mutable[CentinelaWidget.KEY_PERMANENTE] = prefsApp.servicioPermanente
                    mutable
                }
            }
            "suspension" -> {
                prefsApp.modoSilencioso = !prefsApp.modoSilencioso
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    val mutable = prefs.toMutablePreferences()
                    mutable[CentinelaWidget.KEY_SILENCIOSO] = prefsApp.modoSilencioso
                    mutable
                }
            }
        }

        context.startService(Intent(context, CentinelaService::class.java))
        context.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(context.packageName))
        CentinelaWidget().update(context, glanceId)
    }
}

class CentinelaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CentinelaWidget()
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // IMPORTANTE: Escuchar la acción oficial de Android para que cargue en producción
        if (intent.action == "dev.jmcerezo.ACTUALIZAR_CONFIGURACION" || 
            intent.action == android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            
            val motor = GrabadoraMotor.getInstance(context)
            val prefsApp = Preferencias(context)

            MainScope().launch {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(CentinelaWidget::class.java)
                ids.forEach { id ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        val mutable = prefs.toMutablePreferences()
                        
                        Log.d("CENTINELA_WIDGET", "Sincronizando Grabación: ${motor.estaGrabando}")

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
