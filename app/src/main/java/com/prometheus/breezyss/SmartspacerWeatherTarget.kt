package com.prometheus.breezyss

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon as SmartspacerIcon
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate

class SmartspacerWeatherTarget : SmartspacerTargetProvider() {

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val context = provideContext()
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        
        val location = prefs.getString("last_location", null) ?: return emptyList()
        val temp = prefs.getInt("last_temp", Int.MIN_VALUE)
        val minTemp = prefs.getInt("last_min_temp", Int.MIN_VALUE)
        val maxTemp = prefs.getInt("last_max_temp", Int.MIN_VALUE)
        val condition = prefs.getString("last_condition", "Unknown")
        val shortcutName = prefs.getString("last_shortcut_name", "") ?: ""
        
        val tempText = if (temp != Int.MIN_VALUE) "$temp°C" else ""
        val rangeText = if ((minTemp != Int.MIN_VALUE) && (maxTemp != Int.MIN_VALUE)) " ($minTemp°/$maxTemp°)" else ""
        
        // Use the shortcut name directly from the provider
        val icon = BreezyWeatherResources.getWeatherIcon(context, shortcutName)

        val smartspacerIcon = if (icon != null) {
            SmartspacerIcon(icon)
        } else {
            SmartspacerIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
        }

        val locale = java.util.Locale.getDefault()

        val sdfDay = java.text.SimpleDateFormat("EEE", locale)
        val sdfMonth = java.text.SimpleDateFormat("MMM", locale)
        val sdfDayNum = java.text.SimpleDateFormat("d", locale)

        val date = java.util.Date()

        val dateText =
            "${sdfDay.format(date).replace(".", "").replaceFirstChar { it.uppercase() }}. " +
                    "${sdfDayNum.format(date)} " +
                    sdfMonth.format(date).replaceFirstChar { it.uppercase() }

        val launchIntent = context.packageManager.getLaunchIntentForPackage("org.breezyweather")?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val launchPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context, 100, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val target = TargetTemplate.Basic(
            id = "breezy_weather_$smartspacerId",
            componentName = ComponentName(context, SmartspacerWeatherTarget::class.java),
            title = Text(dateText),
            subtitle = Text("$tempText$rangeText $condition • $location"),
            icon = smartspacerIcon,
            onClick = launchPendingIntent?.let { TapAction(id = "tap_action", pendingIntent = it) }
        ).create()

        return listOf(target)
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = "Breezy Weather",
            description = "Shows weather from Breezy Weather",
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_launcher_foreground),
            configActivity = Intent(provideContext(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }
}
