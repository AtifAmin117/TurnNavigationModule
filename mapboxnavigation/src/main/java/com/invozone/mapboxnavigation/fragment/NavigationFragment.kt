package com.invozone.mapboxnavigation.fragment

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
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
import com.invozone.mapboxnavigation.databinding.FragmentNavigationBinding
import com.invozone.mapboxnavigation.extension.clickWithDebounce
import com.invozone.mapboxnavigation.extension.getCompleteAddressString
import com.invozone.mapboxnavigation.listener.CustomOnClickListener
import com.invozone.mapboxnavigation.model.*
import com.invozone.mapboxnavigation.storage.KeyListPref
import com.invozone.mapboxnavigation.storage.getPref
import com.invozone.mapboxnavigation.utils.LocationPermissionHelper
import com.invozone.mapboxnavigation.viewmodel.PlacePredictionViewModel
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt


class NavigationFragment : BaseFragment() {
    private lateinit var currLatLong: LatLng
    private lateinit var currLocationAddress: String
    private var speedKmh: Int = 0
    lateinit var coroutineScope: CoroutineScope
    private val KILO_MILES_FACTOR = 0.621371
    private var pickupListenerCallRequired = true
    private var destinationListenerCallRequired = true
    private lateinit var favoriteAdapter: FavoriteListAdapter
    private lateinit var recentPlaceListAdapter: RecentPlaceListAdapter
    private lateinit var adapter: ConcatAdapter
    private lateinit var textWatcherPickup: TextWatcher
    private lateinit var textWatcherDestination: TextWatcher
    private val placeAdapter = PlacePredictionAdapter()
    private var edtType: PlaceType = PlaceType.PICKUP
    private val placePredictionViewModel by viewModels<PlacePredictionViewModel>()
    private var isNewSession = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var recentList: ArrayList<MainPlace>

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    /**
     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
     */
    private val mapboxReplayer = MapboxReplayer()

    /**
     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
     */
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

    /**
     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
     */
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    /**
     * Bindings to the example layout.
     */

    /**
     * Mapbox Maps entry point obtained from the [MapView].
     * You need to get a new reference to this object whenever the [MapView] is recreated.
     */
    private lateinit var mapboxMap: MapboxMap

    /**
     * Mapbox Navigation entry point. There should only be one instance of this object for the app.
     * You can use [MapboxNavigationProvider] to help create and obtain that instance.
     */
    private lateinit var mapboxNavigation: MapboxNavigation

    /**
     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
     */
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /*
     * Below are generated camera padding values to ensure that the route fits well on screen while
     * other elements are overlaid on top of the map (including instruction view, buttons, etc.)
     */
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val speedLimitFormatter: SpeedLimitFormatter by lazy {
        SpeedLimitFormatter(requireContext())
    }

