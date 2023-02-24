package com.invozone.mapboxnavigation.model
import com.google.android.gms.maps.model.LatLng

class Trip {
    private var tripStaxiPlaces: ArrayList<MainPlace> = arrayListOf()

    fun getPlaceByPlaceType(placeType: PlaceType): MainPlace?{
        return  tripStaxiPlaces.find {
            it.place_type == placeType
        }
    }

    fun isPickupAndDestinationsLocationAvailable(): Boolean{
        var isSourceAdded=false
        var isDestination = false
        for (model in tripStaxiPlaces){
            if (model.place_address.isNullOrEmpty() || model.place_latLong == null){
                return false
            }
            if (model.place_type == PlaceType.PICKUP){
                isSourceAdded =true
            }

            if (model.place_type == PlaceType.DESTINATION){
                isDestination =true
            }
        }

        return isSourceAdded && isDestination

    }

    fun addPlace(StaxiPlace: MainPlace){
        tripStaxiPlaces.removeIf { it.place_type == StaxiPlace.place_type}
        tripStaxiPlaces.add(StaxiPlace)
    }

    fun removePlaceByPlaceType(placeType: PlaceType){
        tripStaxiPlaces.removeIf {
            it.place_type == placeType
        }
    }

    fun removePlacesExcept(placeType: PlaceType){
        tripStaxiPlaces.removeIf {
            it.place_type != placeType
        }
    }

    fun getTripData(): java.util.ArrayList<MainPlace> {
        return arrayListOf<MainPlace>().apply {
            getPlaceByPlaceType(PlaceType.PICKUP)?.let {
                add(it)
            }
            getPlaceByPlaceType(PlaceType.DESTINATION)?.let {
                add(it)
            }
        }
    }

    fun addPlace(placeType: PlaceType, currLatLong: LatLng, currLocationAddress: String){
        addPlace(
            MainPlace(
                place_latLong = currLatLong,
                place_address = currLocationAddress,
                place_type = placeType
            )
        )
    }
}