package com.invozone.mapboxnavigation.listener

import android.view.View

interface CustomOnClickListener<T> {
    fun onClick(item: T,position: Int,view: View)
}