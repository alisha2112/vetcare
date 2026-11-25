package com.alisha2112.vetcare.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import android.location.Location
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val MAP_STYLE_URL =
    "https://tiles.openfreemap.org/styles/liberty"

private const val ROUTE_SOURCE_ID = "vetcare-route-source"
private const val POINTS_SOURCE_ID = "vetcare-points-source"

private const val ROUTE_LAYER_ID = "vetcare-route-layer"
private const val POINTS_LAYER_ID = "vetcare-points-layer"
private const val LABELS_LAYER_ID = "vetcare-labels-layer"

private fun setupRouteLayers(style: Style) {
    val emptyCollection =
        FeatureCollection.fromFeatures(emptyList<Feature>())

    // Джерело для лінії маршруту.
    style.addSource(
        GeoJsonSource(
            ROUTE_SOURCE_ID,
            emptyCollection
        )
    )

    // Джерело для маркерів.
    style.addSource(
        GeoJsonSource(
            POINTS_SOURCE_ID,
            emptyCollection
        )
    )

    // Лінія між послідовними GPS-точками.
    style.addLayer(
        LineLayer(
            ROUTE_LAYER_ID,
            ROUTE_SOURCE_ID
        ).withProperties(
            lineColor("#00796B"),
            lineWidth(5f),
            lineOpacity(0.9f)
        )
    )

    // Кольорові маркери GPS-точок.
    style.addLayer(
        CircleLayer(
            POINTS_LAYER_ID,
            POINTS_SOURCE_ID
        ).withProperties(
            circleRadius(12f),
            circleColor(
                Expression.toColor(
                    Expression.get("color")
                )
            ),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(2f)
        )
    )

    // Порядкові номери маркерів.
    style.addLayer(
        SymbolLayer(
            LABELS_LAYER_ID,
            POINTS_SOURCE_ID
        ).withProperties(
            textField(Expression.get("label")),
            textFont(arrayOf("Noto Sans Regular")),
            textSize(12f),
            textColor("#FFFFFF"),
            textAllowOverlap(true),
            textIgnorePlacement(true)
        )
    )
}

private fun updateRouteLayers(
    style: Style,
    routePoints: List<Location>
) {
    val routeSource =
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

    val pointsSource =
        style.getSourceAs<GeoJsonSource>(POINTS_SOURCE_ID)

    // Перетворення GPS-координат у формат GeoJSON.
    val coordinates = routePoints.map { location ->
        Point.fromLngLat(
            location.longitude,
            location.latitude
        )
    }

    // Лінію можна побудувати від двох точок.
    val lineFeatures = if (coordinates.size >= 2) {
        listOf(
            Feature.fromGeometry(
                LineString.fromLngLats(coordinates)
            )
        )
    } else {
        emptyList<Feature>()
    }

    routeSource?.setGeoJson(
        FeatureCollection.fromFeatures(lineFeatures)
    )

    // Об'єднуємо маркери, якщо координати збігаються.
    // Наприклад, при поверненні до початкової точки.
    val groupedPoints = routePoints
        .withIndex()
        .groupBy {
            it.value.latitude to it.value.longitude
        }

    val pointFeatures = groupedPoints.values.map { group ->
        val location = group.first().value

        val isStart = group.any {
            it.index == 0
        }

        val isEnd = group.any {
            it.index == routePoints.lastIndex
        }

        val color = when {
            routePoints.size == 1 -> "#15803D"
            isStart && isEnd -> "#7C3AED"
            isStart -> "#15803D"
            isEnd -> "#DC2626"
            else -> "#00796B"
        }

        val label = group.joinToString("/") {
            (it.index + 1).toString()
        }

        Feature.fromGeometry(
            Point.fromLngLat(
                location.longitude,
                location.latitude
            )
        ).apply {
            addStringProperty("color", color)
            addStringProperty("label", label)
        }
    }

    pointsSource?.setGeoJson(
        FeatureCollection.fromFeatures(pointFeatures)
    )
}

