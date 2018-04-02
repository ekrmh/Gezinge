package com.app.gezinge

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.beust.klaxon.Klaxon


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.loopj.android.http.*
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.StringEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import com.afollestad.materialdialogs.MaterialDialog
import android.R.array
import android.view.View
import com.google.gson.Gson


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val URL: String = "http://192.168.43.61:3000"
    private lateinit var mMap: GoogleMap
    private var path: Data? = null
    private val TAG = MapsActivity::class.java.simpleName
    private lateinit var dialog:ProgressDialog
    private var filteredData:String = ""
    private var filteredModelData:myModel = myModel()
    private var ModelData:myModel = myModel()
    private var arraySearch = arrayListOf<Marker>()
    private var searchFilterData:Boolean = false
    private var data: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        dialog= ProgressDialog(this)
        dialog.setMessage("Please wait")
        dialog.setTitle("Loading")
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.setOnMapLongClickListener {
            if(arraySearch.size >= 2) {
                arraySearch.forEach { it.remove() }
                arraySearch.clear()
            }
            val marker = mMap.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).title("1").icon(BitmapDescriptorFactory.fromResource(R.drawable.search)))
            arraySearch.add(marker)
            if(arraySearch.size == 2){

                val postData = if(searchFilterData) filteredModelData else ModelData
                postData.latF = arraySearch[0].position.latitude
                postData.lonF = arraySearch[0].position.longitude
                postData.latL = arraySearch[1].position.latitude
                postData.lonL = arraySearch[1].position.longitude
                Log.d(TAG,"$postData")

                val client = AsyncHttpClient()
                var se: StringEntity? = null
                val SEARCH_URL = "$URL/search"
                try {
                    se = StringEntity (Gson().toJson(postData))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()

                }
                Log.d(TAG,Gson().toJson(postData).toString())
                client.post(applicationContext, SEARCH_URL, se, "application/json",
                        object : TextHttpResponseHandler() {
                            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseString: String?) {
                                responseString?.let {
                                    try {
                                        val rootObject = JSONObject(it)
                                        val rows = rootObject.getJSONArray("search") // Get all JSONArray data
                                        val count = rows.length()
                                        for (i in 0 until count) {
                                            val jsonArr = rows.getJSONObject(i)
                                            Log.d(TAG, "jsonArray $i: $jsonArr")

                                            val lat = jsonArr.get("lat") as Double
                                            val lon = jsonArr.get("lon") as Double
                                            filteredModelData.locations.add(mySubModel(lat, lon))
                                            mMap.addMarker(MarkerOptions().position(LatLng(lat, lon)).title("$lat").icon(BitmapDescriptorFactory.fromResource(R.drawable.search)))
                                        }

                                    } catch (e: JSONException) {
                                        e.message?.showToast()
                                    }
                                }
                            }

                            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable: Throwable?) {
                            }

                        })
            }
        }

    }
    private fun reset(){
        path = null
        filteredData = ""
        filteredModelData= myModel()
        ModelData= myModel()
        arraySearch = arrayListOf()
        searchFilterData= false
        data= ""
        mMap.clear()

    }
    private fun addMarkers() {
        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.RED)
        polylineOptions.width(3f)
            var count = 0
            path?.data?.locations?.forEach {
                mMap.addMarker(MarkerOptions().position(LatLng(it.lat, it.lon)).title("p$count").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_tracker)))
                polylineOptions.add(LatLng(it.lat, it.lon))
                count++
            }
        path?.data?.locations?.let {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(it[0].lat,it[0].lon)))
        }



        mMap.addPolyline(polylineOptions)
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            dialog.show()
            when (it.itemId) {
                R.id.add_file -> {
                    "Adding File".showToast()
                    val data: String = applicationContext.assets.open("example.json").bufferedReader().use { it.readText() }
                    path = Klaxon().parse<Data>(data)
                    path?.let {
                        addMarkers()
                    }


                }
                R.id.reduction -> {
                    "Reduction".showToast()
                    val client = AsyncHttpClient()
                    data= applicationContext.assets.open("example.json").bufferedReader().use { it.readText() }
                    val result = Klaxon().parse<Data>(data)
                    result?.data?.locations?.forEach {
                        ModelData.locations.add(mySubModel(it.lat,it.lon))
                    }
                    var se: StringEntity? = null
                    try {
                        se = StringEntity (data);
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace();

                    }
                    client.post(applicationContext, "$URL/", se, "application/json",
                            object : TextHttpResponseHandler() {
                                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseString: String?) {
                                    responseString?.let {
                                        filteredData = it
                                        val polylineOptions = PolylineOptions()
                                        polylineOptions.color(Color.GREEN)
                                        polylineOptions.width(5f)
                                        try {
                                            val rootObject = JSONObject(it)
                                            rootObject.get("rate").toString().showToast()
                                            val rows = rootObject.getJSONArray("filteredData") // Get all JSONArray data
                                            val count = rows.length()
                                            for (i in 0 until count) {
                                                val jsonArr = rows.getJSONArray(i)
                                                Log.d(TAG,"jsonArray $i: $jsonArr")

                                                val lat = jsonArr[0] as Double
                                                val lon = jsonArr[1] as Double
                                                filteredModelData.locations.add(mySubModel(lat,lon))
                                                mMap.addMarker(MarkerOptions().position(LatLng(lat, lon)).title("$lat").icon(BitmapDescriptorFactory.fromResource(R.drawable.select)))
                                                polylineOptions.add(LatLng(lat, lon))
                                            }
                                            mMap.addPolyline(polylineOptions)

                                        }
                                         catch (e: JSONException) {
                                             e.message?.showToast()
                                        }

                                    }


                                }

                                override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable: Throwable?) {
                                    Log.d(TAG,responseString)
                                }

                            })
                }
                R.id.search -> {
                    MaterialDialog.Builder(this)
                            .title("Arama Seçimi")
                            .items(arrayListOf("Ana Veri","Filtrelenmiş Veri"))
                            .itemsCallbackSingleChoice(-1) { dialog, itemView, which, text ->
                                searchFilterData = !text.toString().equals("Ana Veri",ignoreCase = true)
                                "$text  : $searchFilterData".showToast()

                                true
                            }
                            .positiveText("Tamam")
                            .show()

                }
                R.id.reset -> {
                    reset()
                }

                else -> {
                    return super.onOptionsItemSelected(item)
                }
            }
        }
        if(dialog.isShowing) dialog.dismiss()
        return true
}


    private fun String.showToast() = Toast.makeText(applicationContext, this, Toast.LENGTH_SHORT).show()
}
