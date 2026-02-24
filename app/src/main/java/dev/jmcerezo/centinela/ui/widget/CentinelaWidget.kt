package dev.jmcerezo.centinela.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.jmcerezo.centinela.R
import dev.jmcerezo.centinela.core.service.CentinelaService
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * WIDGET PREMIUM CENTINELA
 * 
 * Implementa persistencia reactiva mediante GlanceStateDefinition para asegurar
 * que la interfaz se actualice visualmente al instante.
 */
class CentinelaWidget : GlanceAppWidget() {

    companion object {
        val KEY_BOTONES = booleanPreferencesKey("botones_habilitados")
        val KEY_PERMANENTE = booleanPreferencesKey("servicio_permanente")
        val KEY_SILENCIOSO = booleanPreferencesKey("modo_silencioso")
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                CentinelaWidgetContent()
            }
        }
    }

    @Composable
    private fun CentinelaWidgetContent() {
        // Obtenemos el estado actual del widget (reactivo)
        val state = currentState<androidx.datastore.preferences.core.Preferences>()
        val botones = state[KEY_BOTONES] ?: true
        val permanente = state[KEY_PERMANENTE] ?: false
        val silencioso = state[KEY_SILENCIOSO] ?: false
        
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg))
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ITEM 1: BOTONES
            IconoControl(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = android.R.drawable.ic_btn_speak_now,
                activo = botones,
                onAction = actionRunCallback<ToggleAction>(
                    actionParametersOf(ActionParameters.Key<String>("pref") to "botones")
                )
            )

            VerticalDividerLine()

            // ITEM 2: PERMANENTE
            IconoControl(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = android.R.drawable.ic_lock_idle_lock,
                activo = permanente,
                onAction = actionRunCallback<ToggleAction>(
                    actionParametersOf(ActionParameters.Key<String>("pref") to "permanente")
                )
            )

            VerticalDividerLine()

            // ITEM 3: ANTI-SUSPENSIÓN
            IconoControl(
                modifier = GlanceModifier.defaultWeight(),
                iconRes = android.R.drawable.ic_media_play,
                activo = silencioso,
                onAction = actionRunCallback<ToggleAction>(
                    actionParametersOf(ActionParameters.Key<String>("pref") to "suspension")
                )
            )
        }
    }

    @Composable
    private fun IconoControl(modifier: GlanceModifier, iconRes: Int, activo: Boolean, onAction: androidx.glance.action.Action) {
        Column(
            modifier = modifier.fillMaxHeight().clickable(onAction),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(provider = ImageProvider(iconRes), contentDescription = null, modifier = GlanceModifier.size(30.dp))
            Spacer(modifier = GlanceModifier.height(6.dp))
            Image(
                provider = ImageProvider(if (activo) R.drawable.indicador_on else R.drawable.indicador_off),
                contentDescription = null,
                modifier = GlanceModifier.size(8.dp)
            )
        }
    }

    @Composable
    private fun VerticalDividerLine() {
        Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp).background(ImageProvider(R.drawable.indicador_off))) {}
    }
}

/**
 * Acción que actualiza tanto el estado del Widget (para la UI) como SharedPreferences (para la App).
 */
class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefKey = parameters[ActionParameters.Key<String>("pref")] ?: return
        val prefsApp = Preferencias(context)

        // 1. Actualizamos el estado interno del Widget (Atómicamente para forzar redibujado)
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

        // 2. Sincronizamos servicios y avisamos a la App
        context.startService(Intent(context, CentinelaService::class.java))
        context.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(context.packageName))

        // 3. Forzamos redibujado de todas las instancias
        CentinelaWidget().updateAll(context)
    }
}

class CentinelaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CentinelaWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "dev.jmcerezo.ACTUALIZAR_CONFIGURACION") {
            val prefsApp = Preferencias(context)
            MainScope().launch {
                // Sincronizamos el estado del widget con el de la app cuando esta cambia internamente
                val glanceIds = androidx.glance.appwidget.GlanceAppWidgetManager(context).getGlanceIds(CentinelaWidget::class.java)
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
