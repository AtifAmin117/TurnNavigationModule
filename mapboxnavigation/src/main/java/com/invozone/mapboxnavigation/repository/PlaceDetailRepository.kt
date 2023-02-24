package com.invozone.mapboxnavigation.repository

import android.app.Application
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.invozone.mapboxnavigation.base.BaseApplication

class PlaceDetailRepository(val application: Application) {
    private val placesClient: PlacesClient get() = (application as BaseApplication).getPlaceClient()

    fun getPlaceDetailsById(placeId: String, onPlaceDetails: (Place) -> Unit) {
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
}