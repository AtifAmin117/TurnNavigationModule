package com.invozone.mapboxnavigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.net.PlacesClient
import com.invozone.mapboxnavigation.ConstantsUtils.locationDestination
import com.invozone.mapboxnavigation.ConstantsUtils.locationStart
import com.invozone.mapboxnavigation.LocationHandler.isGpsEnabled
import com.invozone.mapboxnavigation.LocationHandler.isLocationApproved
import com.invozone.mapboxnavigation.ScreenUtils.getScreenHeight
import com.invozone.mapboxnavigation.ScreenUtils.getScreenWidth
import com.invozone.navigationmapbox.adapter.PlaceArrayAdapter
import com.invozone.navigationmapbox.model.PlaceDataModel
import kotlinx.coroutines.*
import java.util.*

open class LocationSelectionActivity : AppCompatActivity() {
    private val placesClient: PlacesClient get() = (application as MainApplication).getPlaceClient()
    private var startPlaceAdapter: PlaceArrayAdapter? = null
    private var destinationPlaceAdapter: PlaceArrayAdapter? = null
    private lateinit var mPlacesClient: PlacesClient
    private lateinit var etStartText: AppCompatAutoCompleteTextView
    private lateinit var etDestinationText: AppCompatAutoCompleteTextView
    private lateinit var btnStartNav: Button

    private val handler = CoroutineExceptionHandler { _, exception ->
        Log.e("RouteFinderTAg", "$exception")
        isRouteFetched = false
    }


    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var progressDialog: Dialog? = null

    private var isRouteFetched = false

