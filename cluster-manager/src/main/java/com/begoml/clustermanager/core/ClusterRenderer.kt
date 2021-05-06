package com.begoml.clustermanager.core

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.content.Context
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.begoml.clustermanager.BitmapDescriptorGenerator
import com.begoml.clustermanager.Cluster
import com.begoml.clustermanager.ClusterItem
import com.begoml.clustermanager.HuaweiClusterManager
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptor
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions

internal class ClusterRenderer<T : ClusterItem>(
    context: Context,
    private val huaweiMap: HuaweiMap
) : HuaweiMap.OnMarkerClickListener {

    private val clusters: MutableList<Cluster<T>> = mutableListOf()
    private val markers: MutableMap<Cluster<T>, MarkerState> = HashMap()
    private var iconGenerator: BitmapDescriptorGenerator<T> = BitmapDescriptorImpl(context)
    private var callbacks: HuaweiClusterManager.Callbacks<T>? = null

    init {
        huaweiMap.setOnMarkerClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (marker.tag is Cluster<*>) {
            val cluster = marker.tag as Cluster<T>
            val clusterItems = cluster.items

            callbacks?.let { callbacks ->
                return if (clusterItems.size > 1) {
                    callbacks.onClusterClick(cluster)
                } else {
                    callbacks.onClusterItemClick(clusterItems[0])
                }
            }
        }
        return false
    }

    fun setCallbacks(listener: HuaweiClusterManager.Callbacks<T>?) {
        callbacks = listener
    }

    fun setIconGenerator(iconGenerator: BitmapDescriptorGenerator<T>) {
        this.iconGenerator = iconGenerator
    }

    fun render(clusters: List<Cluster<T>>) {
        val clustersToAdd: MutableList<Cluster<T>> = mutableListOf()
        val clustersToRemove: MutableList<Cluster<T>> = mutableListOf()
        for (cluster in clusters) {
            if (!markers.containsKey(cluster)) {
                clustersToAdd.add(cluster)
            }
        }
        for (cluster in markers.keys) {
            if (!clusters.contains(cluster)) {
                clustersToRemove.add(cluster)
            }
        }
        this.clusters.addAll(clustersToAdd)
        this.clusters.removeAll(clustersToRemove)

        // Remove the old clusters.
        for (clusterToRemove in clustersToRemove) {
            val markerToRemove = markers[clusterToRemove]!!.marker
            markerToRemove.zIndex = BACKGROUND_MARKER_Z_INDEX.toFloat()
            val parentCluster = findParentCluster(
                this.clusters, clusterToRemove.latitude,
                clusterToRemove.longitude
            )
            if (parentCluster != null) {
                animateMarkerToLocation(
                    markerToRemove, LatLng(
                        parentCluster.latitude,
                        parentCluster.longitude
                    ), true
                )
            } else {
                markerToRemove.remove()
            }
            markers.remove(clusterToRemove)
        }

        // Add the new clusters.
        for (clusterToAdd in clustersToAdd) {
            var markerToAdd: Marker
            val markerIcon = getMarkerIcon(clusterToAdd)
            val markerTitle = getMarkerTitle(clusterToAdd)
            val markerSnippet = getMarkerSnippet(clusterToAdd)
            val parentCluster: Cluster<*>? = findParentCluster(
                clustersToRemove, clusterToAdd.latitude,
                clusterToAdd.longitude
            )
            if (parentCluster != null) {
                markerToAdd = huaweiMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(parentCluster.latitude, parentCluster.longitude))
                        .icon(markerIcon)
                        .title(markerTitle)
                        .snippet(markerSnippet)
                        .zIndex(FOREGROUND_MARKER_Z_INDEX.toFloat())
                )
                animateMarkerToLocation(
                    markerToAdd,
                    LatLng(clusterToAdd.latitude, clusterToAdd.longitude), false
                )
            } else {
                markerToAdd = huaweiMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(clusterToAdd.latitude, clusterToAdd.longitude))
                        .icon(markerIcon)
                        .title(markerTitle)
                        .snippet(markerSnippet)
                        .alpha(0.0f)
                        .zIndex(FOREGROUND_MARKER_Z_INDEX.toFloat())
                )
                animateMarkerAppearance(markerToAdd)
            }
            markerToAdd.tag = clusterToAdd
            markers[clusterToAdd] = MarkerState(markerToAdd)
        }
    }

    private fun getMarkerIcon(cluster: Cluster<T>): BitmapDescriptor {
        val clusterIcon: BitmapDescriptor
        val clusterItems = cluster.items
        clusterIcon = if (clusterItems.size > 1) {
            iconGenerator.createClusterBitmapDescriptor(cluster)
        } else {
            iconGenerator.createMarkerBitmapDescriptor(clusterItems[0])
        }
        return clusterIcon
    }

    private fun getMarkerTitle(cluster: Cluster<T>): String? {
        val clusterItems = cluster.items
        return if (clusterItems.size > 1) {
            null
        } else {
            clusterItems[0].title
        }
    }

    private fun getMarkerSnippet(cluster: Cluster<T>): String? {
        val clusterItems = cluster.items
        return if (clusterItems.size > 1) {
            null
        } else {
            clusterItems[0].snippet
        }
    }

    private fun findParentCluster(
        clusters: List<Cluster<T>>,
        latitude: Double, longitude: Double
    ): Cluster<T>? {
        for (cluster in clusters) {
            if (cluster.contains(latitude, longitude)) {
                return cluster
            }
        }
        return null
    }

    private fun animateMarkerToLocation(
        marker: Marker, targetLocation: LatLng,
        removeAfter: Boolean
    ) {
        ObjectAnimator.ofObject(
            marker, "position",
            LatLngTypeEvaluator(), targetLocation
        ).apply {
            interpolator = FastOutSlowInInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (removeAfter) {
                        marker.remove()
                    }
                }
            })
        }.start()
    }

    private fun animateMarkerAppearance(marker: Marker) {
        ObjectAnimator.ofFloat(marker, "alpha", 1.0f).start()
    }

    private class LatLngTypeEvaluator : TypeEvaluator<LatLng> {
        override fun evaluate(fraction: Float, startValue: LatLng, endValue: LatLng): LatLng {
            val latitude = (endValue.latitude - startValue.latitude) * fraction + startValue.latitude
            val longitude = (endValue.longitude - startValue.longitude) * fraction + startValue.longitude
            return LatLng(latitude, longitude)
        }
    }

    companion object {
        private const val BACKGROUND_MARKER_Z_INDEX = 0
        private const val FOREGROUND_MARKER_Z_INDEX = 1
    }
}
