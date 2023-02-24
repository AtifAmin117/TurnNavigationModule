package com.invozone.mapboxnavigation.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.invozone.mapboxnavigation.base.BaseApplication
import com.invozone.mapboxnavigation.model.MainPlace
import com.invozone.mapboxnavigation.model.PlaceType
import com.invozone.mapboxnavigation.repository.PlaceDetailRepository
import com.invozone.mapboxnavigation.repository.PlacePredictionRepository
import com.invozone.mapboxnavigation.storage.KeyListPref
import com.invozone.mapboxnavigation.storage.getPref
import com.invozone.mapboxnavigation.storage.setPref


class PlacePredictionViewModel(val mApplication: Application) : BaseViewModel(mApplication) {
    private val placePredictionRepository by lazy {
        PlacePredictionRepository(mApplication as BaseApplication)
    }
    private val placeDetailRepository by lazy {
        PlaceDetailRepository(mApplication as BaseApplication)
    }
    private var recentList: ArrayList<MainPlace>  = mApplication.getPref(KeyListPref.RECENT_LIST)
    private var _predictionLiveData = MutableLiveData<List<AutocompletePrediction>>()
    var predictionLiveData :  LiveData<List<AutocompletePrediction>> = _predictionLiveData

    fun searchPlace(query: String?,isNewSession: Boolean) {
        placePredictionRepository.getPlacePredictions(query, isNewSession, _predictionLiveData)
    }

    // Get place details from PlaceId
    fun getPlaceDetailById(prediction: AutocompletePrediction, placeType: PlaceType, onPlaceDetails: (MainPlace) -> Unit){
        placeDetailRepository.getPlaceDetailsById(prediction.placeId){ MainPlace ->
            onPlaceDetails(addPlaceInRecentList(prediction,placeType,MainPlace))
        }
    }
    private fun addPlaceInRecentList(prediction: AutocompletePrediction,placeType: PlaceType,placeDetail: Place): MainPlace{
        // Remove existing place from the list
        recentList.find { p -> p.place_id == prediction.placeId }?.let { recentPlace ->
            recentList.remove(recentPlace)
        }
        // Create a new place from prediction and detail fetched from google with placeType
        val MainPlace = MainPlace(
            place_type = placeType,
            place_address = prediction.getSecondaryText(null).toString(),
            place_id = prediction.placeId,
            place_name = prediction.getPrimaryText(null).toString(),
            place_latLong = placeDetail.latLng
        )
        // Add a place to recent list
        recentList.add(MainPlace)
        // Save list to shared preference
        mApplication.setPref(KeyListPref.RECENT_LIST, recentList)
        return  MainPlace
    }

}