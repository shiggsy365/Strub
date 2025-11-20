package com.example.stremiompvplayer.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSeriesBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LibraryAdapter(
            onClick = { item ->
                val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
                    putExtra("metaId", item.id)
                    putExtra("title", item.name)
                    putExtra("poster", item.poster)
                    putExtra("background", item.background)
                    putExtra("description", item.description)
                    putExtra("type", "series")
                }
                startActivity(intent)
            },
            onFocus = { item -> updateDetails(item) }
        )

        binding.rvContent.layoutManager = GridLayoutManager(context, 5)
        binding.rvContent.adapter = adapter

        viewModel.librarySeries.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            if (items.isNotEmpty()) {
                updateDetails(items[0])
            }
        }
    }

    private fun updateDetails(item: MetaItem) {
        binding.detailTitle.text = item.name
        binding.detailDescription.text = item.description ?: "No description available."
        Glide.with(this).load(item.background ?: item.poster).into(binding.backgroundImage)
    }

    fun handleBackPress(): Boolean = false

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class LibraryAdapter(
        private val onClick: (MetaItem) -> Unit,
        private val onFocus: (MetaItem) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {
        private var items = listOf<MetaItem>()
        inner class ViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val poster: android.widget.ImageView = view.findViewById(R.id.poster)
        }
        fun submitList(newItems: List<MetaItem>) {
            items = newItems
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poster, parent, false)
            return ViewHolder(view)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.poster.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            Glide.with(holder.view).load(item.poster).placeholder(R.drawable.movie).into(holder.poster)
            holder.view.setOnClickListener { onClick(item) }
            holder.view.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) onFocus(item) }
            holder.view.isFocusable = true
            holder.view.isFocusableInTouchMode = true
        }
    }
}