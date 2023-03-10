package com.invozone.mapboxnavigation.repository

import android.app.Application
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.invozone.mapboxnavigation.R

class PlaceDetailRepository(val application: Application) {
    private lateinit var placesClient: PlacesClient

//    private val placesClient: PlacesClient get() = (application as BaseApplication).getPlaceClient()

    fun getPlaceDetailsById(placeId: String, onPlaceDetails: (Place) -> Unit) {
        getPlaceClient()
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response: FetchPlaceResponse ->
                onPlaceDetails(response.place)
            }.addOnFailureListener { exception: Exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                }
            }
    }
    fun getPlaceClient(): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(application, application.resources.getString(R.string.places_api_key))
        }
        if (::placesClient.isInitialized) {
            return placesClient
        } else {
            placesClient = Places.createClient(application)
            return placesClient
        }
    }
}