package com.invozone.mapboxnavigation.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.invozone.mapboxnavigation.databinding.InflateProgressViewBinding

class ProgressDialogFragment : DialogFragment() {
    var binding: InflateProgressViewBinding? = null

    companion object {
        var FRAGMENT_TAG = "dialog"
        fun newInstance(): ProgressDialogFragment {
            val dialogFragment = ProgressDialogFragment()
            dialogFragment.isCancelable = false
            return dialogFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = InflateProgressViewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onStart() {
        super.onStart()

        val window = dialog?.window
        val windowParams = window!!.attributes
//        windowParams.dimAmount = 0f

        window.decorView.setBackgroundResource(android.R.color.transparent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
//        windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
//        windowParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
        window.attributes = windowParams
    }
}