    private var myCurrentAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.location_selection_activity)
        etStartText = findViewById(R.id.etStartText)
        etDestinationText = findViewById(R.id.etDestinationText)
        btnStartNav = findViewById(R.id.btn_start_nav)
        if (isLocationApproved()) {
            if (isGpsEnabled()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    getCurrentLocation()
                }, 300)
            } else {
                try {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {

                }
            }

        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LocationHandler.LOCATION_PERMISSION
            )
        }

        startPlaceAdapter = PlaceArrayAdapter(this, R.layout.layout_item_places, placesClient)
        etStartText.setAdapter(startPlaceAdapter)

        destinationPlaceAdapter = PlaceArrayAdapter(this, R.layout.layout_item_places, placesClient)
        etDestinationText.setAdapter(destinationPlaceAdapter)

        etStartText.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val place = parent.getItemAtPosition(position) as PlaceDataModel
                etStartText.apply {
                    setText(place.fullText)
                    setSelection(etStartText.length())
                }
            }

        etDestinationText.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val place = parent.getItemAtPosition(position) as PlaceDataModel
                etDestinationText.apply {
                    setText(place.fullText)
                    setSelection(etDestinationText.length())
                }
            }
        onclickMethod()
        initProgressDialog()
    }

    private fun onclickMethod() {

        btnStartNav.setOnClickListener {
            if (etStartText.text.toString()
                    .isNotEmpty() && etDestinationText.text.toString().isNotEmpty()
            ) {
                findLocations(
                    etStartText.text.toString(),
                    etDestinationText.text.toString()
                )
            } else {
                Toast.makeText(this, "Please Enter Address", Toast.LENGTH_SHORT).show()

            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    locationStart.placeLatitude = location.latitude
                    locationStart.placeLongitude = location.longitude

                    findAddress(locationStart)
                }
            }

    }

    private fun initProgressDialog() {
        progressDialog = Dialog(this)
        progressDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = View.inflate(this, R.layout.dialog_progress, null)
        progressDialog?.setContentView(dialogView)
        progressDialog?.setCanceledOnTouchOutside(false)
        progressDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog?.setCancelable(false)
        val progressCard = dialogView.findViewById<LinearLayout>(R.id.progress_card)
        progressCard.requestLayout()
        progressCard.layoutParams.width =
            (getScreenWidth() * .90).toInt()
        progressCard.layoutParams.height =
            (getScreenHeight() * .10).toInt()

    }

    private fun moveToDirection() {
        runOnUiThread {
            progressDialog?.dismiss()
        }

        startActivity(Intent(this, RouteDirectionActivity::class.java))
    }

    @SuppressLint("SetTextI18n")
    private fun findAddress(mLoc: MapLocation) {

        var addresses: List<Address> = ArrayList()

        runOnUiThread {
            progressDialog?.show()
        }

        CoroutineScope(Dispatchers.Main + handler).launch {
            async(Dispatchers.IO + handler) {

                val geocoder = Geocoder(this@LocationSelectionActivity, Locale.getDefault())
                addresses = geocoder.getFromLocation(mLoc.placeLatitude, mLoc.placeLongitude, 1)
            }.await()
            runOnUiThread {
                progressDialog?.dismiss()
            }

            try {
                if (addresses.isNotEmpty()) {
                    addresses[0].getAddressLine(0)?.let { mAddress ->
                        myCurrentAddress = mAddress
                        runOnUiThread {
                            etStartText.setText(mAddress)
                        }
                    } ?: run {
                        myCurrentAddress = "Unknown Place"
                        etStartText.setText("Unknown Place")
                    }
                }
            } catch (ex: Exception) {

            }
        }


    }

    private fun findLocations(startAddress: String, endAddress: String) {
        var startResponses: List<Address> = ArrayList()
        var endResponses: List<Address> = ArrayList()

        runOnUiThread {
            progressDialog?.show()
        }
        CoroutineScope(Dispatchers.Main + handler).launch {
            try {

                if (myCurrentAddress.equals(startAddress)) {
                    Log.d("MapInformation", "Addresses are equals")

                    async(Dispatchers.IO + handler) {
                        val geocoder = Geocoder(this@LocationSelectionActivity, Locale.getDefault())
                        endResponses = geocoder.getFromLocationName(endAddress, 1)
                        Log.d("MapInformation", "Geocoder End")
                    }.await()

                    if (endResponses.isNotEmpty()) {
                        Log.d("MapInformation", "Both Response are ready")
                        locationDestination =
                            MapLocation(endResponses[0].latitude, endResponses[0].longitude)

                        Log.d("MapInformation", "End Address: ${endResponses[0].getAddressLine(0)}")

                        moveToDirection()

                    } else {
                        Log.d("MapInformation", "Both Response not ready")
                        runOnUiThread {
                            progressDialog?.dismiss()
                        }
                    }

                } else {
                    Log.d("MapInformation", "Addresses are not equals")

                    async(Dispatchers.IO + handler) {
                        val geocoder = Geocoder(this@LocationSelectionActivity, Locale.getDefault())
                        startResponses = geocoder.getFromLocationName(startAddress, 1)
                        Log.d("MapInformation", "Geocoder Start")
                    }.await()


                    if (startResponses.isNotEmpty()) {
                        Log.d("MapInformation", "First Response is ready")
                        async(Dispatchers.IO + handler) {
                            val geocoder = Geocoder(this@LocationSelectionActivity, Locale.getDefault())
                            endResponses = geocoder.getFromLocationName(endAddress, 1)
                            Log.d("MapInformation", "Geocoder End")
                        }.await()

                        if (endResponses.isNotEmpty()) {
                            Log.d("MapInformation", "Both Response are ready")
                            locationStart =
                                MapLocation(startResponses[0].latitude, startResponses[0].longitude)
                            locationDestination =
                                MapLocation(endResponses[0].latitude, endResponses[0].longitude)
                            myCurrentAddress = startResponses[0].getAddressLine(0)
                            Log.d(
                                "MapInformation",
                                "Start Address: ${startResponses[0].getAddressLine(0)}"
                            )
                            Log.d(
                                "MapInformation",
                                "End Address: ${endResponses[0].getAddressLine(0)}"
                            )

                            moveToDirection()

                        } else {
                            Log.d("MapInformation", "Both Response not ready")
                            runOnUiThread {
                                progressDialog?.dismiss()
                            }
                        }

                    } else {
                        Log.d("MapInformation", "First Response not ready")
                        runOnUiThread {
                            progressDialog?.dismiss()
                        }
                    }

                }
            } catch (ex: Exception) {
                Log.d("MapInformation", "${ex.message}")
                runOnUiThread {
                    progressDialog?.dismiss()
                }
            }
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (LocationHandler.LOCATION_PERMISSION == requestCode) {
            try {
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (isGpsEnabled()) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                getCurrentLocation()
                            }, 300)
                        } else {
                            try {
                                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } catch (e: Exception) {

                            }
                        }
                    }
                }
            } catch (e: Exception) {

            }


        }
    }
}