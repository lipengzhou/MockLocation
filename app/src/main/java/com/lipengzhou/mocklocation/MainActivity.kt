package com.lipengzhou.mocklocation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
                val viewModel: MockLocationViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.onPermissionsResult()
                }

                LaunchedEffect(viewModel) {
                    viewModel.refreshRuntimeState()
                }

                LaunchedEffect(viewModel, permissionLauncher) {
                    viewModel.startMockAction.collect { action ->
                        if (action == StartMockAction.RequestPermissions) {
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
                    onLocateCurrentPosition = viewModel::locateCurrentPosition,
                    onStart = viewModel::startMocking,
                    onStop = viewModel::stopMocking,
                    onRequestPermissions = {
                        permissionLauncher.launch(requiredPermissions())
                    },
                    onOpenDeveloperSettings = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    },
                    onOpenApplicationSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    },
                    onCopyDiagnostics = viewModel::copyDiagnostics,
                    onLatitudeChange = viewModel::onLatitudeChange,
                    onLongitudeChange = viewModel::onLongitudeChange,
                    onAltitudeChange = viewModel::onAltitudeChange,
                    onUpdateIntervalChange = viewModel::onUpdateIntervalChange,
                    onWakeDurationChange = viewModel::onWakeDurationChange
                )
            }
        }
    }
}

private fun requiredPermissions(): Array<String> {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
