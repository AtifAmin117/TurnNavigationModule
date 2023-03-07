package com.invozone.mapboxnavigation.activity

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.invozone.mapboxnavigation.R
import com.invozone.mapboxnavigation.base.BaseActivity
import com.invozone.mapboxnavigation.base.BaseFragment
import com.invozone.mapboxnavigation.databinding.ActivityHomeBinding
import com.invozone.mapboxnavigation.extension.hideKeyboard
import com.invozone.mapboxnavigation.fragment.NavigationFragment
import com.ncapdevi.fragnav.FragNavController
import com.ncapdevi.fragnav.FragNavLogger
import com.ncapdevi.fragnav.FragNavSwitchController
import com.ncapdevi.fragnav.FragNavTransactionOptions
import com.ncapdevi.fragnav.tabhistory.UniqueTabHistoryStrategy


const val INDEX_HOME = FragNavController.TAB1
open class HomeActivity : BaseActivity(), BaseFragment.FragmentNavigation,
    FragNavController.TransactionListener, FragNavController.RootFragmentListener {


    lateinit var binding: ActivityHomeBinding
    private val fragNavController: FragNavController =
        FragNavController(supportFragmentManager, R.id.container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        initializeFragmentNavigation(savedInstanceState)

    }

    private fun initializeFragmentNavigation(savedInstanceState: Bundle?){
        fragNavController.apply {
            transactionListener = this@HomeActivity
            rootFragmentListener = this@HomeActivity
            createEager = true
            fragNavLogger = object : FragNavLogger {
                override fun error(message: String, throwable: Throwable) {
                    //Log.e("TAG", message, throwable)
                }
            }

            defaultTransactionOptions = FragNavTransactionOptions.newBuilder().build()
            fragmentHideStrategy = FragNavController.DETACH

            navigationStrategy = UniqueTabHistoryStrategy(object : FragNavSwitchController {
                override fun switchTab(index: Int, transactionOptions: FragNavTransactionOptions?) {
                    //Log.e("switchTab", index.toString())
                    fragNavController.switchTab(index)
                }
            })
        }
        fragNavController.initialize(INDEX_HOME, savedInstanceState)
    }



    override val numberOfRootFragments: Int
        get() = 1

    override fun getRootFragment(index: Int): Fragment {
       return NavigationFragment.newInstance()
    }

    override fun onFragmentTransaction(
        fragment: Fragment?,
        transactionType: FragNavController.TransactionType
    ) {
        supportActionBar?.setDisplayHomeAsUpEnabled(fragNavController.isRootFragment.not())
    }

    override fun onTabTransaction(fragment: Fragment?, index: Int) {
        supportActionBar?.setDisplayHomeAsUpEnabled(fragNavController.isRootFragment.not())
    }

    override fun pushFragment(fragment: Fragment, transactionOptions: FragNavTransactionOptions?) {
        if (transactionOptions != null) {
            fragNavController.pushFragment(fragment, transactionOptions)
        } else {
            fragNavController.pushFragment(fragment)
        }
    }

    override fun popFragment(depth: Int, transactionOptions: FragNavTransactionOptions?) {
        if (transactionOptions != null) {
            fragNavController.popFragments(depth, transactionOptions)
        } else {
            fragNavController.popFragments(depth)
        }

    }
    private var isBackPressed = false
    override fun onBackPressed() {
        val currentFragment = fragNavController.currentFrag
        hideKeyboard(this@HomeActivity)
        /*if (isOpen) {
            showHideMenuView()
        } else */if (currentFragment != null && currentFragment is BaseFragment && currentFragment.onBackPressed()) {
            return
        }
    }


}