package com.invozone.mapboxnavigation.fragment

import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapboxturnmodule.FavoriteListAdapter
import com.example.mapboxturnmodule.PlacePredictionAdapter
import com.example.mapboxturnmodule.RecentPlaceListAdapter
import com.example.mapboxturnmodule.viewmodel.SharedTripViewModel
import com.google.android.gms.maps.model.LatLng
import com.invozone.mapboxnavigation.R
import com.invozone.mapboxnavigation.base.BaseFragment
import com.invozone.mapboxnavigation.databinding.FragmentEnterDestinationBinding
import com.invozone.mapboxnavigation.extension.getCompleteAddressString
import com.invozone.mapboxnavigation.listener.CustomOnClickListener
import com.invozone.mapboxnavigation.model.*
import com.invozone.mapboxnavigation.storage.KeyListPref
import com.invozone.mapboxnavigation.storage.getPref
import com.invozone.mapboxnavigation.viewmodel.PlacePredictionViewModel
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import java.util.*
import kotlin.collections.ArrayList

class EnterDestinationFragment : BaseFragment() {
    private lateinit var binding: FragmentEnterDestinationBinding
    private lateinit var adapter: ConcatAdapter
    private lateinit var favoriteAdapter: FavoriteListAdapter
    private lateinit var recentPlaceListAdapter: RecentPlaceListAdapter
    private val placeAdapter = PlacePredictionAdapter()
    private var pickupListenerCallRequired = true
    private var destinationListenerCallRequired = true

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var recentList: ArrayList<MainPlace>
    private var edtType: PlaceType = PlaceType.PICKUP

    private lateinit var textWatcherPickup: TextWatcher
    private lateinit var textWatcherDestination: TextWatcher

    private val sharedTripViewModel by activityViewModels<SharedTripViewModel>()
    private val placePredictionViewModel by viewModels<PlacePredictionViewModel>()
    private var isNewSession = false

