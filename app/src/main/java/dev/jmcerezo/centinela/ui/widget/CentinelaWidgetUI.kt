package dev.jmcerezo.centinela.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import dev.jmcerezo.centinela.R

/**
 * INTERFAZ VISUAL PREMIUM DEL WIDGET CENTINELA
 * 
 * Diseño minimalista basado en iconos con botón de grabación directa.
 * Optimizado para ser apilable en las pilas de widgets de Android.
 */
@Composable
fun CentinelaWidgetContent(
    grabando: Boolean,
    botones: Boolean,
    permanente: Boolean,
    silencioso: Boolean
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- BOTÓN PRINCIPAL DE GRABACIÓN (🎙️ / ⏹️) ---
        WidgetIconAction(
            iconRes = if (grabando) android.R.drawable.ic_media_pause else android.R.drawable.ic_btn_speak_now,
            activo = grabando,
            prefKey = "grabar",
            isMain = true
        )

        VerticalLine()

        // --- MODO 1: GRABACIÓN POR BOTONES (⚙️) ---
        WidgetIconAction(
            iconRes = android.R.drawable.ic_menu_preferences,
            activo = botones,
            prefKey = "botones"
        )

        // --- MODO 2: SERVICIO PERMANENTE (🛡️) ---
        WidgetIconAction(
            iconRes = android.R.drawable.ic_lock_idle_lock,
            activo = permanente,
            prefKey = "permanente"
        )

        // --- MODO 3: ANTI-SUSPENSIÓN (⚡) ---
        WidgetIconAction(
            iconRes = android.R.drawable.ic_media_play,
            activo = silencioso,
            prefKey = "suspension"
        )
    }
}

/**
 * Elemento de icono individual con lógica de visualización profesional.
 */
@Composable
private fun RowScope.WidgetIconAction(
    iconRes: Int,
    activo: Boolean,
    prefKey: String,
    isMain: Boolean = false
) {
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .fillMaxHeight()
            .clickable(actionRunCallback<ToggleAction>(
                actionParametersOf(CentinelaWidget.PARAM_ACCION to prefKey)
            )),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aplicamos color rojo al icono principal si está grabando, blanco al resto
        val tintColor = if (isMain && activo) GlanceTheme.colors.error else GlanceTheme.colors.onSurface
        
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(if (isMain) 32.dp else 26.dp),
            colorFilter = ColorFilter.tint(tintColor)
        )
        
        Spacer(modifier = GlanceModifier.height(6.dp))
        
        // Indicador LED de estado
        Image(
            provider = ImageProvider(if (activo) R.drawable.indicador_on else R.drawable.indicador_off),
            contentDescription = null,
            modifier = GlanceModifier.size(8.dp)
        )
    }
}

@Composable
private fun VerticalLine() {
    Box(
        modifier = GlanceModifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .background(ImageProvider(R.drawable.indicador_off))
    ) {}
}
