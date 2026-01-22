package com.palmfarm.manager.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Helper class for handling runtime permissions
 */
object PermissionHelper {

    // Permission request codes
    const val REQUEST_CAMERA = 100
    const val REQUEST_STORAGE_WRITE = 101
    const val REQUEST_STORAGE_READ = 102
    const val REQUEST_STORAGE_ALL = 103

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if storage write permission is granted
     */
    fun isStorageWritePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses scoped storage, no need for WRITE_EXTERNAL_STORAGE
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if storage read permission is granted
     */
    fun isStorageReadPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses new media permissions
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request camera permission from Activity
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA
        )
    }

    /**
     * Request camera permission from Fragment
     */
    fun requestCameraPermission(fragment: Fragment) {
        fragment.requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA
        )
    }

    /**
     * Request storage permissions from Activity
     */
    fun requestStoragePermissions(activity: Activity) {
        val permissions = getStoragePermissions()
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            REQUEST_STORAGE_ALL
        )
    }

    /**
     * Request storage permissions from Fragment
     */
    fun requestStoragePermissions(fragment: Fragment) {
        val permissions = getStoragePermissions()
        fragment.requestPermissions(
            permissions,
            REQUEST_STORAGE_ALL
        )
    }

    /**
     * Get appropriate storage permissions based on Android version
     */
    private fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 9 and below
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Handle permission result
     */
    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_CAMERA, REQUEST_STORAGE_WRITE, REQUEST_STORAGE_READ, REQUEST_STORAGE_ALL -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }

    /**
     * Check if permission should show rationale
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Get permission rationale message
     */
    fun getPermissionRationaleMessage(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA ->
                "Camera permission is required to capture photos for receipts and documentation."
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE ->
                "Storage permission is required to save and restore database backups, generate PDF reports, and store receipts."
            Manifest.permission.READ_MEDIA_IMAGES ->
                "Media permission is required to access images for receipts and documentation."
            else -> "This permission is required for the app to function properly."
        }
    }

    /**
     * Check if all required permissions are granted for backup/restore
     */
    fun hasBackupRestorePermissions(context: Context): Boolean {
        return isStorageWritePermissionGranted(context) && isStorageReadPermissionGranted(context)
    }

    /**
     * Check if all required permissions are granted for camera features
     */
    fun hasCameraPermissions(context: Context): Boolean {
        return isCameraPermissionGranted(context) && isStorageWritePermissionGranted(context)
    }

    /**
     * Request backup/restore permissions
     */
    fun requestBackupRestorePermissions(activity: Activity) {
        requestStoragePermissions(activity)
    }

    /**
     * Request backup/restore permissions from Fragment
     */
    fun requestBackupRestorePermissions(fragment: Fragment) {
        requestStoragePermissions(fragment)
    }

    /**
     * Request camera feature permissions (camera + storage)
     */
    fun requestCameraFeaturePermissions(activity: Activity) {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        permissions.addAll(getStoragePermissions())

        ActivityCompat.requestPermissions(
            activity,
            permissions.toTypedArray(),
            REQUEST_CAMERA
        )
    }

    /**
     * Request camera feature permissions from Fragment
     */
    fun requestCameraFeaturePermissions(fragment: Fragment) {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        permissions.addAll(getStoragePermissions())

        fragment.requestPermissions(
            permissions.toTypedArray(),
            REQUEST_CAMERA
        )
    }
}
