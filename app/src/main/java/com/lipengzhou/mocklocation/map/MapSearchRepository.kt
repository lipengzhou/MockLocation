package com.lipengzhou.mocklocation.map

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.core.ServiceSettings
import com.amap.api.services.geocoder.GeocodeAddress
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeAddress
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Tip
import com.amap.api.services.poisearch.PoiSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MapSearchResult(
    val title: String,
    val address: String,
    val city: String,
    val coordinate: Coordinate,
)

data class MapSearchResponse(
    val keyword: String,
    val results: List<MapSearchResult>,
    val message: String,
)

class MapSearchRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun search(keyword: String): MapSearchResponse = withContext(Dispatchers.IO) {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return@withContext MapSearchResponse(
                keyword = normalizedKeyword,
                results = emptyList(),
                message = ""
            )
        }

        runCatching {
            ServiceSettings.updatePrivacyShow(appContext, true, true)
            ServiceSettings.updatePrivacyAgree(appContext, true)

            val searchAttempts = listOf(
                runCatching { appContext.searchInputTips(normalizedKeyword) },
                runCatching { appContext.searchGeocode(normalizedKeyword) },
                runCatching { appContext.searchPoiFallback(normalizedKeyword) }
            )
            val results = searchAttempts
                .flatMap { it.getOrDefault(emptyList()) }
                .distinctBy { result ->
                    "${formatCoordinate(result.coordinate.longitude)},${formatCoordinate(result.coordinate.latitude)}"
                }
                .take(MAX_POI_SEARCH_RESULTS)
            val firstError = searchAttempts.firstOrNull { it.isFailure }?.exceptionOrNull()

            MapSearchResponse(
                keyword = normalizedKeyword,
                results = results,
                message = if (results.isEmpty()) {
                    if (firstError is AMapException) {
                        searchErrorMessage(firstError.errorCode)
                    } else {
                        "未找到相关地点。"
                    }
                } else {
                    "找到 ${results.size} 个地点。"
                }
            )
        }.getOrElse { throwable ->
            MapSearchResponse(
                keyword = normalizedKeyword,
                results = emptyList(),
                message = if (throwable is AMapException) {
                    searchErrorMessage(throwable.errorCode)
                } else {
                    throwable.localizedMessage?.takeIf { it.isNotBlank() } ?: "搜索失败。"
                }
            )
        }
    }

    suspend fun reverseGeocode(coordinate: Coordinate): String? = withContext(Dispatchers.IO) {
        runCatching {
            ServiceSettings.updatePrivacyShow(appContext, true, true)
            ServiceSettings.updatePrivacyAgree(appContext, true)

            val query = RegeocodeQuery(
                LatLonPoint(coordinate.latitude, coordinate.longitude),
                REVERSE_GEOCODE_RADIUS_METERS,
                GeocodeSearch.AMAP
            ).apply {
                extensions = GeocodeSearch.EXTENSIONS_ALL
            }
            GeocodeSearch(appContext)
                .getFromLocation(query)
                .toDisplayText()
        }.getOrNull()
    }

    private fun Context.searchInputTips(keyword: String): List<MapSearchResult> {
        val query = InputtipsQuery(keyword, "").apply {
            setCityLimit(false)
        }
        return Inputtips(this, query).requestInputtips()
            .mapNotNull { it.toMapSearchResult() }
    }

    private fun Context.searchGeocode(keyword: String): List<MapSearchResult> {
        return GeocodeSearch(this)
            .getFromLocationName(GeocodeQuery(keyword, ""))
            .mapNotNull { it.toMapSearchResult(keyword) }
    }

    private fun Context.searchPoiFallback(keyword: String): List<MapSearchResult> {
        val query = PoiSearch.Query(keyword, "", "").apply {
            pageSize = MAX_POI_SEARCH_RESULTS
            pageNum = 0
            setCityLimit(false)
            setExtensions(PoiSearch.EXTENSIONS_BASE)
        }
        return PoiSearch(this, query)
            .apply { setLanguage(PoiSearch.CHINESE) }
            .searchPOI()
            .pois
            .orEmpty()
            .mapNotNull { it.toMapSearchResult() }
    }

    private fun Tip.toMapSearchResult(): MapSearchResult? {
        val point = point ?: return null
        val name = name.ifNotBlank() ?: "未命名地点"
        val district = district.orEmpty()
        val address = address.orEmpty()
        return MapSearchResult(
            title = name,
            address = listOfNotNull(
                district.ifNotBlank(),
                address.takeIf { it.isNotBlank() && it != district }
            ).joinToString("").ifBlank {
                "经度 ${formatCoordinate(point.longitude)}，纬度 ${formatCoordinate(point.latitude)}"
            },
            city = district,
            coordinate = point.toCoordinate()
        )
    }

    private fun GeocodeAddress.toMapSearchResult(keyword: String): MapSearchResult? {
        val point = latLonPoint ?: return null
        val province = province.orEmpty()
        val city = city.orEmpty()
        val district = district.orEmpty()
        val township = township.orEmpty()
        val neighborhood = neighborhood.orEmpty()
        val building = building.orEmpty()
        val displayAddress = formatAddress.orEmpty()
            .ifBlank {
                listOfNotNull(
                    province.ifNotBlank(),
                    city.takeIf { it.isNotBlank() && it != province },
                    district.ifNotBlank(),
                    township.ifNotBlank(),
                    neighborhood.ifNotBlank(),
                    building.ifNotBlank()
                ).joinToString("")
            }
        return MapSearchResult(
            title = building.ifBlank { neighborhood.ifBlank { keyword } },
            address = displayAddress.ifBlank {
                "经度 ${formatCoordinate(point.longitude)}，纬度 ${formatCoordinate(point.latitude)}"
            },
            city = city,
            coordinate = point.toCoordinate()
        )
    }

    private fun PoiItem.toMapSearchResult(): MapSearchResult? {
        val point = latLonPoint ?: return null
        val province = provinceName.orEmpty()
        val city = cityName.orEmpty()
        val district = adName.orEmpty()
        val address = snippet.orEmpty()
        val displayAddress = listOfNotNull(
            province.ifNotBlank(),
            city.takeIf { it.isNotBlank() && it != province },
            district.ifNotBlank(),
            address.ifNotBlank()
        ).joinToString("")
        return MapSearchResult(
            title = title.ifNotBlank() ?: "未命名地点",
            address = displayAddress.ifBlank {
                "经度 ${formatCoordinate(point.longitude)}，纬度 ${formatCoordinate(point.latitude)}"
            },
            city = city,
            coordinate = point.toCoordinate()
        )
    }

    private fun RegeocodeAddress.toDisplayText(): String? {
        val address = formatAddress.orEmpty()
        val nearbyPoi = pois.orEmpty()
            .firstOrNull()
            ?.title
            .ifNotBlank()
            ?.takeIf { poiName -> address.isBlank() || !address.contains(poiName) }
        return listOfNotNull(
            nearbyPoi,
            address.ifNotBlank()
        ).joinToString(" · ").ifBlank { null }
    }

    private fun LatLonPoint.toCoordinate(): Coordinate =
        Coordinate(
            latitude = latitude,
            longitude = longitude
        )

    private fun String?.ifNotBlank(): String? =
        this?.takeIf { it.isNotBlank() }

    private fun searchErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            AMapException.CODE_AMAP_INVALID_USER_KEY,
            AMapException.CODE_AMAP_USERKEY_PLAT_NOMATCH,
            AMapException.CODE_AMAP_SIGNATURE_ERROR -> "高德 Key 校验失败，请检查 Key、包名和 SHA1。"
            AMapException.CODE_AMAP_DAILY_QUERY_OVER_LIMIT -> "搜索配额已用完，请稍后再试。"
            AMapException.CODE_AMAP_ACCESS_TOO_FREQUENT -> "搜索过于频繁，请稍后再试。"
            AMapException.CODE_AMAP_CLIENT_NETWORK_EXCEPTION,
            AMapException.CODE_AMAP_CLIENT_UNKNOWHOST_EXCEPTION,
            AMapException.CODE_AMAP_ENGINE_CONNECT_TIMEOUT,
            AMapException.CODE_AMAP_ENGINE_RETURN_TIMEOUT -> "网络不可用或请求超时。"
            else -> "搜索失败，错误码 $errorCode。"
        }
    }

    companion object {
        const val MAX_POI_SEARCH_RESULTS = 20
        private const val REVERSE_GEOCODE_RADIUS_METERS = 200f
    }
}
