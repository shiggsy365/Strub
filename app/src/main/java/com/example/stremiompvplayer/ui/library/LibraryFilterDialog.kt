package com.example.stremiompvplayer.ui.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.TMDBGenre
import com.example.stremiompvplayer.viewmodels.LibraryFilterConfig
import com.example.stremiompvplayer.viewmodels.SortType
import com.google.android.material.button.MaterialButton

/**
 * Dialog for library filtering and sorting options.
 */
class LibraryFilterDialog(
    private val context: Context,
    private val currentConfig: LibraryFilterConfig,
    private val movieGenres: List<TMDBGenre>,
    private val tvGenres: List<TMDBGenre>,
    private val onApply: (LibraryFilterConfig) -> Unit,
    private val onClear: () -> Unit
) {
    private var selectedSortType: SortType = currentConfig.sortBy
    private var sortAscending: Boolean = currentConfig.sortAscending
    private var selectedMovieGenreId: String? = currentConfig.movieGenreFilter
    private var selectedTVGenreId: String? = currentConfig.tvGenreFilter

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_library_filter, null)

        // Setup sort options
        setupSortOptions(dialogView)

        // Setup genre filters
        setupGenreFilters(dialogView)

        // Setup buttons
        val btnClear = dialogView.findViewById<MaterialButton>(R.id.btnClearFilters)
        val btnApply = dialogView.findViewById<MaterialButton>(R.id.btnApply)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnClear.setOnClickListener {
            dialog.dismiss()
            onClear()
        }

        btnApply.setOnClickListener {
            val newConfig = LibraryFilterConfig(
                sortBy = selectedSortType,
                sortAscending = sortAscending,
                movieGenreFilter = selectedMovieGenreId,
                tvGenreFilter = selectedTVGenreId
            )
            dialog.dismiss()
            onApply(newConfig)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupSortOptions(dialogView: View) {
        val radioAddedDate = dialogView.findViewById<RadioButton>(R.id.radioAddedDate)
        val radioRating = dialogView.findViewById<RadioButton>(R.id.radioRating)
        val radioReleaseDate = dialogView.findViewById<RadioButton>(R.id.radioReleaseDate)
        val radioTitle = dialogView.findViewById<RadioButton>(R.id.radioTitle)

        val indicatorAddedDate = dialogView.findViewById<TextView>(R.id.indicatorAddedDate)
        val indicatorRating = dialogView.findViewById<TextView>(R.id.indicatorRating)
        val indicatorReleaseDate = dialogView.findViewById<TextView>(R.id.indicatorReleaseDate)
        val indicatorTitle = dialogView.findViewById<TextView>(R.id.indicatorTitle)

        // Set initial state
        when (selectedSortType) {
            SortType.ADDED_DATE -> {
                radioAddedDate.isChecked = true
                indicatorAddedDate.text = if (sortAscending) "▲" else "▼"
                indicatorAddedDate.visibility = View.VISIBLE
            }
            SortType.RATING -> {
                radioRating.isChecked = true
                indicatorRating.text = if (sortAscending) "▲" else "▼"
                indicatorRating.visibility = View.VISIBLE
            }
            SortType.RELEASE_DATE -> {
                radioReleaseDate.isChecked = true
                indicatorReleaseDate.text = if (sortAscending) "▲" else "▼"
                indicatorReleaseDate.visibility = View.VISIBLE
            }
            SortType.TITLE -> {
                radioTitle.isChecked = true
                indicatorTitle.text = if (sortAscending) "▲" else "▼"
                indicatorTitle.visibility = View.VISIBLE
            }
        }

        // Helper to update indicators
        fun updateIndicators(selectedType: SortType) {
            indicatorAddedDate.visibility = if (selectedType == SortType.ADDED_DATE) View.VISIBLE else View.INVISIBLE
            indicatorRating.visibility = if (selectedType == SortType.RATING) View.VISIBLE else View.INVISIBLE
            indicatorReleaseDate.visibility = if (selectedType == SortType.RELEASE_DATE) View.VISIBLE else View.INVISIBLE
            indicatorTitle.visibility = if (selectedType == SortType.TITLE) View.VISIBLE else View.INVISIBLE

            when (selectedType) {
                SortType.ADDED_DATE -> indicatorAddedDate.text = if (sortAscending) "▲" else "▼"
                SortType.RATING -> indicatorRating.text = if (sortAscending) "▲" else "▼"
                SortType.RELEASE_DATE -> indicatorReleaseDate.text = if (sortAscending) "▲" else "▼"
                SortType.TITLE -> indicatorTitle.text = if (sortAscending) "▲" else "▼"
            }
        }

        // Helper to handle click/check
        fun handleSortSelection(type: SortType) {
            if (selectedSortType == type) {
                // Toggle direction if already selected
                sortAscending = !sortAscending
            } else {
                // Set new sort type with default direction
                selectedSortType = type
                sortAscending = when (type) {
                    SortType.TITLE -> true  // A-Z by default
                    SortType.RATING -> false  // Highest first by default
                    else -> false  // Newest first by default
                }
            }
            updateIndicators(type)
        }

        radioAddedDate.setOnClickListener {
            handleSortSelection(SortType.ADDED_DATE)
        }

        radioRating.setOnClickListener {
            handleSortSelection(SortType.RATING)
        }

        radioReleaseDate.setOnClickListener {
            handleSortSelection(SortType.RELEASE_DATE)
        }

        radioTitle.setOnClickListener {
            handleSortSelection(SortType.TITLE)
        }
    }

    private fun setupGenreFilters(dialogView: View) {
        val spinnerMovieGenre = dialogView.findViewById<Spinner>(R.id.spinnerMovieGenre)
        val spinnerTVGenre = dialogView.findViewById<Spinner>(R.id.spinnerTVGenre)

        // Setup Movie Genre Spinner
        val movieGenreNames = mutableListOf("All Movie Genres")
        val movieGenreIds = mutableListOf<String?>(null)

        movieGenres.forEach { genre ->
            movieGenreNames.add(genre.name)
            movieGenreIds.add(genre.id.toString())
        }

        val movieAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, movieGenreNames)
        movieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMovieGenre.adapter = movieAdapter

        // Set current movie genre selection
        val currentMovieIndex = if (selectedMovieGenreId == null) {
            0
        } else {
            movieGenreIds.indexOf(selectedMovieGenreId).coerceAtLeast(0)
        }
        spinnerMovieGenre.setSelection(currentMovieIndex)

        spinnerMovieGenre.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMovieGenreId = movieGenreIds[position]
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedMovieGenreId = null
            }
        }

        // Setup TV Genre Spinner
        val tvGenreNames = mutableListOf("All TV Genres")
        val tvGenreIds = mutableListOf<String?>(null)

        tvGenres.forEach { genre ->
            tvGenreNames.add(genre.name)
            tvGenreIds.add(genre.id.toString())
        }

        val tvAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, tvGenreNames)
        tvAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTVGenre.adapter = tvAdapter

        // Set current TV genre selection
        val currentTVIndex = if (selectedTVGenreId == null) {
            0
        } else {
            tvGenreIds.indexOf(selectedTVGenreId).coerceAtLeast(0)
        }
        spinnerTVGenre.setSelection(currentTVIndex)

        spinnerTVGenre.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTVGenreId = tvGenreIds[position]
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedTVGenreId = null
            }
        }
    }
}
