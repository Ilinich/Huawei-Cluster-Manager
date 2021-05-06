package com.begoml.clustermanager.core

import com.huawei.hms.maps.model.Marker

internal class MarkerState constructor(
        val marker: Marker,
        var isDirty: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as MarkerState
        return if (isDirty != that.isDirty) false else marker == that.marker
    }

    override fun hashCode(): Int {
        var result = marker.hashCode()
        result = 31 * result + if (isDirty) 1 else 0
        return result
    }
}
