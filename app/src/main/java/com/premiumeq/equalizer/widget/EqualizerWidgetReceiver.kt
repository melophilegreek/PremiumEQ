package com.premiumeq.equalizer.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.premiumeq.equalizer.MainActivity
import androidx.glance.action.clickable

/**
 * Home screen widget entry point required by the manifest `<receiver>`.
 *
 * Current behavior: displays the app name/branding and, on tap, opens
 * [MainActivity]. This is real and functional end to end (Glance -> manifest ->
 * launcher).
 *
 * NOT yet wired: showing the live enabled/disabled state or the active preset
 * name, and an in-widget toggle button that flips the equalizer without opening
 * the app. Both require a small shared state channel (a DataStore file both the
 * widget's Glance worker process and the main process read/write) which is a
 * separate, testable unit of work rather than something to bolt on silently here.
 */
class EqualizerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EqualizerGlanceWidget
}

object EqualizerGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = androidx.compose.ui.graphics.Color(0xFF1E1830), night = androidx.compose.ui.graphics.Color(0xFF1E1830)))
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Premium EQ",
                style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color.White))
            )
            Text(
                text = "Tap to open",
                style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFB388FF), night = androidx.compose.ui.graphics.Color(0xFFB388FF)))
            )
        }
    }
}