@Composable
fun RouteMap(
    routePoints: List<Location>,
    modifier: Modifier = Modifier,
    onMarkerClick: ((List<Int>) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val currentOnMarkerClick by rememberUpdatedState(
        onMarkerClick
    )

    var mapSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val mapView = remember(context, lifecycle) {
        MapLibre.getInstance(context.applicationContext)

        MapView(
            context,
            MapLibreMapOptions
                .createFromAttributes(context)
                .textureMode(true)
        )
    }

    var map by remember(mapView) {
        mutableStateOf<MapLibreMap?>(null)
    }

    var isLoading by remember(mapView) {
        mutableStateOf(true)
    }

    var mapError by remember(mapView) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(mapView, lifecycle) {
        var disposed = false
        var started = false
        var resumed = false
        var clickMap: MapLibreMap? = null

        var clickListener: MapLibreMap.OnMapClickListener? = null

        mapView.onCreate(null)

        val errorListener =
            MapView.OnDidFailLoadingMapListener { error ->
                if (!disposed) {
                    isLoading = false
                    mapError =
                        "Не вдалося завантажити карту: $error"
                }
            }

        mapView.addOnDidFailLoadingMapListener(
            errorListener
        )

        mapView.getMapAsync { readyMap ->
            if (!disposed) {
                readyMap.setStyle(
                    Style.Builder().fromUri(MAP_STYLE_URL)
                ) { style ->
                    if (!disposed) {
                        setupRouteLayers(style)

                        val listener =
                            MapLibreMap.OnMapClickListener { point ->
                                val handler = currentOnMarkerClick

                                if (disposed || handler == null) {
                                    false
                                } else {
                                    val screenPoint =
                                        readyMap.projection
                                            .toScreenLocation(point)

                                    val features =
                                        readyMap.queryRenderedFeatures(
                                            screenPoint,
                                            POINTS_LAYER_ID
                                        )

                                    val indices = features
                                        .flatMap { feature ->
                                            feature
                                                .getStringProperty("label")
                                                .orEmpty()
                                                .split("/")
                                        }
                                        .mapNotNull { label ->
                                            label.toIntOrNull()
                                                ?.minus(1)
                                        }
                                        .filter { it >= 0 }
                                        .distinct()
                                        .sorted()

                                    if (indices.isNotEmpty()) {
                                        handler(indices)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }

                        readyMap.addOnMapClickListener(listener)

                        clickMap = readyMap
                        clickListener = listener

                        map = readyMap
                        isLoading = false
                        mapError = null
                    }
                }
            }
        }

        fun releaseMap() {
            if (disposed) return

            disposed = true

            if (resumed) {
                mapView.onPause()
                resumed = false
            }

            if (started) {
                mapView.onStop()
                started = false
            }

            mapView.removeOnDidFailLoadingMapListener(
                errorListener
            )

            clickListener?.let { listener ->
                clickMap?.removeOnMapClickListener(listener)
            }

            clickListener = null
            clickMap = null

            mapView.onDestroy()
        }

        val observer = LifecycleEventObserver { _, event ->
            if (!disposed) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (!started) {
                            mapView.onStart()
                            started = true
                        }
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        if (!resumed) {
                            mapView.onResume()
                            resumed = true
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (resumed) {
                            mapView.onPause()
                            resumed = false
                        }
                    }

                    Lifecycle.Event.ON_STOP -> {
                        if (started) {
                            mapView.onStop()
                            started = false
                        }
                    }

                    Lifecycle.Event.ON_DESTROY -> {
                        releaseMap()
                    }

                    else -> Unit
                }
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            releaseMap()
        }
    }

    LaunchedEffect(map, routePoints, mapSize) {
        val readyMap = map ?: return@LaunchedEffect
        val style = readyMap.style ?: return@LaunchedEffect

        // Оновлюємо маркери та лінію маршруту.
        updateRouteLayers(style, routePoints)

        if (routePoints.isEmpty()) {
            return@LaunchedEffect
        }

        if (mapSize.width <= 0 || mapSize.height <= 0) {
            return@LaunchedEffect
        }

        // Очікуємо кадр, щоб карта завершила компонування.
        withFrameNanos { }

        val coordinates = routePoints.map { point ->
            LatLng(
                point.latitude,
                point.longitude
            )
        }.distinct()

        if (coordinates.size == 1) {
            // Центрування на єдиній точці.
            readyMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    coordinates.first(),
                    16.0
                )
            )
        } else {
            // Межі, які охоплюють увесь маршрут.
            val boundsBuilder = LatLngBounds.Builder()

            coordinates.forEach { coordinate ->
                boundsBuilder.include(coordinate)
            }

            val bounds = boundsBuilder.build()

            val padding = with(density) {
                32.dp.roundToPx()
            }.coerceAtMost(
                minOf(mapSize.width, mapSize.height) / 4
            )

            readyMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    padding
                )
            )
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Карта маршруту",
            style = MaterialTheme.typography.titleMedium
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Завантаження карти...",
                style = MaterialTheme.typography.bodySmall
            )
        }

        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .onSizeChanged { size ->
                    mapSize = size
                }
        )

        Text(
            text = "Зелений — початок; бірюзовий — " +
                    "проміжна точка; червоний — кінець; " +
                    "фіолетовий — збіг початку та кінця.",
            style = MaterialTheme.typography.bodySmall
        )

        mapError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = "Картографічні дані: © OpenStreetMap contributors, " +
                "OpenFreeMap.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
