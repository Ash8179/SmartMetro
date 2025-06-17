package com.example.metroinfo.ui.nearby

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.R
import com.example.metroinfo.databinding.FragmentNearbyStationsBinding
import com.example.metroinfo.ui.adapter.NearbyStationsAdapter
import com.example.metroinfo.util.viewBinding
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NearbyStationsFragment : Fragment(R.layout.fragment_nearby_stations) {
    private val binding by viewBinding(FragmentNearbyStationsBinding::bind)
    private val viewModel: NearbyStationsViewModel by viewModels()
    
    private val stationsAdapter = NearbyStationsAdapter { _ ->
        // TODO: 处理站点点击事件
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupLocationUpdates()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = stationsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.updateLocation(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nearbyStations.collectLatest { stations ->
                    stationsAdapter.submitList(stations)
                }
            }
        }
    }
} 