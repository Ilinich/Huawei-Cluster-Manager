package com.begoml.clustermanager.core

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.begoml.clustermanager.R

internal data class IconStyle(
    val context: Context,

    val clusterBackgroundColor: Int = ContextCompat.getColor(
        context, R.color.huawei_cluster_background
    ),
    val clusterTextColor: Int = ContextCompat.getColor(
        context, R.color.huawei_cluster_text
    ),

    val clusterStrokeColor: Int = ContextCompat.getColor(
        context, R.color.huawei_cluster_stroke
    ),


    val clusterStrokeWidth: Int = context.resources
        .getDimensionPixelSize(R.dimen.huawei_cluster_stroke_width),

    val clusterTextSize: Int = context.resources
        .getDimensionPixelSize(R.dimen.huawei_cluster_text_size),

    @DrawableRes
    val clusterIconResId: Int = R.mipmap.ic_marker_green,
)
