package com.reshimarket.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.reshimarket.R
import com.reshimarket.ReshimarketApp
import com.reshimarket.ui.MainActivity

/** ווידג'ט למסך הבית: כמה מוצרים חסרים ברשימה הפעילה */
class MissingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ReshimarketApp
        val lists = app.repository.lists()
        val wanted = app.settings.current.currentListId
        val list = lists.firstOrNull { it.id == wanted } ?: lists.firstOrNull()
        val count = list?.let { app.repository.missingCount(it.id) } ?: 0
        val title = list?.let { "${it.emoji} ${it.name}" } ?: "רשימרקט"
        provideContent { Content(title, count) }
    }

    @Composable
    private fun Content(title: String, count: Int) {
        val red = Color(0xFFD93A3F)
        val green = Color(0xFF178A4A)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFFFF8EE))
                .cornerRadius(22.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(provider = ImageProvider(R.drawable.ic_logo), contentDescription = null, modifier = GlanceModifier.size(28.dp))
                Spacer(GlanceModifier.width(6.dp))
                Text(title, style = TextStyle(color = ColorProvider(Color(0xFF8A4B17)), fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            Text(
                count.toString(),
                style = TextStyle(color = ColorProvider(if (count > 0) red else green), fontSize = 44.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                if (count > 0) "חסרים" else "הכל בבית",
                style = TextStyle(color = ColorProvider(Color(0xFF7A6252)), fontSize = 13.sp),
            )
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { MissingWidget().updateAll(context) }
        }
    }
}

class MissingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MissingWidget()
}
