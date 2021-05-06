package com.begoml.huaweiclustermanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.begoml.clustermanager.HuaweiClusterItem
import com.begoml.clustermanager.HuaweiClusterManager
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.SupportMapFragment
import com.huawei.hms.maps.model.LatLng
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var huaweiMap: HuaweiMap
    private var supportMapFragment: SupportMapFragment? = null
    private var clusterManager: HuaweiClusterManager<HuaweiClusterItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportMapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        supportMapFragment?.getMapAsync(this)
    }

    override fun onMapReady(huaweiMap: HuaweiMap) {
        this.huaweiMap = huaweiMap

        clusterManager = HuaweiClusterManager(this, this.huaweiMap)

        val clusterItems: MutableList<HuaweiClusterItem> = mutableListOf()
        for (i in 0..99999) {
            clusterItems.add(
                HuaweiClusterItem(
                    itemId = "1",
                    location = LatLng(Random.nextDouble(), Random.nextDouble())
                )
            )
        }
        huaweiMap.setOnCameraIdleListener(clusterManager)
        clusterManager?.addItems(clusterItems)
    }


    override fun onDestroy() {
        clusterManager?.clearSate()
        super.onDestroy()
    }
}
