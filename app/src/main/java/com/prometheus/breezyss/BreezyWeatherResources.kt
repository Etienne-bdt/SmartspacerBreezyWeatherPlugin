package com.prometheus.breezyss

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.core.content.res.ResourcesCompat

object BreezyWeatherResources {

    private const val PACKAGE_NAME = "org.breezyweather"

    fun getRemoteContext(context: Context, packageName: String = PACKAGE_NAME): Context? {
        return try {
            context.createPackageContext(
                packageName,
                Context.CONTEXT_IGNORE_SECURITY
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves a drawable directly using the resource name provided by the content provider.
     */
    fun getWeatherDrawable(
        context: Context,
        resourceName: String
    ): Drawable? {
        if (resourceName.isEmpty()) return null
        val remote = getRemoteContext(context) ?: return null

        val resId = remote.resources.getIdentifier(resourceName, "drawable", PACKAGE_NAME)
        android.util.Log.d("BreezyResources", "getWeatherDrawable: $resourceName -> resId: $resId")
        if (resId != 0) {
            return ResourcesCompat.getDrawable(remote.resources, resId, null)
        }
        return null
    }

    /**
     * Resolves an icon directly using the resource name provided by the content provider.
     */
    fun getWeatherIcon(
        context: Context,
        resourceName: String
    ): Icon? {
        if (resourceName.isEmpty()) return null
        val remote = getRemoteContext(context) ?: return null

        val resId = remote.resources.getIdentifier(resourceName, "drawable", PACKAGE_NAME)
        android.util.Log.d("BreezyResources", "getWeatherIcon: $resourceName -> resId: $resId")
        if (resId != 0) {
            return Icon.createWithResource(PACKAGE_NAME, resId)
        }
        return null
    }
}
