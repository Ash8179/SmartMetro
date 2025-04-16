package com.example.metroinfo.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.metroinfo.R
import com.example.metroinfo.databinding.FragmentMainBinding
import com.example.metroinfo.ui.crowding.CrowdingActivity

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nearbyMetroCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_nearbyStationsFragment)
        }

        binding.arrivalTimeCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_arrivalTimeFragment)
        }

        binding.routeCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_routeFragment)
        }

        binding.crowdingCard.setOnClickListener {
            startActivity(Intent(requireContext(), CrowdingActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 