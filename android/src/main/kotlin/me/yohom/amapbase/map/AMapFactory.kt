package me.yohom.amapbase.map

import android.content.Context
import android.view.View
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.route.*
import com.google.gson.Gson
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import me.yohom.amapbase.AMapBasePlugin
import me.yohom.amapbase.map.model.*
import me.yohom.amapbase.map.overlay.DrivingRouteOverlay
import me.yohom.amapbase.utils.*
import java.util.*


const val mapChannelName = "me.yohom/map"
const val success = "调用成功"

class AMapFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, id: Int, params: Any?): PlatformView {
        val options = Gson()
                .fromJson(params as String, UnifiedAMapOptions::class.java)

        val view = AMapView(context, id, options.toAMapOption())
        view.setup()
        return view
    }
}

class AMapView(private val context: Context,
               private val id: Int,
               amapOptions: AMapOptions) : PlatformView {

    private val mapView = TextureMapView(context, amapOptions)

    override fun getView(): View = mapView

    override fun dispose() {
        mapView.onPause()
        mapView.onDestroy()
    }

    fun setup() {
        mapView.onCreate(null)
        val mapChannel = MethodChannel(AMapBasePlugin.registrar.messenger(), "$mapChannelName$id")
        mapChannel.setMethodCallHandler { call, result ->
            handleMethodCall(call, result)
        }
    }

    private fun handleMethodCall(methodCall: MethodCall, methodResult: MethodChannel.Result) {
        val map = mapView.map
        when (methodCall.method) {
            "map#setMyLocationStyle" -> {
                val styleJson = methodCall.argument<String>("myLocationStyle") ?: "{}"

                log("方法setMyLocationEnabled android端参数: styleJson -> $styleJson")

                styleJson.parseJson<UnifiedMyLocationStyle>().applyTo(map)

                methodResult.success(success)
            }
            "map#setUiSettings" -> {
                val uiSettingsJson = methodCall.argument<String>("uiSettings") ?: "{}"

                log("方法setUiSettings android端参数: uiSettingsJson -> $uiSettingsJson")

                uiSettingsJson.parseJson<UnifiedUiSettings>().applyTo(map)

                methodResult.success(success)
            }
            "map#calculateDriveRoute" -> {
                // 规划参数
                val param = methodCall.argument<String>("routePlanParam")!!.parseJson<RoutePlanParam>()
                val showRouteImmediately = methodCall.argument<Boolean>("showRouteImmediately")
                        ?: true

                log("方法setUiSettings android端参数: routePlanParam -> $param")

                val routeQuery = RouteSearch.DriveRouteQuery(
                        RouteSearch.FromAndTo(param.from.toLatLonPoint(), param.to.toLatLonPoint()),
                        param.mode,
                        param.passedByPoints?.map { it.toLatLonPoint() },
                        param.avoidPolygons?.map { list -> list.map { it.toLatLonPoint() } },
                        param.avoidRoad
                )
                RouteSearch(context).run {
                    setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
                        override fun onDriveRouteSearched(r: DriveRouteResult?, errorCode: Int) {
                            if (errorCode != AMapException.CODE_AMAP_SUCCESS || r == null) {
                                methodResult.error("路线规划失败, 错误码: $errorCode", null, null)
                            } else if (r.paths.isEmpty()) {
                                methodResult.error("没有规划出合适的路线", null, null)
                            } else if (showRouteImmediately) {
                                map.clear()
                                DrivingRouteOverlay(map, r.startPos, r.targetPos, listOf(), r.paths[0])
                                        .apply {
                                            nodeIconVisible = false//设置节点marker是否显示
                                            removeFromMap()
                                            addToMap()
                                            zoomToSpan()
                                        }

                                methodResult.success(success)
                            }
                        }

                        override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) {}

                        override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) {}

                        override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {}
                    })

                    calculateDriveRouteAsyn(routeQuery)
                }
            }
            "marker#addMarker" -> {
                val optionsJson = methodCall.argument<String>("markerOptions") ?: "{}"

                log("方法marker#addMarker android端参数: optionsJson -> $optionsJson")

                optionsJson.parseJson<UnifiedMarkerOptions>().applyTo(map)

                methodResult.success(success)
            }
            "marker#addMarkers" -> {
                val moveToCenter = methodCall.argument<Boolean>("moveToCenter") ?: true
                val optionsListJson = methodCall.argument<String>("markerOptionsList") ?: "[]"
                val clear = methodCall.argument<Boolean>("clear") ?: false

                log("方法marker#addMarkers android端参数: optionsListJson -> $optionsListJson")

                val optionsList = ArrayList(optionsListJson.parseJson<List<UnifiedMarkerOptions>>().map { it.toMarkerOption() })
                if (clear) map.mapScreenMarkers.forEach { it.remove() }
                map.addMarkers(optionsList, moveToCenter)

                methodResult.success(success)
            }
            "marker#clear" -> {
                map.mapScreenMarkers.forEach { it.remove() }

                methodResult.success(success)
            }
            "map#showIndoorMap" -> {
                val enabled = methodCall.argument<Boolean>("showIndoorMap") ?: false

                log("方法map#showIndoorMap android端参数: enabled -> $enabled")

                map.showIndoorMap(enabled)

                methodResult.success(success)
            }
            "map#setMapType" -> {
                val mapType = methodCall.argument<Int>("mapType") ?: 1

                log("方法map#setMapType android端参数: mapType -> $mapType")

                map.mapType = mapType

                methodResult.success(success)
            }
            "map#setLanguage" -> {
                val language = methodCall.argument<String>("language") ?: "0"

                log("方法map#setLanguage android端参数: language -> $language")

                map.setMapLanguage(language)

                methodResult.success(success)
            }
            "map#clear" -> {
                map.clear()

                methodResult.success(success)
            }
            "map#searchPoi" -> {
                val query = methodCall.argument<String>("query") ?: "{}"

                log("方法map#searchPoi android端参数: query -> $query")

                query.parseJson<UnifiedPoiSearchQuery>()
                        .toPoiSearch(context)
                        .apply {
                            setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                                override fun onPoiItemSearched(result: PoiItem?, rCode: Int) {}

                                override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                                    if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                                        if (result != null) {
                                            methodResult.success(UnifiedPoiResult(result).toJson())
                                        } else {
                                            methodResult.error(rCode.toAMapError(), null, null)
                                        }
                                    } else {
                                        methodResult.error(rCode.toAMapError(), null, null)
                                    }
                                }
                            })
                        }.searchPOIAsyn()
            }
            "map#searchPoiBound" -> {
                val query = methodCall.argument<String>("query") ?: "{}"

                log("方法map#searchPoi android端参数: query -> $query")

                query.parseJson<UnifiedPoiSearchQuery>()
                        .toPoiSearchBound(context)
                        .apply {
                            setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                                override fun onPoiItemSearched(result: PoiItem?, rCode: Int) {}

                                override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                                    if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                                        if (result != null) {
                                            methodResult.success(UnifiedPoiResult(result).toJson())
                                        } else {
                                            methodResult.error(rCode.toAMapError(), null, null)
                                        }
                                    } else {
                                        methodResult.error(rCode.toAMapError(), null, null)
                                    }
                                }
                            })
                        }.searchPOIAsyn()
            }
            "map#searchPoiPolygon" -> {
                val query = methodCall.argument<String>("query") ?: "{}"

                log("方法map#searchPoi android端参数: query -> $query")

                query.parseJson<UnifiedPoiSearchQuery>()
                        .toPoiSearchPolygon(context)
                        .apply {
                            setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                                override fun onPoiItemSearched(result: PoiItem?, rCode: Int) {}

                                override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                                    if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                                        if (result != null) {
                                            methodResult.success(UnifiedPoiResult(result).toJson())
                                        } else {
                                            methodResult.error(rCode.toAMapError(), null, null)
                                        }
                                    } else {
                                        methodResult.error(rCode.toAMapError(), null, null)
                                    }
                                }
                            })
                        }.searchPOIAsyn()
            }
            "map#searchPoiId" -> {
                val id = methodCall.argument<String>("id") ?: ""

                log("方法map#searchPoiId android端参数: id -> $id")

                PoiSearch(context, null).apply {
                    setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                        override fun onPoiItemSearched(result: PoiItem?, rCode: Int) {
                            if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                                if (result != null) {
                                    methodResult.success(UnifiedPoiItem(result).toJson())
                                } else {
                                    methodResult.error(rCode.toAMapError(), null, null)
                                }
                            } else {
                                methodResult.error(rCode.toAMapError(), null, null)
                            }
                        }

                        override fun onPoiSearched(result: PoiResult?, rCode: Int) {}
                    })
                }.searchPOIIdAsyn(id)
            }
            "map#searchRoutePoiLine" -> {
                val query = methodCall.argument<String>("query") ?: "{}"

                log("方法map#searchRoutePoiLine android端参数: query -> $query")

                query.parseJson<UnifiedRoutePoiSearchQuery>()
                        .toRoutePoiSearchLine(context)
                        .apply {
                            setPoiSearchListener { result, rCode ->
                                if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                                    if (result != null) {
                                        methodResult.success(UnifiedRoutePOISearchResult(result).toJson())
                                    } else {
                                        methodResult.error(rCode.toAMapError(), null, null)
                                    }
                                } else {
                                    methodResult.error(rCode.toAMapError(), null, null)
                                }
                            }
                        }.searchRoutePOIAsyn()
            }
            "map#searchRoutePoiPolygon" -> {
                val query = methodCall.argument<String>("query") ?: "{}"

                log("方法map#searchRoutePoiPolygon android端参数: query -> $query")

                query.parseJson<UnifiedRoutePoiSearchQuery>()
                        .toRoutePoiSearchPolygon(context)
                        .apply {
                            setPoiSearchListener { result, rCode ->
                                if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                                    if (result != null) {
                                        methodResult.success(UnifiedRoutePOISearchResult(result).toJson())
                                    } else {
                                        methodResult.error(rCode.toAMapError(), null, null)
                                    }
                                } else {
                                    methodResult.error(rCode.toAMapError(), null, null)
                                }
                            }
                        }.searchRoutePOIAsyn()
            }
            "map#setZoomLevel" -> {
                val zoomLevel = methodCall.argument<Int>("zoomLevel") ?: 15

                map.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel.toFloat()))

                methodResult.success(success)
            }
            else -> methodResult.notImplemented()
        }
    }
}