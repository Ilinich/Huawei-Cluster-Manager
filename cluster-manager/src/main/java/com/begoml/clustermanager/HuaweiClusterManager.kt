package com.begoml.clustermanager

import android.content.Context
import com.begoml.clustermanager.core.ClusterRenderer
import com.begoml.clustermanager.core.QuadTree
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.LatLngBounds
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow

class HuaweiClusterManager<T : ClusterItem>(
        context: Context,
        private val huaweiMap: HuaweiMap
) : HuaweiMap.OnCameraIdleListener {

    interface Callbacks<T : ClusterItem> {

        fun onClusterClick(cluster: Cluster<T>): Boolean

        fun onClusterItemClick(clusterItem: T): Boolean
    }

    companion object {
        private const val QUAD_TREE_BUCKET_CAPACITY = 4
        private const val DEFAULT_MIN_CLUSTER_SIZE = 1
    }

    private val coroutineScope: CoroutineScope =
            CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val quadTree = QuadTree<T>(QUAD_TREE_BUCKET_CAPACITY)
    private val renderer: ClusterRenderer<T> = ClusterRenderer(context, huaweiMap)
    private val quadTreeLock = ReentrantReadWriteLock()

    private var minClusterSize = DEFAULT_MIN_CLUSTER_SIZE

    private var quadTreeJob: Job? = null
    private var clusterJob: Job? = null

    fun setIconGenerator(iconGenerator: BitmapDescriptorGenerator<T>) {
        renderer.setIconGenerator(iconGenerator)
    }

    fun setCallbacks(callbacks: Callbacks<T>) {
        renderer.setCallbacks(callbacks)
    }

    fun addItems(clusterItems: List<T>) {
        buildQuadTree(clusterItems)
    }

    fun addItem(clusterItem: T) {
        quadTreeWriteLock {
            quadTree.insert(clusterItem)
        }
    }

    fun clearItems() {
        quadTreeWriteLock { quadTree.clear() }
    }

    fun setMinClusterSize(minClusterSize: Int) {
        check(minClusterSize > 0)
        this.minClusterSize = minClusterSize
    }

    override fun onCameraIdle() {
        cluster()
    }

    private fun buildQuadTree(clusterItems: List<T>) {
        if (quadTreeJob?.isActive == true) {
            quadTreeJob?.cancel()
        }
        quadTreeJob = coroutineScope.launch {
            withContext(Dispatchers.Default) {
                quadTreeWriteLock {
                    quadTree.clear()
                    for (clusterItem in clusterItems) {
                        quadTree.insert(clusterItem)
                    }
                }
            }
            cluster()
        }
    }

    private fun cluster() {
        if (clusterJob?.isActive == true) {
            clusterJob?.cancel()
        }

        clusterJob = coroutineScope.launch {
            val latLngBounds = huaweiMap.projection.visibleRegion.latLngBounds
            val zoomLevel = huaweiMap.cameraPosition.zoom
            val clusters = withContext(Dispatchers.Default) {
                getClusters(latLngBounds, zoomLevel)
            }
            renderer.render(clusters)
        }
    }

    private fun getClusters(latLngBounds: LatLngBounds, zoomLevel: Float): List<Cluster<T>> {
        val clusters: MutableList<Cluster<T>> = ArrayList()
        val tileCount = (2.0.pow(zoomLevel.toDouble()) * 2).toLong()
        val startLatitude: Double = latLngBounds.northeast.latitude
        val endLatitude: Double = latLngBounds.southwest.latitude
        val startLongitude: Double = latLngBounds.southwest.longitude
        val endLongitude: Double = latLngBounds.northeast.longitude
        val stepLatitude: Double = 180.0 / tileCount
        val stepLongitude: Double = 360.0 / tileCount
        if (startLongitude > endLongitude) { // Longitude +180°/-180° overlap.
            // [start longitude; 180]
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    startLongitude, 180.0, stepLatitude, stepLongitude)
            // [-180; end longitude]
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    -180.0, endLongitude, stepLatitude, stepLongitude)
        } else {
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    startLongitude, endLongitude, stepLatitude, stepLongitude)
        }
        return clusters
    }

    private fun getClustersInsideBounds(clusters: MutableList<Cluster<T>>,
                                        startLatitude: Double, endLatitude: Double,
                                        startLongitude: Double, endLongitude: Double,
                                        stepLatitude: Double, stepLongitude: Double) {
        val startX = ((startLongitude + 180.0) / stepLongitude).toLong()
        val startY = ((90.0 - startLatitude) / stepLatitude).toLong()
        val endX = ((endLongitude + 180.0) / stepLongitude).toLong() + 1
        val endY = ((90.0 - endLatitude) / stepLatitude).toLong() + 1
        quadTreeReadLock {
            for (tileX in startX..endX) {
                for (tileY in startY..endY) {
                    val north = 90.0 - tileY * stepLatitude
                    val west = tileX * stepLongitude - 180.0
                    val south = north - stepLatitude
                    val east = west + stepLongitude

                    val points = quadTree.queryRange(north, west, south, east)

                    if (points.isEmpty()) {
                        continue
                    }

                    if (points.size >= minClusterSize) {
                        var totalLatitude = 0.0
                        var totalLongitude = 0.0
                        for (point in points) {
                            totalLatitude += point.latitude
                            totalLongitude += point.longitude
                        }
                        val latitude = totalLatitude / points.size
                        val longitude = totalLongitude / points.size
                        clusters.add(Cluster(latitude, longitude, points, north, west, south, east))
                    } else {
                        for (point in points) {
                            clusters.add(Cluster(point.latitude, point.longitude, listOf(point), north, west, south, east))
                        }
                    }
                }
            }
        }
    }

    private fun quadTreeWriteLock(runnable: Runnable) {
        quadTreeLock.writeLock().lock()
        try {
            runnable.run()
        } finally {
            quadTreeLock.writeLock().unlock()
        }
    }

    private fun quadTreeReadLock(runnable: Runnable) {
        quadTreeLock.readLock().lock()
        try {
            runnable.run()
        } finally {
            quadTreeLock.readLock().unlock()
        }
    }

    fun clearSate() {
        coroutineScope.cancel()
    }
}

internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {

    override val coroutineContext: CoroutineContext = context

    override fun close() {
        coroutineContext.cancel()
    }
}
