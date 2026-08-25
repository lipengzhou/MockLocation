package com.lipengzhou.mocklocation.map

import java.util.Locale

fun formatCoordinate(value: Double): String =
    String.format(Locale.US, "%.8f", value)
