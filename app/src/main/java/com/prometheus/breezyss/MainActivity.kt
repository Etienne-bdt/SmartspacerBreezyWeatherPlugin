package com.prometheus.breezyss

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.prometheus.breezyss.ui.theme.SmartspacerBreezyWeatherPluginTheme

import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartspacerBreezyWeatherPluginTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SetupScreen()
                }
            }
        }
    }
}

@Composable
fun WeatherStatus(context: Context) {
    val prefs = remember { context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE) }
    val location = prefs.getString("last_location", null) ?: return
    val temp = prefs.getInt("last_temp", Int.MIN_VALUE)
    val condition = prefs.getString("last_condition", "Unknown")
    val iconName = prefs.getString("last_icon_name", "") ?: ""

    val drawable = BreezyWeatherResources.getWeatherDrawable(context, iconName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (drawable != null) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            setImageDrawable(drawable)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = location,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (temp != Int.MIN_VALUE) "$temp°C • " else ""}$condition",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SetupScreen() {
    val context = LocalContext.current

    val systemPermissions = mutableListOf<String>().apply {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    var hasNotifications by remember {
        mutableStateOf(
            systemPermissions.isEmpty() || systemPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasNotifications = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!hasNotifications && systemPermissions.isNotEmpty()) {
            launcher.launch(systemPermissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Breezy Weather Plugin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasNotifications && systemPermissions.isNotEmpty()) {
            Text(text = "Please allow notifications to receive weather updates in the background.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { launcher.launch(systemPermissions) }) {
                Text("Grant Notification Permission")
            }
        } else {
            Text(text = "✅ Ready", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            WeatherStatus(context)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("org.breezyweather")
                    if (intent != null) context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("1. Open Breezy Weather")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            component = ComponentName(
                                "org.breezyweather",
                                "org.breezyweather.ui.settings.activities.SettingsActivity"
                            )
                            putExtra("org.breezyweather.EXTRA_SHOW_FRAGMENT", "org.breezyweather.ui.settings.widgets.DataSharingFragment")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = context.packageManager.getLaunchIntentForPackage("org.breezyweather")
                        if (intent != null) context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("2. Enable Data Sharing")
            }

            Text(
                text = "In Breezy: Settings > External modules > Data sharing > Enable 'Smartspacer BreezyWeather Plugin'",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
