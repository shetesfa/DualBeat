package com.tesfa.dualbeat.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    /**
     * Required media permissions depending on Android version.
     * Android 13+ (API 33+): READ_MEDIA_AUDIO and READ_MEDIA_VIDEO
     * Android 12 and below: READ_EXTERNAL_STORAGE
     */
    fun getRequiredMediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Checks if all required media read permissions are granted.
     */
    fun hasMediaPermissions(context: Context): Boolean {
        return getRequiredMediaPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Required Bluetooth permissions depending on Android version.
     * Android 12+ (API 31+): BLUETOOTH_CONNECT and BLUETOOTH_SCAN
     * Below Android 12: Normal permissions granted at install time
     */
    fun getRequiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            emptyArray()
        }
    }

    /**
     * Checks if required Bluetooth runtime permissions are granted.
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        val required = getRequiredBluetoothPermissions()
        return required.isEmpty() || required.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Notification permission for Android 13+ (API 33+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Check if any required permission was permanently denied by the user.
     * If user selected 'Don't ask again' or denied repeatedly, shouldShowRequestPermissionRationale is false.
     */
    fun isPermanentlyDenied(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { perm ->
            ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        }
    }

    /**
     * Safely opens the application settings page so the user can manually enable permissions.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
