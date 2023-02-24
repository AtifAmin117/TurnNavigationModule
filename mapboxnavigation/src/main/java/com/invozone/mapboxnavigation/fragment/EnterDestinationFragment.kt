package com.invozone.mapboxnavigation.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.invozone.mapboxnavigation.base.BaseFragment
import com.invozone.mapboxnavigation.databinding.FragmentEnterDestinationBinding
import com.invozone.mapboxnavigation.listener.CustomOnClickListener
import com.invozone.mapboxnavigation.model.*
import com.invozone.mapboxnavigation.storage.KeyListPref
import com.invozone.mapboxnavigation.storage.getPref
import com.invozone.mapboxnavigation.viewmodel.PlacePredictionViewModel

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recentList = requireContext().getPref(KeyListPref.RECENT_LIST)
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