package com.begoml.clustermanager

import com.huawei.hms.maps.model.LatLng

data class HuaweiClusterItem(
    override val itemId: String,
    private val location: LatLng,
    override val snippet: String = "",
    override val title: String = "",
) : ClusterItem {

    override val latitude: Double
        get() = location.latitude

    override val longitude: Double
        get() = location.longitude
}
