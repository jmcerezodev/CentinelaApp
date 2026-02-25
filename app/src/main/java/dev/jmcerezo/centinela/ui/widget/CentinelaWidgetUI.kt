package dev.jmcerezo.centinela.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import dev.jmcerezo.centinela.R

/**
 * INTERFAZ VISUAL PREMIUM DEL WIDGET CENTINELA
 * 
 * Diseño de tarjetas que simulan botones físicos.
 * Las esquinas exteriores de los laterales están redondeadas (24dp).
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
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- TARJETA 1: REC / STOP (Redondeada Izquierda) ---
        WidgetCard(
            activo = grabando,
            prefKey = "grabar",
            bgRes = R.drawable.btn_left_rounded
        ) {
            val tint = if (grabando) Color(0xFFFF5252) else Color.White
            Image(
                provider = ImageProvider(if (grabando) android.R.drawable.ic_media_pause else android.R.drawable.ic_btn_speak_now),
                contentDescription = null,
                modifier = GlanceModifier.size(26.dp),
                colorFilter = ColorFilter.tint(ColorProvider(tint, tint))
            )
        }

        Spacer(modifier = GlanceModifier.width(2.dp))

        // --- TARJETA 2: BOTONES (Cuadrada) ---
        WidgetCard(
            activo = botones,
            prefKey = "botones",
            bgRes = R.drawable.btn_square
        ) {
            WidgetLabel("BOTONES", botones)
        }

        Spacer(modifier = GlanceModifier.width(2.dp))

        // --- TARJETA 3: SERVICIO (Cuadrada) ---
        WidgetCard(
            activo = permanente,
            prefKey = "permanente",
            bgRes = R.drawable.btn_square
        ) {
            WidgetLabel("SERV PERMANENTE", permanente)
        }

        Spacer(modifier = GlanceModifier.width(2.dp))

        // --- TARJETA 4: VIGILIA (Redondeada Derecha) ---
        WidgetCard(
            activo = silencioso,
            prefKey = "suspension",
            bgRes = R.drawable.btn_right_rounded
        ) {
            WidgetLabel("ANTI-SUSPENSION", silencioso)
        }
    }
}

/**
 * Módulo de tarjeta individual que simula un botón.
 */
@Composable
private fun RowScope.WidgetCard(
    activo: Boolean,
    prefKey: String,
    bgRes: Int,
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .fillMaxHeight()
            .background(ImageProvider(bgRes))
            .clickable(actionRunCallback<ToggleAction>(
                actionParametersOf(CentinelaWidget.PARAM_ACCION to prefKey)
            )),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
            Spacer(modifier = GlanceModifier.height(6.dp))
            // Punto LED indicador
            Image(
                provider = ImageProvider(if (activo) R.drawable.indicador_on else R.drawable.indicador_off),
                contentDescription = null,
                modifier = GlanceModifier.size(6.dp)
            )
        }
    }
}

/**
 * Estilo de texto para las tarjetas de control.
 */
@Composable
private fun WidgetLabel(text: String, activo: Boolean) {
    val color = if (activo) Color.White else Color(0xFF808080)
    Text(
        text = text,
        style = TextStyle(
            color = ColorProvider(color, color),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
}
