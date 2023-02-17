package com.invozone.mapboxnavigation

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class MainApplication:Application() {
    private lateinit var placesClient: PlacesClient
    companion object {
        lateinit var _applicationContext: MainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        _applicationContext = this
    }

    fun getPlaceClient(): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(this, resources.getString(R.string.places_api_key))
        }
        if (::placesClient.isInitialized) {
            return placesClient
        } else {
            placesClient = Places.createClient(this)
            return placesClient
        }
    }

}