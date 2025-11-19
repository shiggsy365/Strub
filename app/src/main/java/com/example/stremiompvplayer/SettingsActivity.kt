package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.adapters.CatalogConfigAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivitySettingsBinding
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.data.ServiceLocator

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    // Reusing MainViewModel for simplicity, but could use a dedicated one
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(ServiceLocator.getInstance(applicationContext))
    }
    private lateinit var adapter: CatalogConfigAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUserSection()
        setupCatalogList()
    }

    private fun setupCatalogList() {
        adapter = CatalogConfigAdapter(
            onUpdate = { catalog -> viewModel.updateCatalogConfig(catalog) },
            onMoveUp = { catalog, pos -> 
                 val list = adapter.currentList
                 if (pos > 0) {
                     val prev = list[pos - 1]
                     viewModel.swapCatalogOrder(catalog, prev)
                 }
            },
            onMoveDown = { catalog, pos ->
                 val list = adapter.currentList
                 if (pos < list.size - 1) {
                     val next = list[pos + 1]
                     viewModel.swapCatalogOrder(catalog, next)
                 }
            }
        )
        
        binding.rvCatalogConfigs.layoutManager = LinearLayoutManager(this)
        binding.rvCatalogConfigs.adapter = adapter

        // Initialize defaults if empty (usually done on app start, but ensuring here)
        viewModel.initDefaultCatalogs()

        // Observe changes
        viewModel.allCatalogConfigs.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    private fun setupUserSection() {
        val userId = SharedPreferencesManager.getUserId(this)
        binding.tvCurrentUser.text = "Current User ID: $userId"

        binding.btnSwitchUser.setOnClickListener {
            startActivity(Intent(this, UserSelectionActivity::class.java))
        }

        binding.btnManageAddons.setOnClickListener {
            // Placeholder for existing addon logic
        }
    }
}
