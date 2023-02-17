package com.invozone.mapboxnavigation


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MapLocation(var placeLatitude:Double, var placeLongitude:Double): Parcelable
