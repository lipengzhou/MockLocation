package com.lipengzhou.mocklocation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lipengzhou.mocklocation.location.MockLocationService
import com.lipengzhou.mocklocation.state.StartMockAction
import com.lipengzhou.mocklocation.ui.MockLocationScreen
import com.lipengzhou.mocklocation.ui.theme.MockLocationTheme
import com.lipengzhou.mocklocation.viewmodel.MockLocationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MockLocationTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val viewModel: MockLocationViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.onPermissionsResult()
                    if (shouldOpenLocationPermissionSettingsAfterRequest()) {
                        viewModel.onRuntimePermissionSettingsRequired("定位权限")
                        context.openAppSettings()
                    } else if (shouldOpenNotificationPermissionSettingsAfterRequest()) {
                        viewModel.onRuntimePermissionSettingsRequired("通知权限")
                        context.openAppSettings()
                    }
                }
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.onPermissionsResult()
                    if (shouldOpenLocationPermissionSettingsAfterRequest()) {
                        viewModel.onRuntimePermissionSettingsRequired("定位权限")
                        context.openAppSettings()
                    }
                }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {
                    viewModel.onPermissionsResult()
                    if (shouldOpenNotificationPermissionSettingsAfterRequest()) {
                        viewModel.onRuntimePermissionSettingsRequired("通知权限")
                        context.openAppSettings()
                    }
                }

                LaunchedEffect(viewModel) {
                    viewModel.refreshRuntimeState()
                }

                LaunchedEffect(viewModel, uiState.hasAcceptedAgreement) {
                    if (uiState.hasAcceptedAgreement) {
                        viewModel.checkForUpdatesIfNeeded()
                    }
                }

                DisposableEffect(lifecycleOwner, viewModel) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshRuntimeState()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(viewModel, permissionLauncher) {
                    viewModel.startMockAction.collect { action ->
                        if (action == StartMockAction.RequestPermissions) {
                            viewModel.onRuntimePermissionRequestStarted()
                            permissionLauncher.launch(requiredPermissions())
                        }
                    }
                }

                DisposableEffect(context, viewModel) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(receiverContext: Context, intent: Intent) {
                            if (intent.action == MockLocationService.ACTION_STATUS) {
                                viewModel.onServiceStatusChanged(
                                    isRunning = intent.getBooleanExtra(
                                        MockLocationService.EXTRA_STATUS_RUNNING,
                                        false
                                    ),
                                    statusMessage = intent.getStringExtra(
                                        MockLocationService.EXTRA_STATUS_MESSAGE
                                    )
                                )
                            }
                        }
                    }
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(MockLocationService.ACTION_STATUS),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }

                MockLocationScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onPageSelected = viewModel::selectPage,
                    onSearchClick = viewModel::openSearchPage,
                    onSearchKeywordChange = viewModel::onSearchKeywordChange,
                    onSearchKeyword = viewModel::searchPoi,
                    onSearchClear = viewModel::clearSearch,
                    onSearchBack = viewModel::closeSearchPage,
                    onSearchResultSelected = viewModel::selectSearchResult,
                    onSearchHistoryDelete = viewModel::deleteSearchHistory,
                    onPointSelected = viewModel::onMapPointSelected,
                    onCoordinateInputConfirmed = viewModel::onCoordinateInputConfirmed,
                    onLocateCurrentPosition = viewModel::locateCurrentPosition,
                    onStart = viewModel::startMocking,
                    onStop = viewModel::stopMocking,
                    onRequestPermissions = {
                        viewModel.onRuntimePermissionRequestStarted()
                        permissionLauncher.launch(requiredPermissions())
                    },
                    onRequestLocationPermission = {
                        if (shouldOpenLocationPermissionSettings(uiState.permissions.hasRequestedLocationPermission)) {
                            viewModel.onRuntimePermissionSettingsRequired("定位权限")
                            context.openAppSettings()
                        } else {
                            viewModel.onLocationPermissionRequestStarted()
                            locationPermissionLauncher.launch(locationPermissions())
                        }
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (
                                shouldOpenNotificationPermissionSettings(
                                    uiState.permissions.hasRequestedNotificationPermission
                                )
                            ) {
                                viewModel.onRuntimePermissionSettingsRequired("通知权限")
                                context.openAppSettings()
                            } else {
                                viewModel.onNotificationPermissionRequestStarted()
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.onPermissionsResult()
                        }
                    },
                    onOpenDeveloperSettings = {
                        context.openSettings(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    },
                    onOpenApplicationSettings = {
                        context.openSettings(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    },
                    onOpenLocationSettings = {
                        context.openSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    onRefreshRuntimeState = viewModel::refreshRuntimeState,
                    onCopyDiagnostics = viewModel::copyDiagnostics,
                    onLatitudeChange = viewModel::onLatitudeChange,
                    onLongitudeChange = viewModel::onLongitudeChange,
                    onAltitudeChange = viewModel::onAltitudeChange,
                    onUpdateIntervalChange = viewModel::onUpdateIntervalChange,
                    onWakeDurationChange = viewModel::onWakeDurationChange,
                    onAgreementAccepted = viewModel::acceptAgreement,
                    onPermissionGuideCompleted = viewModel::completePermissionGuide,
                    onCheckForUpdates = viewModel::checkForUpdatesManually,
                    onDismissUpdatePrompt = viewModel::dismissUpdatePrompt,
                    onDownloadUpdate = { downloadUrl ->
                        context.openSettings(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                    }
                )
            }
        }
    }
}

private fun Context.openSettings(intent: Intent) {
    runCatching { startActivity(intent) }
}

private fun Context.openAppSettings() {
    openSettings(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    )
}

private fun Context.hasAnyPermission(permissions: Array<String>): Boolean =
    permissions.any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun MainActivity.shouldShowAnyPermissionRationale(permissions: Array<String>): Boolean =
    permissions.any { permission -> shouldShowRequestPermissionRationale(permission) }

private fun MainActivity.shouldOpenLocationPermissionSettings(hasRequestedLocationPermission: Boolean): Boolean =
    hasRequestedLocationPermission &&
        !hasAnyPermission(locationPermissions()) &&
        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

private fun MainActivity.shouldOpenLocationPermissionSettingsAfterRequest(): Boolean =
    !hasAnyPermission(locationPermissions()) &&
        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

private fun MainActivity.shouldOpenNotificationPermissionSettings(
    hasRequestedNotificationPermission: Boolean,
): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        hasRequestedNotificationPermission &&
        !hasPermission(Manifest.permission.POST_NOTIFICATIONS) &&
        !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

private fun MainActivity.shouldOpenNotificationPermissionSettingsAfterRequest(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !hasPermission(Manifest.permission.POST_NOTIFICATIONS) &&
        !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        locationPermissions() + Manifest.permission.POST_NOTIFICATIONS
    } else {
        locationPermissions()
    }
}

private fun locationPermissions(): Array<String> =
    arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
