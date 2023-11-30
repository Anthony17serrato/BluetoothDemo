package com.serratocreations.bluetoothdemoclient.common.permissions

import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class PermissionsHelper @Inject constructor(
    private val activity: Activity
) {
    private val _results = Channel<PermissionsResult>(Channel.UNLIMITED)
    val results: ReceiveChannel<PermissionsResult> = _results

    private fun hasPermission(permission: String?): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission!!
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermissions(vararg permissions: String) : Boolean {
        var areAllPermissionsGranted = true
        for (permission in permissions) {
            if (!hasPermission(permission)) {
                areAllPermissionsGranted = false
                break
            }
        }
        return areAllPermissionsGranted
    }

    fun requestPermissionsIfRequired(vararg permissions: String, requestCode: Int) {
        if (!hasPermissions(*permissions)) {
            requestPermissions(permissions = permissions, requestCode = requestCode)
        } else {
            Timber.i("requestPermissionsIfRequired permissions already granted")
        }
    }

    private fun requestPermissions(vararg permissions: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        for (permission in permissions) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _results.trySend(PermissionsResult.PermissionGranted(permission, requestCode))
                Toast.makeText(activity, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    _results.trySend(PermissionsResult.PermissionDeclined(permission, requestCode))
                    Toast.makeText(activity, "Permission declined can show rational", Toast.LENGTH_LONG).show()
                } else {
                    _results.trySend(PermissionsResult.PermissionDeclinedDontAskAgain(permission, requestCode))
                    Toast.makeText(activity, "Permission permanently declined", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

sealed class PermissionsResult {
    data class PermissionGranted(val permission: String, val requestCode: Int) : PermissionsResult()
    data class PermissionDeclined(val permission: String, val requestCode: Int) : PermissionsResult()
    data class PermissionDeclinedDontAskAgain(val permission: String, val requestCode: Int) : PermissionsResult()
}