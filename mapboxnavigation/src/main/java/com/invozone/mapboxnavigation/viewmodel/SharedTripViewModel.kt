package com.example.mapboxturnmodule.viewmodel

import android.app.Application
import com.invozone.mapboxnavigation.model.Trip
import com.invozone.mapboxnavigation.viewmodel.BaseViewModel

class SharedTripViewModel(application: Application): BaseViewModel(application) {
    val trip = Trip()
}