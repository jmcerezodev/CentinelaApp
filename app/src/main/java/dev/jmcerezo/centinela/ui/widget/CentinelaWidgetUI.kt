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
 * INTERFAZ VISUAL DEL WIDGET CENTINELA
 * 
 * Diseño minimalista y profesional basado en iconos.
 * Optimizado para ser apilable en las pilas de widgets de Android.
 */
@Composable
fun CentinelaWidgetContent(
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
        // Icono 1: Micrófono (Grabación con botones)
        WidgetIconElement(
            iconRes = android.R.drawable.ic_btn_speak_now,
            activo = botones,
            prefKey = "botones"
        )

        VerticalLine()

        // Icono 2: Escudo (Servicio Permanente)
        WidgetIconElement(
            iconRes = android.R.drawable.ic_lock_idle_lock,
            activo = permanente,
            prefKey = "permanente"
        )

        VerticalLine()

        // Icono 3: Rayo (Modo Anti-Suspensión)
        WidgetIconElement(
            iconRes = android.R.drawable.ic_media_play,
            activo = silencioso,
            prefKey = "suspension"
        )
    }
}

@Composable
private fun RowScope.WidgetIconElement(
    iconRes: Int,
    activo: Boolean,
    prefKey: String
) {
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .fillMaxHeight()
            .clickable(actionRunCallback<ToggleAction>(
                actionParametersOf(CentinelaWidget.PARAM_KEY to prefKey)
            )),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filtro de color forzado para uniformidad profesional (Blanco brillante)
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(28.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
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
