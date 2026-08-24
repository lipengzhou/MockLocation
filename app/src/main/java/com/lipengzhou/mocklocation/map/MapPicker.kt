package com.lipengzhou.mocklocation.map

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onPointSelected: (gcj02: Coordinate, wgs84: Coordinate) -> Unit,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
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

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}

private const val DEFAULT_ZOOM = 16f
