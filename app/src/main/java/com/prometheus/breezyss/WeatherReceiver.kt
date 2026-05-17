package com.prometheus.breezyss

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

class WeatherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER" ||
            action == "org.breezyweather.ACTION_UPDATE_NOTIFIER"
        ) {
            val locationIds = intent.getStringArrayExtra("AllLocationIds")
            val idToQuery = (locationIds?.firstOrNull() ?: "gps")
                .filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == ':' }
                .take(64)
                .ifEmpty { "gps" }
            queryBreezyProvider(context, idToQuery)
        }
    }

    private fun queryBreezyProvider(context: Context, locationId: String) {
        val authority = "org.breezyweather.provider.weather"
        val uri = Uri.parse("content://$authority/weather")
        
        try {
            val cursor = context.contentResolver.query(uri, null, "id = ?", arrayOf(locationId), null)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val cityName = it.getString(it.getColumnIndexOrThrow("city"))
                    val weatherBlob = it.getBlob(it.getColumnIndexOrThrow("weather"))
                    
                    if (weatherBlob != null) {
                        val jsonStr = decompressGzip(weatherBlob)
                        val json = JSONObject(jsonStr)
                        
                        val current = json.optJSONObject("current")
                        val weatherObj = current?.optJSONObject("weather")

                        // Fetch current weather status text
                        val statusText = current?.optString("weatherText") ?: "unknown"

                        val baseCode = current?.optString("weatherCode") ?: "UNKNOWN"

                        val hour = java.time.LocalTime.now().hour
                        val isNight = hour < 6 || hour >= 20

                        val statusCode: String = when (baseCode) {
                            "clear", "partly_cloudy" -> {
                                val timeTag = if (isNight) "NIGHT" else "DAY"
                                "${baseCode}_$timeTag"
                            }
                            else -> baseCode
                        }

                        // Prepare resource name based on status: weather_{status}
                        val cleanStatus = statusCode?.lowercase()?.replace(" ", "_")
                        val resourceName = "weather_$cleanStatus"
                        val shortcutName = "weather_$cleanStatus"

                        val tempObj = current?.optJSONObject("temperature")?.optJSONObject("temperature")
                        val temp = tempObj?.optDouble("value", Double.NaN)?.toInt() ?: -1
                        
                        val dailyArray = json.optJSONArray("daily")
                        val today = dailyArray?.optJSONObject(0)
                        
                        val maxTemp = today?.optJSONObject("day")
                            ?.optJSONObject("temperature")?.optJSONObject("temperature")
                            ?.optInt("value", -1) ?: -1
                            
                        val minTemp = today?.optJSONObject("night")
                            ?.optJSONObject("temperature")?.optJSONObject("temperature")
                            ?.optInt("value", -1) ?: -1

                        saveWeatherData(context, cityName, temp, minTemp, maxTemp, statusText, resourceName, shortcutName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherReceiver", "Provider query failed", e)
        }
    }

    private fun saveWeatherData(
        context: Context, location: String, temp: Int, min: Int, max: Int, 
        condition: String, iconName: String, shortcutName: String
    ) {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_location", location)
            putInt("last_temp", temp)
            putInt("last_min_temp", if (min != -1) min else temp)
            putInt("last_max_temp", if (max != -1) max else temp)
            putString("last_condition", condition)
            putString("last_icon_name", iconName)
            putString("last_shortcut_name", shortcutName)
            putLong("last_update", System.currentTimeMillis())
            apply()
        }
        SmartspacerTargetProvider.notifyChange(context, SmartspacerWeatherTarget::class.java)
    }

    private fun decompressGzip(contentBytes: ByteArray): String {
        return try {
            val out = StringBuilder()
            GZIPInputStream(ByteArrayInputStream(contentBytes)).use { gzip ->
                InputStreamReader(gzip, "UTF-8").use { reader ->
                    val buffer = CharArray(1024)
                    var n: Int
                    while (reader.read(buffer).also { n = it } != -1) {
                        out.append(buffer, 0, n)
                    }
                }
            }
            out.toString()
        } catch (e: Exception) {
            Log.e("WeatherReceiver", "Decompression failed", e)
            ""
        }
    }
}
