package com.begoml.clustermanager.core

import com.begoml.clustermanager.QuadTreePoint

internal class QuadTree<T : QuadTreePoint>(private val bucketSize: Int) {

    private var root: QuadTreeNode

    fun insert(point: T) {
        root.insert(point)
    }

    fun queryRange(north: Double, west: Double, south: Double, east: Double): List<T> {
        val points: MutableList<QuadTreePoint> = mutableListOf()
        root.queryRange(QuadTreeRect(north, west, south, east), points)
        return points as List<T>
    }

    fun clear() {
        root = createRootNode(bucketSize)
    }

    private fun createRootNode(bucketSize: Int): QuadTreeNode {
        return QuadTreeNode(90.0, -180.0, -90.0, 180.0, bucketSize)
    }

    init {
        this.root = createRootNode(bucketSize)
    }
}
