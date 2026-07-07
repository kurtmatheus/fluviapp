package br.com.gruponaveg.ui.components

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestMultiplePermissions(
    context: Context,
    onGrantedPermission: () -> Unit,
    onDeniedPermission: () -> Unit,
    permissionsList: List<String>,
) {

    val state = rememberMultiplePermissionsState(permissions = permissionsList)

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allIsGranted = permissions.values.all { it }
        if (allIsGranted) {
            onGrantedPermission()
        } else {
            onDeniedPermission()
        }
    }

    LaunchedEffect(state) {
        if (
            permissionsList.all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            onGrantedPermission()
        } else {
            requestPermissionLauncher.launch(permissionsList.toTypedArray())
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermission(
    context: Context,
    onGrantedPermission: () -> Unit,
    onDeniedPermission: () -> Unit,
    permission: String,
) {

    val state = rememberPermissionState(
        permission = permission
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGrantedPermission()
        } else {
            onDeniedPermission()
        }
    }

    LaunchedEffect(state) {
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            onGrantedPermission()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }
}