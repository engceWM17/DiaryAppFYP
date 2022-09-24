package com.example. diaryapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.media.browse.MediaBrowser
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.VolleyLog
import com.example.diaryapp.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.*

class LocationMapsActivity : AppCompatActivity(), OnMapReadyCallback , LocationListener{

    private lateinit var mMap: GoogleMap
    var fusedlocationProviderClient: FusedLocationProviderClient?= null
    var currentLocation: Location? = null
    var currentMarker: Marker?= null
    var currentAddress:String ?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_maps)
        val searchbtn: Button = findViewById(R.id.btSearch)
        val btConfirm: Button = findViewById(R.id.btconfirm)
        val etSearchLocation : EditText= findViewById(R.id.etSearchLocation)
        val location = intent.getStringExtra("location")

        if (location != "" && location != null) {
            val address=searchLocation(location)
            if( address!= null) {
                currentLocation = Location("")
                currentLocation!!.latitude= address.latitude
                currentLocation!!.longitude=address.longitude
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }else {
            fusedlocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fetchLocation()
        }

        searchbtn.setOnClickListener {
            val address = searchLocation(etSearchLocation.text.toString().trim())
            if (address!=null) {
                drawMarker(address)
            }
        }

        btConfirm.setOnClickListener {
            intent.putExtra("location", currentAddress)
            Log.i("", "Response address is: $currentAddress")
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun fetchLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=
            PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),1000)
            return
        }

        val task = fusedlocationProviderClient?.lastLocation
        task?.addOnSuccessListener { location->
            if (location!=null){
                this.currentLocation = location
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }

    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            1000 -> if (grantResults.size>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                fetchLocation()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (currentLocation == null){
            currentLocation = Location("")
            currentLocation!!.latitude= 0.0
            currentLocation!!.longitude=0.0
        }

        val latlong =LatLng(currentLocation?.latitude!!,currentLocation?.longitude!!)
        drawMarker(latlong)

        mMap.setOnMarkerDragListener(object: GoogleMap.OnMarkerDragListener{
            override fun onMarkerDrag(p0: Marker) {

            }

            override fun onMarkerDragEnd(p0: Marker) {
                if(currentMarker!= null){
                    currentMarker?.remove()
                }
                val newLatLong = LatLng(p0.position.latitude,p0.position.longitude)
                drawMarker(newLatLong)
            }

            override fun onMarkerDragStart(p0: Marker) {

            }
        })
    }

    private fun drawMarker(latlong:LatLng){
        currentAddress = getAddress(latlong.latitude, latlong.longitude)
        val markeroptions = MarkerOptions().position(latlong).title("I am here")
            .snippet(currentAddress).draggable(true)
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latlong))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlong,15f))
        currentMarker = mMap.addMarker(markeroptions)
        currentMarker?.showInfoWindow()
    }

    private fun getAddress(lat: Double, long:Double):String?{
        val geoCoder = Geocoder(this, Locale.getDefault())
        val addresses = geoCoder.getFromLocation(lat,long,1)
        return addresses[0].getAddressLine(0).toString()
    }

    override fun onLocationChanged(location: Location) {
        if (currentMarker!= null) {
            currentMarker!!.remove()
        }
        val latLong= LatLng(location.latitude,location.longitude)
        drawMarker(latLong)
    }

    private fun searchLocation(location:String):LatLng?{
        var addressList: List<Address>?= null

        if ( location == ""){
            Toast.makeText(this, "Provide Location", Toast.LENGTH_SHORT)
                .show()
            return null
        }else{
            val geocoder = Geocoder(this)
            try {
                addressList = geocoder.getFromLocationName(location,1)

            }catch (e:IOException){
                e.printStackTrace()
            }

            if (addressList!!.size!=0){
                val address = addressList!![0]
                val latLng = LatLng(address.latitude,address.longitude)

                return latLng
            }else {

                return null
            }
        }
    }
}