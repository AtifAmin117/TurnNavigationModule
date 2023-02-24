package com.invozone.mapboxnavigation.base

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.invozone.mapboxnavigation.fragment.ProgressDialogFragment
import com.ncapdevi.fragnav.FragNavTransactionOptions


open class BaseActivity : AppCompatActivity() {
    private val progressDialogFragment: ProgressDialogFragment by lazy { ProgressDialogFragment.newInstance() }

    open fun attachObserver() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachObserver()

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    fun setStatusBarColor(color: Int) {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255

        if (Build.VERSION.SDK_INT < 30) {
            var systemUiVisibility = window.decorView.systemUiVisibility
            systemUiVisibility = if (darkness < 0.5) {
                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = systemUiVisibility
        } else {

            if (darkness > 0.5) {
                val controller = window.insetsController
                controller?.setSystemBarsAppearance(
                    0,
                    APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {

                val controller = window.insetsController
                controller?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS,
                    APPEARANCE_LIGHT_STATUS_BARS
                )
            }

        }

        window.statusBarColor = color
    }

    fun showProgressDialog(isShow: Boolean) {
        if (isShow) {
            try {
                progressDialogFragment.dismissAllowingStateLoss()
            } catch (e: Exception) {
            }
            progressDialogFragment.show(
                supportFragmentManager,
                ProgressDialogFragment.FRAGMENT_TAG
            )
        } else {
            try {
                progressDialogFragment.dismissAllowingStateLoss()
            } catch (e: Exception) {
            }
        }

    }
}