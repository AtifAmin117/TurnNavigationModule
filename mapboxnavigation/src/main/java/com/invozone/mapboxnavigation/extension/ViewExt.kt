package com.invozone.mapboxnavigation.extension

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import java.util.*

fun View.clickWithDebounce(debounceTime: Long = 700L, shouldHideKeyBoard:Boolean = true, action: () -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0
        override fun onClick(v: View) {
            if(shouldHideKeyBoard)
                hideKeyBoard()
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
            else action()
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

/**
 * to hide keyboard
 */
fun View.hideKeyBoard() {
    val inputManager = this.context
        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
}

/**
 * to show keyboard
 */
fun View.showKeyBoard() {
    this.postDelayed({
        val inputManager = this.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(this, InputMethodManager.HIDE_NOT_ALWAYS)
    }, 600)
}

fun hideKeyboard(activity: Activity?) {
    val view = activity?.findViewById<View>(android.R.id.content)
    if (view != null) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun showKeyboard(activity: Activity?) {
    val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

val Int.toPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics)



val Int.toDp get() = run {
    val density: Float = Resources.getSystem().displayMetrics.density
    (this / density).toInt()
}

fun View.slideUpAnimation() {
    this.visibility = View.VISIBLE
    val animate = TranslateAnimation(0f, 0f, this.height.toFloat(), 0f)
    animate.duration = 1000
    animate.fillAfter = true
    this.startAnimation(animate)
}
fun LatLng.getCompleteAddressString(context: Context): String {
    var strAdd = ""
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses: List<Address>? = geocoder.getFromLocation(this.latitude, this.longitude, 1)
        if (addresses != null) {
            val returnedAddress: Address = addresses[0]
            val strReturnedAddress = StringBuilder("")
            for (i in 0..returnedAddress.maxAddressLineIndex) {
                strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
            }
            strAdd = strReturnedAddress.toString()
            Log.w("My Current loction address", strReturnedAddress.toString())
        } else {
            Log.w("My Current loction address", "No Address returned!")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.w("My Current loction address", "Canont get Address!")
    }
    return strAdd
}
fun getLocationRequest(needHighAccurate: Boolean): LocationRequest {
    val mLocationRequest = LocationRequest.create()
    mLocationRequest.interval = 10000
    mLocationRequest.smallestDisplacement = 5f
    mLocationRequest.fastestInterval = 5000
    mLocationRequest.priority =
        if (needHighAccurate) LocationRequest.PRIORITY_HIGH_ACCURACY else LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    return mLocationRequest
}