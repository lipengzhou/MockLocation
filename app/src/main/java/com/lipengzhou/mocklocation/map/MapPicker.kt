package com.lipengzhou.mocklocation.map

import android.os.Bundle
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions

@Composable
fun AMapPicker(
    modifier: Modifier = Modifier,
    initialLatitude: Double,
    initialLongitude: Double,
    selectedCoordinate: Coordinate = Coordinate(
        latitude = initialLatitude,
        longitude = initialLongitude
    ),
    zoomControlsBottomPadding: Dp = 24.dp,
    onTouchStateChange: (Boolean) -> Unit = {},
    onLocateCurrentPosition: () -> Unit = {},
    onPointSelected: (gcj02: Coordinate, wgs84: Coordinate) -> Unit,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply {
            onCreate(Bundle())
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        onTouchStateChange(true)
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_POINTER_UP -> {
                        onTouchStateChange(false)
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.map.uiSettings.setZoomControlsEnabled(false)
                val initialPoint = LatLng(initialLatitude, initialLongitude)
                mapView.map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPoint, DEFAULT_ZOOM))
                mapView.map.addMarker(MarkerOptions().position(initialPoint).title("当前选择"))
                mapView.map.setOnMapClickListener { latLng ->
                    val gcj02 = Coordinate(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude
                    )
                    val wgs84 = CoordinateConverter.gcj02ToWgs84(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude
                    )
                    mapView.map.clear()
                    mapView.map.addMarker(MarkerOptions().position(latLng).title("当前选择"))
                    mapView.map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    onPointSelected(gcj02, wgs84)
                }
                mapView
            }
        )

        MapZoomControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = zoomControlsBottomPadding),
            onZoomIn = {
                mapView.map.animateCamera(CameraUpdateFactory.zoomIn())
            },
            onZoomOut = {
                mapView.map.animateCamera(CameraUpdateFactory.zoomOut())
            },
            onLocateCurrentPosition = onLocateCurrentPosition
        )
    }

    LaunchedEffect(selectedCoordinate) {
        val selectedPoint = LatLng(selectedCoordinate.latitude, selectedCoordinate.longitude)
        mapView.map.clear()
        mapView.map.addMarker(MarkerOptions().position(selectedPoint).title("当前选择"))
        mapView.map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPoint, DEFAULT_ZOOM))
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}

private const val DEFAULT_ZOOM = 16f

@Composable
private fun MapZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocateCurrentPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column {
            MapControlButton(
                contentDescription = "放大地图",
                onClick = onZoomIn
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(MapControlIconSize)
                )
            }
            MapControlButton(
                contentDescription = "缩小地图",
                onClick = onZoomOut
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = null,
                    modifier = Modifier.size(MapControlIconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        MapControlButton(
            contentDescription = "定位到当前位置",
            onClick = onLocateCurrentPosition
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(MapControlIconSize)
            )
        }
    }
}

@Composable
private fun MapControlButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(MapControlButtonSize)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.96f),
        contentColor = Color(0xFF30343B),
        border = BorderStroke(1.dp, Color(0xFFD7DCE3)),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

private val MapControlButtonSize = 40.dp
private val MapControlIconSize = 22.dp
