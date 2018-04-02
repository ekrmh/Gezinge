package com.app.gezinge

import com.beust.klaxon.Json

data class Data(@Json(name = "trk") val data: SubData)
data class SubData(@Json(name = "trkpts") val locations: List<Location> = arrayListOf())
data class Location(var dateTime: String = "",
                      var lon: Double = 0.0,
                      var lat: Double = 0.0,
                      var ele: Double = 0.0)

data class myModel(var latF:Double= 0.0,var lonF:Double= 0.0,var latL:Double= 0.0,var lonL:Double = 0.0,var locations: ArrayList<mySubModel> = arrayListOf())

data class mySubModel(var lat:Double =0.0,var lon: Double=0.0)
