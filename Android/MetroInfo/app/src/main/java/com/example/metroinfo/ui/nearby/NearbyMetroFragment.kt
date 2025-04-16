package com.example.metroinfo.ui.nearby

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentNearbyMetroBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.metroinfo.data.model.NearbyStation

@AndroidEntryPoint
class NearbyMetroFragment : Fragment() {
    private var _binding: FragmentNearbyMetroBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NearbyMetroViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val adapter = NearbyMetroAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyMetroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.nearbyMetroList.layoutManager = LinearLayoutManager(context)
        binding.nearbyMetroList.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        lifecycleScope.launch {
            (viewModel.isLoading as Flow<Boolean>).collect { isLoading ->
                binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            (viewModel.nearbyStations as Flow<List<NearbyStation>>).collect { stations ->
                adapter.submitList(stations)
            }
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        viewModel.loadNearbyStations(it)
                    }
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
} 