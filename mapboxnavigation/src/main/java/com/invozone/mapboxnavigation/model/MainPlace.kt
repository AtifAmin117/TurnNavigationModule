package com.invozone.mapboxnavigation.model

import com.google.android.gms.maps.model.LatLng

data class MainPlace(
    val place_id: String? = null,
    val place_name: String? = null,
    val place_address: String? = null,
    val place_latLong: LatLng? = null,
    var place_type: PlaceType? = null
) {
     fun getPlaceLabel(): String? {
       return if (place_name.isNullOrEmpty()) {
            place_address
        } else {
            place_name
        }
    }
}

enum class PlaceType {
    PICKUP,
    DESTINATION
}

