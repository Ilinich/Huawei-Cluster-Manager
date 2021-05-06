package com.begoml.clustermanager.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.SparseArray
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.begoml.clustermanager.BitmapDescriptorGenerator
import com.begoml.clustermanager.Cluster
import com.begoml.clustermanager.ClusterItem
import com.begoml.clustermanager.R
import com.huawei.hms.maps.model.BitmapDescriptor
import com.huawei.hms.maps.model.BitmapDescriptorFactory

internal class BitmapDescriptorImpl<T : ClusterItem>(
    private val context: Context
) : BitmapDescriptorGenerator<T> {

    private val iconStyle: IconStyle = IconStyle(context)
    private var clusterItemIcon: BitmapDescriptor? = null
    private val clusterIcons = SparseArray<BitmapDescriptor>()


    override fun createClusterBitmapDescriptor(cluster: Cluster<T>): BitmapDescriptor {
        val clusterBucket = getClusterIconBucket(cluster)
        var clusterIcon = clusterIcons[clusterBucket]
        if (clusterIcon == null) {
            clusterIcon = createClusterIcon(clusterBucket)
            clusterIcons.put(clusterBucket, clusterIcon)
        }
        return clusterIcon
    }

    override fun createMarkerBitmapDescriptor(clusterItem: T): BitmapDescriptor {
        if (clusterItemIcon == null) {
            clusterItemIcon = createClusterItemIcon()
        }
        return clusterItemIcon!!
    }

    private fun createClusterIcon(clusterBucket: Int): BitmapDescriptor {
        @SuppressLint("InflateParams")
        val clusterIconView = LayoutInflater.from(context)
            .inflate(R.layout.view_map_cluster_icon, null) as TextView
        clusterIconView.apply {
            background = createClusterBackground()
            setTextColor(iconStyle.clusterTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, iconStyle.clusterTextSize.toFloat())
            text = getClusterIconText(clusterBucket)
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            layout(
                0, 0, clusterIconView.measuredWidth,
                clusterIconView.measuredHeight
            )
        }
        val iconBitmap = Bitmap.createBitmap(
            clusterIconView.measuredWidth,
            clusterIconView.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(iconBitmap)
        clusterIconView.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(iconBitmap)
    }

    private fun createClusterBackground(): Drawable {
        val gradientDrawable = GradientDrawable()
        gradientDrawable.apply {
            shape = GradientDrawable.OVAL
            setColor(iconStyle.clusterBackgroundColor)
            setStroke(
                iconStyle.clusterStrokeWidth,
                iconStyle.clusterStrokeColor
            )
        }

        return gradientDrawable
    }

    private fun createClusterItemIcon(): BitmapDescriptor {
        return BitmapDescriptorFactory.fromResource(iconStyle.clusterIconResId)
    }

    private fun getClusterIconBucket(cluster: Cluster<T>): Int {
        val itemCount = cluster.items.size
        if (itemCount <= CLUSTER_ICON_BUCKETS[0]) {
            return itemCount
        }
        for (i in 0 until CLUSTER_ICON_BUCKETS.size - 1) {
            if (itemCount < CLUSTER_ICON_BUCKETS[i + 1]) {
                return CLUSTER_ICON_BUCKETS[i]
            }
        }
        return CLUSTER_ICON_BUCKETS[CLUSTER_ICON_BUCKETS.size - 1]
    }

    private fun getClusterIconText(clusterIconBucket: Int): String {
        return if (clusterIconBucket < CLUSTER_ICON_BUCKETS[0]) {
            clusterIconBucket.toString()
        } else {
            "$clusterIconBucket+"
        }
    }

    companion object {
        private val CLUSTER_ICON_BUCKETS = intArrayOf(10, 20, 50, 100, 500, 1000, 5000, 10000, 20000, 50000, 100000)
    }
}
