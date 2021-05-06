package com.begoml.clustermanager

class Cluster<T : ClusterItem> constructor(
        val latitude: Double,
        val longitude: Double,
        val items: List<T>,
        private val north: Double,
        private val west: Double,
        private val south: Double,
        private val east: Double
) {

    fun contains(latitude: Double, longitude: Double): Boolean {
        return longitude in west..east && latitude <= north && latitude >= south
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val cluster = other as Cluster<*>
        return cluster.latitude.compareTo(latitude) == 0 &&
                cluster.longitude.compareTo(longitude) == 0
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long = java.lang.Double.doubleToLongBits(latitude)
        result = (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(longitude)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }
}
