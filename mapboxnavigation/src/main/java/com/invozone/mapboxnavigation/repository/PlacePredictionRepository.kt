package com.invozone.mapboxnavigation.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.invozone.mapboxnavigation.base.BaseApplication

class PlacePredictionRepository(val application: BaseApplication) {
    private val placesClient: PlacesClient get() = application.getPlaceClient()
    private var sessionToken: AutocompleteSessionToken? = null
    fun getPlacePredictions(query: String?,isNewSession: Boolean,predictionsLiveData: MutableLiveData<List<AutocompletePrediction>>) {
        if(isNewSession || sessionToken == null){
            sessionToken = AutocompleteSessionToken.newInstance()
        }
        val newRequest = FindAutocompletePredictionsRequest
            .builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()

        // Perform autocomplete predictions request
        placesClient.findAutocompletePredictions(newRequest).addOnSuccessListener { response ->
            predictionsLiveData.postValue(response.autocompletePredictions)
        }.addOnFailureListener { exception: Exception? ->
            if (exception is ApiException) {
                Log.e("TAG", "Place not found: " + exception.statusCode)
            }
        }

    }
}