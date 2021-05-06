package com.begoml.clustermanager

import com.huawei.hms.maps.model.BitmapDescriptor

interface BitmapDescriptorGenerator<T : ClusterItem> {

    fun createClusterBitmapDescriptor(cluster: Cluster<T>): BitmapDescriptor

    fun createMarkerBitmapDescriptor(clusterItem: T): BitmapDescriptor
}
