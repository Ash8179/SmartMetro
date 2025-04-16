package com.example.metroinfo.ui.line

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentLineListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LineListFragment : Fragment() {

    private var _binding: FragmentLineListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LineListViewModel by viewModels()
    private lateinit var adapter: LineAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLineListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = LineAdapter { line ->
            findNavController().navigate(
                LineListFragmentDirections.actionLineListToLineDetail(line.lineId)
            )
        }
        binding.lineRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@LineListFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lines.collect { lines ->
                adapter.submitList(lines)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 