    /*mapbox*/
    private lateinit var mapboxMap: MapboxMap
    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var mapboxNavigation: MapboxNavigation



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placePredictionViewModel.predictionLiveData.observe(this){
            placeAdapter.setPredictions(it)
            binding.viewAnimator.displayedChild = if (it.isEmpty()) 0 else 1
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentEnterDestinationBinding.inflate(inflater, container, false)
        return binding.root
    }
    private fun setUpMap() {
        mapboxMap = binding.mapView.getMapboxMap()
        binding.mapView.scalebar.enabled = false

        // initialize the location puck
        binding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        // initialize Mapbox Navigation
        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(requireContext())
                    .accessToken(getString(R.string.mapbox_access_token))
                    // comment out the location engine setting block to disable simulation
//                    .locationEngine(replayLocationEngine)
                    .build()
            )
        }

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        navigationCamera = NavigationCamera(
            mapboxMap,
            binding.mapView.camera,
            viewportDataSource
        )

        // load map style
        mapboxMap.loadStyleUri(
            Style.DARK
        ) {}

    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
    }


    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

            // if this is the first location update the activity has received,
            // it's best to immediately move the camera to the current user location
            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recentList = requireContext().getPref(KeyListPref.RECENT_LIST)
        setUpMap()
        setupUI()
        setupDataInRecyclerView()
        setupListener()
        initPlaceRecyclerView()
        binding.findRoute.setOnClickListener {
            if (sharedTripViewModel.trip.isPickupAndDestinationsLocationAvailable()) {
                navigateToHomeFragmentWithResult(HomeViewType.ROUTE)
                popFragment()
            }else{
                Toast.makeText(
                    requireContext(),
                    "Please select destination address",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun initPlaceRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.placeRecyclerView.layoutManager = layoutManager
        binding.placeRecyclerView.adapter = placeAdapter
        binding.placeRecyclerView.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
        )
        placeAdapter.onPlaceClickListener = { prediction ->
            isNewSession = true
            placePredictionViewModel.getPlaceDetailById(prediction,edtType){ MainPlace ->
                recentList.add(MainPlace)
                recentPlaceListAdapter.notifyItemInserted(recentList.size-1)
                sharedTripViewModel.trip.addPlace(MainPlace)
                if (sharedTripViewModel.trip.isPickupAndDestinationsLocationAvailable()) {
                    navigateToHomeFragmentWithResult(HomeViewType.ROUTE)
                    popFragment()
                } else {
                    if (edtType == PlaceType.PICKUP) {
                        setPickupValueToEditText(placeName = MainPlace.place_name)
                    } else {
                        setDestinationValueToEditText(placeName = MainPlace.place_name)
                    }
                    binding.viewAnimator.displayedChild =  0
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        navigateToHomeFragmentWithResult(HomeViewType.ENTER_DESTINATION)
        popFragment()
        return false
    }

    private fun navigateToHomeFragmentWithResult(homeViewType: HomeViewType){
        setFragmentResult(
            FragmentResultType.ENTER_DESTINATION_TO_HOME.toString(),
            Bundle().apply {
                putString("type", homeViewType.toString())
            }
        )
    }

    private fun setPickupValueToEditText(placeName: String?) {
        pickupListenerCallRequired = false
        binding.edtPickupAddress.setText(placeName)
        pickupListenerCallRequired = true
    }

    private fun setDestinationValueToEditText(placeName: String?) {
        destinationListenerCallRequired = false
        binding.edtDestinationAddress.setText(placeName)
        destinationListenerCallRequired = true
    }


    private fun setupListener() {
        textWatcherPickup = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(queryText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (pickupListenerCallRequired) {
                    edtType = PlaceType.PICKUP
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ placePredictionViewModel.searchPlace(queryText.toString(),isNewSession) }, 300)
                }
            }

            override fun afterTextChanged(p0: Editable) {

            }

        }
        textWatcherDestination = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(queryText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (destinationListenerCallRequired) {
                    // is only executed if the EditText was directly changed by the user
                    edtType = PlaceType.DESTINATION
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ placePredictionViewModel.searchPlace(queryText.toString(),isNewSession) }, 300)
                }

            }

            override fun afterTextChanged(p0: Editable?) {

            }

        }
        binding.edtPickupAddress.addTextChangedListener(textWatcherPickup)
        binding.edtDestinationAddress.addTextChangedListener(textWatcherDestination)

        binding.btnClose.setOnClickListener {
            navigateToHomeFragmentWithResult(HomeViewType.ENTER_DESTINATION)
            popFragment()
        }


        binding.inputlayoutPickup.setEndIconOnClickListener {
            setPickupValueToEditText("")
            sharedTripViewModel.trip.removePlaceByPlaceType(PlaceType.PICKUP)
        }
        binding.inputDestination.setEndIconOnClickListener {
            setDestinationValueToEditText("")
            sharedTripViewModel.trip.removePlaceByPlaceType(PlaceType.DESTINATION)
        }

    }


    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
        mapboxNavigation.unregisterLocationObserver(locationObserver)

    }

    private fun setupUI() {
        setPickupValueToEditText(sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.PICKUP)?.place_address)
        setDestinationValueToEditText(sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_name)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            EnterDestinationFragment()
    }

    private fun setupDataInRecyclerView() {

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        favoriteAdapter = FavoriteListAdapter(
            arrayListOf(),
            object : CustomOnClickListener<Favorite> {
                override fun onClick(item: Favorite, position: Int, view: View) {

                }

            })
        recentPlaceListAdapter = RecentPlaceListAdapter(recentList,
            object : CustomOnClickListener<MainPlace> {
                override fun onClick(MainPlace: MainPlace, position: Int, view: View) {
                    if (binding.edtPickupAddress.hasFocus()) {
                        MainPlace.place_type = PlaceType.PICKUP
                    } else {
                        MainPlace.place_type = PlaceType.DESTINATION
                    }
                    sharedTripViewModel.trip.addPlace(MainPlace)
                    if (sharedTripViewModel.trip.isPickupAndDestinationsLocationAvailable()) {
                        navigateToHomeFragmentWithResult(HomeViewType.ROUTE)
                        popFragment()
                    } else {
                        if (binding.edtPickupAddress.hasFocus()) {
                            setPickupValueToEditText(MainPlace.place_name)
                        } else {
                            setDestinationValueToEditText(MainPlace.place_name)
                        }
                    }
                }

            })
        adapter = ConcatAdapter(favoriteAdapter, recentPlaceListAdapter)
        binding.recyclerView.adapter = adapter
    }
}