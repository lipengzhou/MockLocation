package com.lipengzhou.mocklocation.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdate
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.AMapGestureListener
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AMapPicker(
    modifier: Modifier = Modifier,
    initialLatitude: Double,
    initialLongitude: Double,
    selectedCoordinate: Coordinate = Coordinate(
        latitude = initialLatitude,
        longitude = initialLongitude
    ),
    cameraMoveRequestId: Long = 0L,
    zoomControlsBottomPadding: Dp = 24.dp,
    onTouchStateChange: (Boolean) -> Unit = {},
    onCoordinateInputClick: () -> Unit = {},
    onLocateCurrentPosition: () -> Unit = {},
    onMapCenterChanged: (gcj02: Coordinate, wgs84: Coordinate) -> Unit = { _, _ -> },
    onPointSelected: (gcj02: Coordinate, wgs84: Coordinate) -> Unit,
) {
    val context = LocalContext.current
    val selectedMarkerColor = MaterialTheme.colorScheme.primary.toArgb()
    val selectedMarkerCutoutColor = MaterialTheme.colorScheme.surface.toArgb()
    val selectedMarkerIcon = remember(context, selectedMarkerColor, selectedMarkerCutoutColor) {
        createSelectedLocationMarkerIcon(
            context = context,
            markerColor = selectedMarkerColor,
            cutoutColor = selectedMarkerCutoutColor
        )
    }
    val currentOnPointSelected by rememberUpdatedState(onPointSelected)
    val currentOnMapCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val currentOnTouchStateChange by rememberUpdatedState(onTouchStateChange)
    val currentSelectedCoordinate by rememberUpdatedState(selectedCoordinate)
    var isMapTouching by remember { mutableStateOf(false) }
    var isProgrammaticCameraMove by remember { mutableStateOf(false) }
    var pendingGestureSelection by remember { mutableStateOf(false) }
    var lastMapSelectedCoordinate by remember { mutableStateOf<Coordinate?>(selectedCoordinate) }
    var selectedMarker by remember { mutableStateOf<Marker?>(null) }
    val mapView = remember {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply {
            onCreate(Bundle())
            addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                selectedMarker?.let { marker ->
                    marker.setPositionAtMapCenter(view as MapView)
                }
            }
        }
    }

    fun coordinatesFromLatLng(latLng: LatLng): Pair<Coordinate, Coordinate> {
        val gcj02 = Coordinate(
            latitude = latLng.latitude,
            longitude = latLng.longitude
        )
        val wgs84 = CoordinateConverter.gcj02ToWgs84(
            latitude = latLng.latitude,
            longitude = latLng.longitude
        )
        return gcj02 to wgs84
    }

    fun publishMapCenterChanged(latLng: LatLng) {
        val (gcj02, wgs84) = coordinatesFromLatLng(latLng)
        if (gcj02.isCloseTo(currentSelectedCoordinate)) {
            return
        }

        lastMapSelectedCoordinate = gcj02
        currentOnMapCenterChanged(gcj02, wgs84)
    }

    fun publishMapPointSelected(latLng: LatLng) {
        val (gcj02, wgs84) = coordinatesFromLatLng(latLng)
        lastMapSelectedCoordinate = gcj02
        currentOnPointSelected(gcj02, wgs84)
    }

    fun animateCamera(cameraUpdate: CameraUpdate) {
        isProgrammaticCameraMove = true
        mapView.map.animateCamera(
            cameraUpdate,
            object : AMap.CancelableCallback {
                override fun onFinish() {
                    isProgrammaticCameraMove = false
                }

                override fun onCancel() {
                    isProgrammaticCameraMove = false
                }
            }
        )
    }

    fun animateCameraToSelectedPoint(latLng: LatLng, zoom: Float? = null) {
        val cameraUpdate = if (zoom == null) {
            CameraUpdateFactory.newLatLng(latLng)
        } else {
            CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        }
        animateCamera(cameraUpdate)
    }

    fun startMapGesture() {
        isProgrammaticCameraMove = false
        isMapTouching = true
        currentOnTouchStateChange(true)
        mapView.parent?.requestDisallowInterceptTouchEvent(true)
    }

    fun finishMapGesture() {
        isMapTouching = false
        currentOnTouchStateChange(false)
        mapView.parent?.requestDisallowInterceptTouchEvent(false)
    }

    fun finishPendingGestureSelection() {
        if (!pendingGestureSelection) {
            return
        }

        pendingGestureSelection = false
        publishMapPointSelected(mapView.map.cameraPosition.target)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.map.uiSettings.setZoomControlsEnabled(false)
                val initialPoint = LatLng(initialLatitude, initialLongitude)
                mapView.map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPoint, DEFAULT_ZOOM))
                selectedMarker = mapView.map.addMarker(
                    selectedLocationMarkerOptions(initialPoint, selectedMarkerIcon)
                )
                selectedMarker?.setPositionAtMapCenter(mapView)
                mapView.map.setOnMapClickListener { latLng ->
                    finishMapGesture()
                    pendingGestureSelection = false
                    publishMapPointSelected(latLng)
                    animateCameraToSelectedPoint(latLng)
                }
                mapView.map.setOnMapTouchListener { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            startMapGesture()
                        }

                        MotionEvent.ACTION_UP -> {
                            finishMapGesture()
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            pendingGestureSelection = false
                            finishMapGesture()
                        }
                    }
                }
                mapView.map.setAMapGestureListener(object : AMapGestureListener {
                    override fun onDoubleTap(x: Float, y: Float) = Unit

                    override fun onSingleTap(x: Float, y: Float) = Unit

                    override fun onFling(x: Float, y: Float) = Unit

                    override fun onScroll(x: Float, y: Float) {
                        startMapGesture()
                        pendingGestureSelection = true
                        publishMapCenterChanged(mapView.map.cameraPosition.target)
                    }

                    override fun onLongPress(x: Float, y: Float) = Unit

                    override fun onDown(x: Float, y: Float) {
                        startMapGesture()
                    }

                    override fun onUp(x: Float, y: Float) {
                        finishMapGesture()
                    }

                    override fun onMapStable() {
                        finishMapGesture()
                        finishPendingGestureSelection()
                    }
                })
                mapView.map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                    override fun onCameraChange(cameraPosition: CameraPosition) {
                        if (isProgrammaticCameraMove) {
                            return
                        }

                        pendingGestureSelection = true
                        publishMapCenterChanged(cameraPosition.target)
                    }

                    override fun onCameraChangeFinish(cameraPosition: CameraPosition) {
                        if (isProgrammaticCameraMove) {
                            isProgrammaticCameraMove = false
                            return
                        }

                        finishPendingGestureSelection()
                    }
                })
                mapView
            },
            update = {
                selectedMarker?.setIcon(selectedMarkerIcon)
                selectedMarker?.setPositionAtMapCenter(it)
            }
        )

        MapZoomControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = zoomControlsBottomPadding),
            onZoomIn = {
                animateCamera(CameraUpdateFactory.zoomIn())
            },
            onZoomOut = {
                animateCamera(CameraUpdateFactory.zoomOut())
            },
            onCoordinateInputClick = onCoordinateInputClick,
            onLocateCurrentPosition = onLocateCurrentPosition
        )
    }

    LaunchedEffect(cameraMoveRequestId) {
        if (cameraMoveRequestId == 0L || lastMapSelectedCoordinate.isCloseTo(selectedCoordinate)) {
            return@LaunchedEffect
        }

        val selectedPoint = LatLng(selectedCoordinate.latitude, selectedCoordinate.longitude)
        animateCameraToSelectedPoint(selectedPoint, DEFAULT_ZOOM)
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
private const val SelectedMarkerAnchorY = 0.95f
private const val CoordinateComparisonTolerance = 0.00000001

private fun Coordinate?.isCloseTo(other: Coordinate): Boolean =
    this != null &&
        abs(latitude - other.latitude) <= CoordinateComparisonTolerance &&
        abs(longitude - other.longitude) <= CoordinateComparisonTolerance

private fun Marker.setPositionAtMapCenter(mapView: MapView) {
    if (mapView.width <= 0 || mapView.height <= 0) {
        return
    }
    setPositionByPixels(mapView.width / 2, mapView.height / 2)
}

private fun selectedLocationMarkerOptions(
    position: LatLng,
    icon: BitmapDescriptor
): MarkerOptions = MarkerOptions()
    .position(position)
    .title("当前选择")
    .icon(icon)
    .anchor(0.5f, SelectedMarkerAnchorY)
    .zIndex(10f)

private fun createSelectedLocationMarkerIcon(
    context: Context,
    markerColor: Int,
    cutoutColor: Int
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val width = (44f * density).roundToInt()
    val height = (58f * density).roundToInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = width / 2f
    val centerY = 20f * density
    val outerRadius = 16f * density
    val innerRadius = 9f * density
    val stemWidth = 5f * density
    val stemBottom = height - 5f * density
    val shadowOffset = 1.5f * density
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(60, 0, 0, 0)
        style = Paint.Style.FILL
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = markerColor
        style = Paint.Style.FILL
    }
    val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cutoutColor
        style = Paint.Style.FILL
    }

    val stemRect = RectF(
        centerX - stemWidth / 2f,
        centerY,
        centerX + stemWidth / 2f,
        stemBottom
    )
    val shadowStemRect = RectF(
        stemRect.left + shadowOffset,
        stemRect.top + shadowOffset,
        stemRect.right + shadowOffset,
        stemRect.bottom + shadowOffset
    )
    canvas.drawRoundRect(
        shadowStemRect,
        stemWidth / 2f,
        stemWidth / 2f,
        shadowPaint
    )
    canvas.drawCircle(centerX + shadowOffset, centerY + shadowOffset, outerRadius, shadowPaint)
    canvas.drawRoundRect(stemRect, stemWidth / 2f, stemWidth / 2f, fillPaint)
    canvas.drawCircle(centerX, centerY, outerRadius, fillPaint)
    canvas.drawCircle(centerX, centerY, innerRadius, cutoutPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
private fun MapZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCoordinateInputClick: () -> Unit,
    onLocateCurrentPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MapControlButton(
            contentDescription = "输入坐标",
            onClick = onCoordinateInputClick
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(MapControlIconSize)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
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
        Spacer(modifier = Modifier.height(10.dp))
        MapZoomControlGroup(
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut
        )
    }
}

@Composable
private fun MapZoomControlGroup(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(
            width = MapControlButtonSize,
            height = MapControlButtonSize + MapControlButtonSize
        ),
        shape = MapControlButtonShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MapControlButtonShape)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MapZoomControlButton(
                    contentDescription = "放大地图",
                    shape = MapZoomInButtonShape,
                    onClick = onZoomIn
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(MapControlIconSize)
                    )
                }
                MapZoomControlButton(
                    contentDescription = "缩小地图",
                    shape = MapZoomOutButtonShape,
                    onClick = onZoomOut
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(MapControlIconSize)
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun MapZoomControlButton(
    contentDescription: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(MapControlButtonSize)
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        content()
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
            .clip(MapControlButtonShape)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
            },
        shape = MapControlButtonShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

private val MapControlButtonSize = 44.dp
private val MapControlIconSize = 20.dp
private val MapControlButtonShape = RoundedCornerShape(12.dp)
private val MapZoomInButtonShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
private val MapZoomOutButtonShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
