package com.begoml.clustermanager

interface ClusterItem : QuadTreePoint {

    override val latitude: Double

    override val longitude: Double

    val itemId: String

    val title: String

    val snippet: String
}