    private val speedLimitApi: MapboxSpeedLimitApi by lazy {
        MapboxSpeedLimitApi(speedLimitFormatter)
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    /**
     * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
     * and remaining distance to the maneuver point.
     */
    private lateinit var maneuverApi: MapboxManeuverApi

    /**
     * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
     */
    private lateinit var tripProgressApi: MapboxTripProgressApi

    /**
     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
     */
    private lateinit var routeLineApi: MapboxRouteLineApi

    /**
     * Draws route lines on the map based on the data from the [routeLineApi]
     */
    private lateinit var routeLineView: MapboxRouteLineView

    /**
     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /**
     * Draws maneuver arrows on the map based on the data [routeArrowApi].
     */
    private lateinit var routeArrowView: MapboxRouteArrowView

    /**
     * Stores and updates the state of whether the voice instructions should be played as they come or muted.
     */
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                binding.soundButton.muteAndExtend(BUTTON_ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                binding.soundButton.unmuteAndExtend(BUTTON_ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

    /**
     * Extracts message that should be communicated to the driver about the upcoming maneuver.
     * When possible, downloads a synthesized audio file that can be played back to the driver.
     */
    private lateinit var speechApi: MapboxSpeechApi


    /**
     * Plays the synthesized audio files with upcoming maneuver instructions
     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
     */
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    /**
     * Observes when a new voice instruction should be played.
     */
    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    /**
     * Based on whether the synthesized audio file is available, the callback plays the file
     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    // play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    // play the sound file from the external generator
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    /**
     * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
     */
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            // remove already consumed file to free-up space
            speechApi.clean(value)
        }

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * Gets notified with location updates.
     *
     * Exposes raw updates coming directly from the location services
     * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
     */
    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(location: Location) {
            val speed: Float = location.speed
            speedKmh = (speed * 3600 / 1000).toInt()
            Log.d("NavigationFragment", "Current speed: $speedKmh km/h")
            binding.normalSpeedTv.text = speedKmh.toString()
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            currLatLong = LatLng(+enhancedLocation.latitude, +enhancedLocation.longitude)
            currLocationAddress = currLatLong.getCompleteAddressString(requireContext())
            Log.d("NavigationFragment", "onNewLocationMatcherResult : " + currLocationAddress)
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

    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // update the camera position to account for the progressed fragment of the route
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        // draw the upcoming maneuver arrow on the map
        val style = mapboxMap.getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        // update top banner with maneuver instructions
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Toast.makeText(
                    requireContext(),
                    error.errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
            },
            {
//                binding.maneuverView.visibility = View.VISIBLE
                binding.maneuverView.renderManeuvers(maneuvers)
            }
        )

        // update bottom trip progress summary
        binding.tripProgressView.render(
            tripProgressApi.getTripProgress(routeProgress)
        )
    }

    /**
     * Gets notified whenever the tracked routes change.
     *
     * A change can mean:
     * - routes get changed with [MapboxNavigation.setRoutes]
     * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
     * - driver got off route and a reroute was executed
     */
    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.routes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            val routeLines = routeUpdateResult.routes.map { RouteLine(it, null) }

            routeLineApi.setRoutes(
                routeLines
            ) { value ->
                mapboxMap.getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(routeUpdateResult.routes.first())
            viewportDataSource.evaluate()
        } else {
            // remove the route line and route arrow from the map
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            // remove the route reference from camera position evaluations
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    private var isRouteFetched = false


    private lateinit var binding: FragmentNavigationBinding
    private var currHomeViewType = HomeViewType.ENTER_DESTINATION
    private val sharedTripViewModel by activityViewModels<SharedTripViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placePredictionViewModel.predictionLiveData.observe(this) {
            placeAdapter.setPredictions(it)
            binding.animateView.displayedChild = if (it.isEmpty()) 0 else 1
        }
//        handleUIOnBackStackCall()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationPermissionHelper = LocationPermissionHelper(WeakReference((requireActivity())))
        locationPermissionHelper.checkPermissions {
            recentList = requireContext().getPref(KeyListPref.RECENT_LIST)
            setUpMap()
//            setupUI()
            setupDataInRecyclerView()
            setupListener()
            initPlaceRecyclerView()
        }
        binding.txtEnterDestination.clickWithDebounce {
            navigateToEnterDestinationFragment()
        }
        binding.txtPin.setOnClickListener {
            if (binding.edtPickupAddress.hasFocus()) {
                sharedTripViewModel.trip.addPlace(
                    PlaceType.PICKUP,
                    currLatLong,
                    currLocationAddress
                )
                setPickupValueToEditText(currLocationAddress)
                if (sharedTripViewModel.trip.isPickupAndDestinationsLocationAvailable()) {
                    binding.viewAnimator.visibility = View.GONE
                    binding.txtPin.visibility = View.GONE
                    findRoute(
                        Point.fromLngLat(
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.longitude,
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.latitude
                        )
                    )
                }
            } else {
                sharedTripViewModel.trip.addPlace(
                    PlaceType.DESTINATION,
                    currLatLong,
                    currLocationAddress
                )
                setDestinationValueToEditText(currLocationAddress)
                if (sharedTripViewModel.trip.isPickupAndDestinationsLocationAvailable()) {
                    binding.viewAnimator.visibility = View.GONE
                    binding.txtPin.visibility = View.GONE
                    findRoute(
                        Point.fromLngLat(
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.longitude,
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.latitude
                        )
                    )
                }
            }
        }
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
                        binding.viewAnimator.visibility = View.GONE
                        binding.txtPin.visibility = View.GONE
                        findRoute(
                            Point.fromLngLat(
                                sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.longitude,
                                sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.latitude
                            )
                        )
                    } else {
                        if (binding.edtPickupAddress.hasFocus()) {
                            setPickupValueToEditText(MainPlace.place_address)
                        } else {
                            setDestinationValueToEditText(MainPlace.place_address)
                        }
                    }
                }

            })
        adapter = ConcatAdapter(favoriteAdapter, recentPlaceListAdapter)
        binding.recyclerView.adapter = adapter
    }

    private fun setupListener() {
        textWatcherPickup = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(queryText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (pickupListenerCallRequired) {
                    edtType = PlaceType.PICKUP
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({
                        placePredictionViewModel.searchPlace(
                            queryText.toString(),
                            isNewSession
                        )
                    }, 300)
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
                    handler.postDelayed({
                        placePredictionViewModel.searchPlace(
                            queryText.toString(),
                            isNewSession
                        )
                    }, 300)
                }

            }

            override fun afterTextChanged(p0: Editable?) {

            }

        }
        binding.edtPickupAddress.addTextChangedListener(textWatcherPickup)
        binding.edtDestinationAddress.addTextChangedListener(textWatcherDestination)

        binding.btnClose.setOnClickListener {
//            navigateToHomeFragmentWithResult(HomeViewType.ENTER_DESTINATION)
//            popFragment()
        }


        binding.inputlayoutPickup.setEndIconOnClickListener {
            setPickupValueToEditText("")
            sharedTripViewModel.trip.removePlaceByPlaceType(PlaceType.PICKUP)
            removeRouteView()
        }
        binding.inputDestination.setEndIconOnClickListener {
            setDestinationValueToEditText("")
            sharedTripViewModel.trip.removePlaceByPlaceType(PlaceType.DESTINATION)
            removeRouteView()
        }

    }

    private fun removeRouteView() {
        mapboxNavigation.setRoutes(listOf())
        mapboxReplayer.stop()
        binding.routeInfo.visibility = View.GONE
        binding.viewAnimator.visibility = View.VISIBLE
        binding.txtPin.visibility = View.VISIBLE
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
            placePredictionViewModel.getPlaceDetailById(prediction, edtType) { MainPlace ->
                recentList.add(MainPlace)
                recentPlaceListAdapter.notifyItemInserted(recentList.size - 1)
                if (binding.edtPickupAddress.hasFocus()) {
                    MainPlace.place_type = PlaceType.PICKUP
                } else {
                    MainPlace.place_type = PlaceType.DESTINATION
                }
                sharedTripViewModel.trip.addPlace(MainPlace)
                if (sharedTripViewModel.trip.isPickupAndDestinationsLocationAvailable()) {
                    binding.viewAnimator.visibility = View.GONE
                    binding.txtPin.visibility = View.GONE
                    binding.edtDestinationAddress.setText(
                        sharedTripViewModel.trip.getPlaceByPlaceType(
                            PlaceType.DESTINATION
                        )?.place_name
                    )
                    findRoute(
                        Point.fromLngLat(
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.longitude,
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.latitude
                        )
                    )
                } else {
                    if (edtType == PlaceType.PICKUP) {
                        setPickupValueToEditText(placeName = MainPlace.place_address)
                    } else {
                        setDestinationValueToEditText(placeName = MainPlace.place_address)
                    }
                    binding.animateView.displayedChild = 0
                }
            }
        }
    }

    private fun handleUIOnBackStackCall() {
        setFragmentResultListener(FragmentResultType.ENTER_DESTINATION_TO_HOME.toString()) { _, bundle ->
            when (bundle.get("type")) {
                HomeViewType.ROUTE.toString() -> {
                    binding.bottomView.visibility = View.GONE
                    handleHomeFragmentUI(HomeViewType.ROUTE)
                    binding.edtDestinationAddress.setText(
                        sharedTripViewModel.trip.getPlaceByPlaceType(
                            PlaceType.DESTINATION
                        )?.place_name
                    )
                    findRoute(
                        Point.fromLngLat(
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.longitude,
                            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_latLong!!.latitude
                        )
                    )
                }
                HomeViewType.ENTER_DESTINATION.toString() -> {
                    binding.bottomView.visibility = View.VISIBLE
                    clearRouteAndStopNavigation()
                    handleHomeFragmentUI(HomeViewType.ENTER_DESTINATION)
                }
            }

        }
        setFragmentResultListener(FragmentResultType.PLACE_PICKER_TO_HOME.toString()) { _, _ ->
//            tripPlaceListAdapter.setData(sharedTripViewModel.trip.getTripData())
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun handleHomeFragmentUI(homeViewType: HomeViewType) {
        currHomeViewType = homeViewType
        when (homeViewType) {
            HomeViewType.ROUTE -> {
                binding.bottomView.visibility = View.GONE
            }
            HomeViewType.ENTER_DESTINATION -> {
                binding.bottomView.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNavigationBinding.inflate(inflater, container, false)
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
        // set the animations lifecycle listener to ensure the NavigationCamera stops
        // automatically following the user location when the map is interacted with
        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        binding.mapView.logo.updateSettings {
            enabled = false
        }

        binding.mapView.attribution.updateSettings {
            enabled = false
        }
        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            // shows/hide the recenter button depending on the camera state
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> binding.recenter.visibility = View.INVISIBLE
                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> binding.recenter.visibility = View.VISIBLE
            }
        }
        // set the padding values depending on screen orientation and visible view layout
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(requireContext())
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(requireContext())
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(requireContext(), TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        // initialize voice instructions api and the voice instruction player
        speechApi = MapboxSpeechApi(
            requireContext(),
            getString(R.string.mapbox_access_token),
            Locale.US.language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            requireContext(),
            getString(R.string.mapbox_access_token),
            Locale.US.language
        )

        // initialize route line, the withRouteLineBelowLayerId is specified to place
        // the route line below road labels layer on the map
        // the value of this option will depend on the style that you are using
        // and under which layer the route line should be placed on the map layers stack
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(requireContext())
            .withRouteLineBelowLayerId("road-label")
            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions = RouteArrowOptions.Builder(requireContext()).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        // load map style
        mapboxMap.loadStyleUri(
            Style.DARK
        ) {
//            findRoute(Point.fromLngLat(ConstantsUtils.locationDestination.placeLongitude, ConstantsUtils.locationDestination.placeLatitude))
        }

        // initialize view interactions
        binding.stop.setOnClickListener {
            clearRouteAndStopNavigation()
        }

        binding.startNavigation.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            binding.routeOverview.showTextAndExtend(BUTTON_ANIMATION_DURATION)
            if (isRouteFetched) {
                isRouteFetched = false
                binding.speedLimitView.visibility = View.VISIBLE
                binding.maneuverView.visibility = View.VISIBLE
                binding.tripProgressCard.visibility = View.VISIBLE
                binding.soundButton.visibility = View.VISIBLE
                binding.setAddressLy.visibility = View.GONE
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }
        binding.recenter.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            binding.routeOverview.showTextAndExtend(BUTTON_ANIMATION_DURATION)
            if (isRouteFetched) {
                isRouteFetched = false
                binding.speedLimitView.visibility = View.VISIBLE
                binding.maneuverView.visibility = View.VISIBLE
                binding.tripProgressCard.visibility = View.VISIBLE
                binding.soundButton.visibility = View.VISIBLE
                binding.setAddressLy.visibility = View.GONE
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }
        binding.routeOverview.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenter.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }
        binding.altRoute.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenter.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }
        binding.soundButton.setOnClickListener {
            // mute/unmute voice instructions
            isVoiceInstructionsMuted = !isVoiceInstructionsMuted
        }

        // set initial sounds button state

        // start the trip session to being receiving location updates in free drive
        // and later when a route is set also receiving route progress updates
        binding.soundButton.unmute()
        mapboxNavigation.startTripSession()
    }


    private fun navigateToEnterDestinationFragment() {
        if (currLocationAddress.isNotEmpty()) {
            binding.bottomView.visibility = View.GONE
            binding.routeInfo.visibility = View.GONE
            binding.setAddressLy.visibility = View.VISIBLE
            binding.viewAnimator.visibility = View.VISIBLE
            binding.edtPickupAddress.isEnabled = true
            binding.edtDestinationAddress.isEnabled = true
            sharedTripViewModel.trip.addPlace(
                PlaceType.PICKUP,
                currLatLong,
                currLocationAddress
            )
            setPickupValueToEditText(currLocationAddress)
        } else {
            showToast("Please check your internet connection or gps provider")
        }
    }

    companion object {
        private const val BUTTON_ANIMATION_DURATION = 1500L

        @JvmStatic
        fun newInstance() =
            NavigationFragment().apply {
            }
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


    override fun onStart() {
        super.onStart()

        // register event listeners
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        MapboxNavigationProvider.destroy()
        mapboxReplayer.finish()
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    private fun findRoute(destination: Point) {
        showProgressDialog(true)
        voiceInstructionsPlayer.volume(SpeechVolume(0f))
        val currentLocation = navigationLocationProvider.lastLocation
        val bearing = currentLocation?.bearing?.toDouble() ?: run {
            45.0
        }

        val originPoint = Point.fromLngLat(
            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.PICKUP)?.place_latLong!!.longitude,
            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.PICKUP)?.place_latLong!!.latitude
        )
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(requireContext())
                .coordinatesList(listOf(originPoint, destination))
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(bearing)
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .build(),
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    if (routes.isNotEmpty()) {
                        setRouteAndStartNavigation(routes)
                    } else {
                        showProgressDialog(false)
                        showToast("Route not found, Please try another one")
                    }
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    showProgressDialog(false)
                    showToast("Route not found, Please try another one")
                    binding.setAddressLy.visibility = View.VISIBLE
                    binding.viewAnimator.visibility = View.VISIBLE
                    binding.txtPin.visibility = View.VISIBLE
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    showProgressDialog(false)
                    showToast("Route not found, Please try another one")
                    binding.setAddressLy.visibility = View.VISIBLE
                    binding.viewAnimator.visibility = View.VISIBLE
                    binding.txtPin.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun setRouteAndStartNavigation(routes: List<DirectionsRoute>) {
        // set routes, where the first route in the list is the primary route that
        // will be used for active guidance
        mapboxNavigation.setRoutes(routes)

        // start location simulation along the primary route
        startSimulation(routes.first())
        // show UI elements
        binding.routeInfo.visibility = View.VISIBLE
        val km = routes.first().distance() / 1000
        binding.routeDistance.text = km.toInt().toString() + "km"
        binding.routeTime.text = convertSecondToTime(routes.first().duration().toInt())
        binding.routeCityName.text =
            sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_name
        binding.edtPickupAddress.setText(sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.PICKUP)?.place_address)
        binding.edtDestinationAddress.setText(sharedTripViewModel.trip.getPlaceByPlaceType(PlaceType.DESTINATION)?.place_name)
//        binding.edtPickupAddress.isEnabled = false
//        binding.edtDestinationAddress.isEnabled = false

        // move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToOverview()
        isRouteFetched = true

        coroutineScope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect {
                val postedSpeedLimitUnit = it.speedLimit?.speedLimitUnit
                val postedSpeedLimit = it.speedLimit?.speedKmph
                if (postedSpeedLimit != null) {
                    if (postedSpeedLimitUnit == SpeedLimitUnit.KILOMETRES_PER_HOUR) {
                        checkOverSpeedScenario(postedSpeedLimit)
                    } else {
                        val speed = (
                                5 * (postedSpeedLimit * KILO_MILES_FACTOR / 5)
                                    .roundToInt()
                                ).toDouble()
                        val formattedSpeed = String.format("%.0f", speed)
                        checkOverSpeedScenario(formattedSpeed.toInt())
                    }
                }
            }
        }
        showProgressDialog(false)
    }

    private fun checkOverSpeedScenario(maxSpeedLimit: Int) {
        binding.overSpeedTv.text = maxSpeedLimit.toString()
        if (speedKmh > maxSpeedLimit) {
            binding.overSpeedRl.visibility = View.VISIBLE
            binding.normalSpeedTv.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.over_speed
                )
            )

        } else {
            binding.overSpeedRl.visibility = View.GONE
            binding.normalSpeedTv.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.normal_speed
                )
            )

        }
    }

    private fun convertSecondToTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val hoursStr = if (hours == 0) "" else "${hours}h"
        val minutesStr = if (minutes == 1) "1 m" else "${minutes}m"
        return "$hoursStr $minutesStr"
    }

    private fun clearRouteAndStopNavigation() {
        // clear
        mapboxNavigation.setRoutes(listOf())

        // stop simulation
        mapboxReplayer.stop()

        // hide UI elements
        binding.soundButton.visibility = View.INVISIBLE
        binding.maneuverView.visibility = View.INVISIBLE
//        binding.routeOverview.visibility = View.INVISIBLE
        binding.tripProgressCard.visibility = View.INVISIBLE
        binding.speedLimitView.visibility = View.GONE
        binding.bottomView.visibility = View.VISIBLE
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.run {
            stop()
            clearEvents()
            val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
            pushEvents(replayEvents)
            if (replayEvents.isNotEmpty()){
                seekTo(replayEvents.first())
            }
            play()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    override fun onDetach() {
        super.onDetach()
        coroutineScope.cancel()
    }
}
