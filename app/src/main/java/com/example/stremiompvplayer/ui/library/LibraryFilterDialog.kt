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
    private val availableGenres: List<TMDBGenre>,
    private val onApply: (LibraryFilterConfig) -> Unit,
    private val onClear: () -> Unit
) {
    private var selectedSortType: SortType = currentConfig.sortBy
    private var sortAscending: Boolean = currentConfig.sortAscending
    private var selectedGenreId: String? = currentConfig.genreFilter

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_library_filter, null)

        // Setup sort options
        setupSortOptions(dialogView)

        // Setup genre filter
        setupGenreFilter(dialogView)

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
                genreFilter = selectedGenreId
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
        val radioReleaseDate = dialogView.findViewById<RadioButton>(R.id.radioReleaseDate)
        val radioTitle = dialogView.findViewById<RadioButton>(R.id.radioTitle)

        val indicatorAddedDate = dialogView.findViewById<TextView>(R.id.indicatorAddedDate)
        val indicatorReleaseDate = dialogView.findViewById<TextView>(R.id.indicatorReleaseDate)
        val indicatorTitle = dialogView.findViewById<TextView>(R.id.indicatorTitle)

        // Set initial state
        when (selectedSortType) {
            SortType.ADDED_DATE -> {
                radioAddedDate.isChecked = true
                indicatorAddedDate.text = if (sortAscending) "▲" else "▼"
                indicatorAddedDate.visibility = View.VISIBLE
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
            indicatorReleaseDate.visibility = if (selectedType == SortType.RELEASE_DATE) View.VISIBLE else View.INVISIBLE
            indicatorTitle.visibility = if (selectedType == SortType.TITLE) View.VISIBLE else View.INVISIBLE

            when (selectedType) {
                SortType.ADDED_DATE -> indicatorAddedDate.text = if (sortAscending) "▲" else "▼"
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
                    else -> false  // Newest first by default
                }
            }
            updateIndicators(type)
        }

        radioAddedDate.setOnClickListener {
            handleSortSelection(SortType.ADDED_DATE)
        }

        radioReleaseDate.setOnClickListener {
            handleSortSelection(SortType.RELEASE_DATE)
        }

        radioTitle.setOnClickListener {
            handleSortSelection(SortType.TITLE)
        }
    }

    private fun setupGenreFilter(dialogView: View) {
        val spinnerGenre = dialogView.findViewById<Spinner>(R.id.spinnerGenre)

        // Create genre list with "All Genres" option
        val genreNames = mutableListOf("All Genres")
        val genreIds = mutableListOf<String?>(null)

        availableGenres.forEach { genre ->
            genreNames.add(genre.name)
            genreIds.add(genre.id.toString())
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, genreNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGenre.adapter = adapter

        // Set current selection
        val currentIndex = if (selectedGenreId == null) {
            0
        } else {
            genreIds.indexOf(selectedGenreId).coerceAtLeast(0)
        }
        spinnerGenre.setSelection(currentIndex)

        // Listen for changes
        spinnerGenre.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedGenreId = genreIds[position]
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedGenreId = null
            }
        }
    }
}
