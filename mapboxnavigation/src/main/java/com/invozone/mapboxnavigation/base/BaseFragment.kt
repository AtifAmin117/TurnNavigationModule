package com.invozone.mapboxnavigation.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.android.libraries.places.api.net.PlacesClient
import com.invozone.mapboxnavigation.fragment.ProgressDialogFragment
import com.ncapdevi.fragnav.FragNavTransactionOptions

open class BaseFragment : Fragment() {

    private var mFragmentNavigation: FragmentNavigation? = null

    /**
     * Used when we dont want to create/inflate view if same fragment instance is used.
     */
    var isFirstTimeLoad: Boolean = true
    var prevViewDataBinding: ViewDataBinding? = null
    fun <T : ViewDataBinding> createOrReloadView(
        inflater: LayoutInflater,
        resLayout: Int,
        container: ViewGroup?
    ): T {
        isFirstTimeLoad = false
        //adjustFontScale(resources.configuration)
        if (prevViewDataBinding == null) {
            prevViewDataBinding = DataBindingUtil.inflate(inflater, resLayout, container, false)
            isFirstTimeLoad = true
        } else if (prevViewDataBinding?.root?.parent != null) {
            container?.removeView(prevViewDataBinding?.root)
            if (prevViewDataBinding?.root?.parent != null) {
                return createOrReloadView(inflater, resLayout, container)
            }
        }
        return prevViewDataBinding as T
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachObserver()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentNavigation) {
            mFragmentNavigation = context
        }
    }

    interface FragmentNavigation {
        fun pushFragment(fragment: Fragment, transactionOptions: FragNavTransactionOptions? = null)

        fun popFragment(depth: Int, transactionOptions: FragNavTransactionOptions? = null)
    }

    /*fun adjustFontScale(configuration: Configuration) {
        configuration.fontScale = 1.0.toFloat()
        val metrics = resources.displayMetrics
        val wm = activity?.getSystemService(Context.WINDOW_SERVICE) as WindowManager?

        wm?.defaultDisplay?.getMetrics(metrics)
        metrics.scaledDensity = configuration.fontScale * metrics.density
        configuration.densityDpi = resources.displayMetrics.xdpi.toInt()
        activity?.baseContext?.resources?.updateConfiguration(configuration, metrics)
    }*/
    open fun onBackPressed(): Boolean {
        return false
    }

    fun pushFragment(fragment: Fragment, transactionOptions: FragNavTransactionOptions? = null) {
        if (mFragmentNavigation != null) {
            mFragmentNavigation?.pushFragment(fragment, transactionOptions)
        } else {
            // todo logic when home Activity not implemented fragmentNavigation
        }
    }

    fun popFragment(depth: Int = 1) {
//        hideKeyboard(activity)
        if (mFragmentNavigation != null) {
            mFragmentNavigation?.popFragment(depth)
        }
    }

    private val progressDialogFragment: ProgressDialogFragment by lazy { ProgressDialogFragment.newInstance() }

    fun showProgressDialog(isShow: Boolean) {
        if (isShow) {
            try {
                progressDialogFragment.dismissAllowingStateLoss()
            } catch (e: Exception) {
            }
            progressDialogFragment.show(
                activity?.supportFragmentManager!!,
                ProgressDialogFragment.FRAGMENT_TAG
            )
        } else {
            try {
                progressDialogFragment.dismissAllowingStateLoss()
            } catch (e: Exception) {
            }
        }

    }

    open fun attachObserver() {}
    fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}