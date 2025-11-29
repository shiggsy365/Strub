package com.example.stremiompvplayer.ui.livetv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.ChannelGroupAdapter
import com.example.stremiompvplayer.adapters.ChannelGroupRow
import com.example.stremiompvplayer.adapters.EPGProgramAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentLiveTvNewBinding
import com.example.stremiompvplayer.models.Channel
import com.example.stremiompvplayer.models.ChannelGroup
import com.example.stremiompvplayer.models.ChannelMapping
import com.example.stremiompvplayer.models.ChannelWithPrograms
import com.example.stremiompvplayer.models.EPGProgram
import com.example.stremiompvplayer.utils.EPGParser
import com.example.stremiompvplayer.utils.M3UParser
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class LiveTVFragment : Fragment() {

    private var _binding: FragmentLiveTvNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var channelGroupAdapter: ChannelGroupAdapter
    private lateinit var epgProgramAdapter: EPGProgramAdapter
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private var allChannels = listOf<Channel>()
    private var allPrograms = listOf<EPGProgram>()
    private var channelsWithPrograms = listOf<ChannelWithPrograms>()
    private var selectedChannel: ChannelWithPrograms? = null

    private var channelMappings = listOf<ChannelMapping>()
    private var groupsOrdering = listOf<ChannelGroup>()

    companion object {
        // Cache EPG data to avoid reloading on every tab switch
        private var cachedChannels = listOf<Channel>()
        private var cachedPrograms = listOf<EPGProgram>()
        private var cachedChannelsWithPrograms = listOf<ChannelWithPrograms>()
        private var lastLoadTime: Long = 0

        fun newInstance(): LiveTVFragment {
            return LiveTVFragment()
        }

        // Called from MainActivity on app startup or from settings to force refresh
        fun clearCache() {
            cachedChannels = emptyList()
            cachedPrograms = emptyList()
            cachedChannelsWithPrograms = emptyList()
            lastLoadTime = 0
        }

        // Check if cache has data
        fun isCachePopulated(): Boolean {
            return cachedChannelsWithPrograms.isNotEmpty()
        }

        // Load EPG in background at app startup
        suspend fun loadEPGInBackground(context: android.content.Context) {
            val prefsManager = SharedPreferencesManager.getInstance(context)
            val m3uUrl = prefsManager.getLiveTVM3UUrl()
            val epgUrl = prefsManager.getLiveTVEPGUrl()

            if (m3uUrl.isNullOrEmpty()) {
                android.util.Log.d("LiveTV", "No M3U URL configured, skipping EPG load")
                return
            }

            try {
                // Load M3U and EPG in background
                cachedChannels = withContext(Dispatchers.IO) {
                    M3UParser.parseM3U(m3uUrl)
                }

                if (!epgUrl.isNullOrEmpty()) {
                    cachedPrograms = withContext(Dispatchers.IO) {
                        EPGParser.parseEPG(epgUrl) { status ->
                            android.util.Log.d("LiveTV", "EPG parsing: $status")
                        }
                    }
                }

                // Build channels with programs
                val currentTime = System.currentTimeMillis()
                cachedChannelsWithPrograms = cachedChannels.map { channel ->
                    val channelPrograms = cachedPrograms.filter {
                        it.channelId == channel.id || it.channelId == channel.tvgId
                    }

                    val currentProgram = channelPrograms.find {
                        currentTime >= it.startTime && currentTime <= it.endTime
                    }

                    val upcomingPrograms = channelPrograms
                        .filter { it.startTime >= currentTime }
                        .sortedBy { it.startTime }
                        .take(5)

                    ChannelWithPrograms(channel, currentProgram, upcomingPrograms)
                }

                lastLoadTime = System.currentTimeMillis()
                android.util.Log.d("LiveTV", "EPG loaded in background: ${cachedChannels.size} channels")
            } catch (e: Exception) {
                android.util.Log.e("LiveTV", "Error loading EPG in background", e)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLiveTvNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        startAutoScroll()

        // Only load EPG data if cache is empty (first time or after manual refresh)
        if (cachedChannelsWithPrograms.isEmpty()) {
            loadLiveTVData()
        } else {
            // Use cached EPG data but apply user-specific mappings
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    allChannels = cachedChannels
                    allPrograms = cachedPrograms

                    // Load and apply database mappings (user-specific)
                    val prefsManager = SharedPreferencesManager.getInstance(requireContext())
                    val userId = prefsManager.getCurrentUserId() ?: ""
                    val tvGuideSource = getTVGuideSource()

                    channelMappings = withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(requireContext()).channelMappingDao()
                            .getMappingsForUser(userId, tvGuideSource)
                    }
                    groupsOrdering = withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(requireContext()).channelGroupDao()
                            .getVisibleGroupsForUser(userId, tvGuideSource)
                    }

                    // Apply mappings to cached channels
                    allChannels = applyChannelMappings(allChannels)

                    // Rebuild channels with programs using mapped channels
                    channelsWithPrograms = buildChannelsWithPrograms()

                    updateChannelGroupRows()
                    if (channelsWithPrograms.isNotEmpty()) {
                        selectedChannel = channelsWithPrograms[0]
                        updateDetailsPane(channelsWithPrograms[0])
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LiveTV", "Error applying mappings to cached data", e)
                    // Fallback to full load if mapping fails
                    loadLiveTVData()
                }
            }
        }
    }

    // Public method to manually refresh EPG data
    fun refreshEPG() {
        clearCache()
        loadLiveTVData()
        // Update last refresh time
        SharedPreferencesManager.getInstance(requireContext()).setLastTVRefreshTime(System.currentTimeMillis())
    }

    private fun setupAdapters() {
        // Channel Group Adapter - displays rows of channels by group
        channelGroupAdapter = ChannelGroupAdapter(
            onChannelClick = { channelWithPrograms ->
                selectedChannel = channelWithPrograms
                updateDetailsPane(channelWithPrograms)
                // Play channel directly on click
                playChannel(channelWithPrograms.channel)
            },
            onChannelFocused = { channelWithPrograms ->
                selectedChannel = channelWithPrograms
                updateDetailsPane(channelWithPrograms)
            }
        )

        binding.rvChannelGroups.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvChannelGroups.adapter = channelGroupAdapter
        binding.rvChannelGroups.setItemViewCacheSize(10)

        // EPG Program Adapter - displays TV Guide for selected channel
        epgProgramAdapter = EPGProgramAdapter()
        binding.rvEPGPrograms.layoutManager = LinearLayoutManager(context)
        binding.rvEPGPrograms.adapter = epgProgramAdapter
    }

    private fun loadLiveTVData() {
        val prefsManager = SharedPreferencesManager.getInstance(requireContext())
        val m3uUrl = prefsManager.getLiveTVM3UUrl()
        val epgUrl = prefsManager.getLiveTVEPGUrl()

        if (m3uUrl.isNullOrEmpty()) {
            Toast.makeText(context, "Please configure M3U URL in Settings", Toast.LENGTH_LONG).show()
            return
        }

        binding.loadingCard.visibility = View.VISIBLE
        binding.tvLoadingMessage.text = "Loading channels..."

        lifecycleScope.launch {
            try {
                // Load M3U
                binding.tvLoadingMessage.text = "Loading channels..."
                allChannels = withContext(Dispatchers.IO) {
                    M3UParser.parseM3U(m3uUrl)
                }

                if (allChannels.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No channels found in M3U", Toast.LENGTH_LONG).show()
                        binding.loadingCard.visibility = View.GONE
                    }
                    return@launch
                }

                // Load database mappings
                val userId = prefsManager.getCurrentUserId() ?: ""
                val tvGuideSource = getTVGuideSource()
                channelMappings = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).channelMappingDao()
                        .getMappingsForUser(userId, tvGuideSource)
                }
                groupsOrdering = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).channelGroupDao()
                        .getVisibleGroupsForUser(userId, tvGuideSource)
                }

                // Apply database mappings to channels
                allChannels = applyChannelMappings(allChannels)

                // Load EPG if configured
                if (!epgUrl.isNullOrEmpty()) {
                    allPrograms = withContext(Dispatchers.IO) {
                        EPGParser.parseEPG(epgUrl) { status ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.tvLoadingMessage.text = status
                            }
                        }
                    }
                }

                // Combine channels with programs
                channelsWithPrograms = buildChannelsWithPrograms()

                // Cache the loaded data
                cachedChannels = allChannels
                cachedPrograms = allPrograms
                cachedChannelsWithPrograms = channelsWithPrograms
                lastLoadTime = System.currentTimeMillis()

                // Update UI
                withContext(Dispatchers.Main) {
                    binding.loadingCard.visibility = View.GONE
                    updateChannelGroupRows()

                    if (channelsWithPrograms.isNotEmpty()) {
                        selectedChannel = channelsWithPrograms[0]
                        updateDetailsPane(channelsWithPrograms[0])
                    }

                    Toast.makeText(context, "Loaded ${allChannels.size} channels", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingCard.visibility = View.GONE
                    Toast.makeText(context, "Error loading Live TV: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applyChannelMappings(channels: List<Channel>): List<Channel> {
        if (channelMappings.isEmpty()) return channels

        return channels.mapNotNull { channel ->
            val mapping = channelMappings.find { it.channelId == channel.id }

            // Filter out hidden channels
            if (mapping != null && mapping.isHidden) {
                return@mapNotNull null
            }

            // Apply custom name, group, and EPG link if mapping exists
            if (mapping != null) {
                val customGroup = groupsOrdering.find { it.id == mapping.groupId }
                channel.copy(
                    name = mapping.customName ?: channel.name,
                    group = customGroup?.name ?: channel.group,
                    tvgId = mapping.tvgId ?: channel.tvgId
                )
            } else {
                // Filter out channels in hidden groups
                val groupIsHidden = channel.group?.let { channelGroup ->
                    groupsOrdering.none { it.name == channelGroup }
                } ?: false

                if (groupIsHidden) null else channel
            }
        }
    }

    private fun getTVGuideSource(): String {
        val prefsManager = SharedPreferencesManager.getInstance(requireContext())
        val m3uUrl = prefsManager.getLiveTVM3UUrl() ?: ""
        val epgUrl = prefsManager.getLiveTVEPGUrl() ?: ""
        return hashString("$m3uUrl|$epgUrl")
    }

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun buildChannelsWithPrograms(): List<ChannelWithPrograms> {
        val currentTime = System.currentTimeMillis()

        return allChannels.map { channel ->
            val channelPrograms = allPrograms.filter { program ->
                program.channelId == channel.tvgId || program.channelId == channel.id
            }

            val currentProgram = EPGParser.getCurrentProgram(channelPrograms, currentTime)
            val nextPrograms = EPGParser.getUpcomingPrograms(channelPrograms, currentTime, 5)

            ChannelWithPrograms(
                channel = channel,
                currentProgram = currentProgram,
                nextPrograms = nextPrograms
            )
        }
    }

    /**
     * Groups channels by their group name and creates rows for display.
     * Each row represents a channel group with horizontal scrolling channels.
     */
    private fun updateChannelGroupRows() {
        // Get group ordering from database or fall back to alphabetical
        val groupNames = if (groupsOrdering.isNotEmpty()) {
            groupsOrdering.map { it.name }
        } else {
            channelsWithPrograms.mapNotNull { it.channel.group }.distinct().sorted()
        }

        // Create rows for each group
        val rows = groupNames.mapNotNull { groupName ->
            val groupChannels = channelsWithPrograms.filter { it.channel.group == groupName }
            if (groupChannels.isNotEmpty()) {
                ChannelGroupRow(
                    id = groupName,
                    title = groupName,
                    channels = groupChannels
                )
            } else {
                null
            }
        }

        // Add "Uncategorized" row for channels without a group
        val uncategorizedChannels = channelsWithPrograms.filter { it.channel.group.isNullOrEmpty() }
        val allRows = if (uncategorizedChannels.isNotEmpty()) {
            rows + ChannelGroupRow(
                id = "uncategorized",
                title = "Uncategorized",
                channels = uncategorizedChannels
            )
        } else {
            rows
        }

        channelGroupAdapter.updateData(allRows)
    }

    private fun updateDetailsPane(channelWithPrograms: ChannelWithPrograms) {
        val channel = channelWithPrograms.channel

        // Channel Name (large, prominent)
        binding.detailChannelName.text = channel.name

        // Channel Logo with black background and centered logo
        if (!channel.logo.isNullOrEmpty()) {
            Glide.with(this)
                .load(channel.logo)
                .placeholder(R.drawable.ic_tv)
                .error(R.drawable.ic_tv)
                .centerInside()
                .into(binding.detailChannelLogo)
            binding.detailChannelLogo.visibility = View.VISIBLE
        } else {
            binding.detailChannelLogo.setImageResource(R.drawable.ic_tv)
            binding.detailChannelLogo.visibility = View.VISIBLE
        }

        // Background uses channel logo for hero effect
        if (!channel.logo.isNullOrEmpty()) {
            Glide.with(this)
                .load(channel.logo)
                .into(binding.pageBackground)
        }

        // Currently Airing Program
        val currentProgram = channelWithPrograms.currentProgram
        if (currentProgram != null) {
            binding.detailCurrentTitle.text = currentProgram.title
            binding.detailCurrentTime.text = formatTimeRange(currentProgram.startTime, currentProgram.endTime)
            binding.detailCurrentDesc.text = currentProgram.description ?: "No description available"

            // Calculate progress for current program
            val progress = calculateProgress(currentProgram.startTime, currentProgram.endTime)
            binding.detailProgressBar.progress = progress
            binding.detailProgressBar.visibility = View.VISIBLE
        } else {
            binding.detailCurrentTitle.text = "No program information"
            binding.detailCurrentTime.text = ""
            binding.detailCurrentDesc.text = ""
            binding.detailProgressBar.visibility = View.GONE
        }

        // Update EPG TV Guide (Right side panel)
        updateEPGGuide(channelWithPrograms)
    }

    /**
     * Updates the TV Guide panel with programs for the next 12-24 hours.
     */
    private fun updateEPGGuide(channelWithPrograms: ChannelWithPrograms) {
        val channel = channelWithPrograms.channel
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursLater = currentTime + (24 * 60 * 60 * 1000) // 24 hours in milliseconds

        // Get all programs for this channel within the next 24 hours
        val channelPrograms = allPrograms.filter { program ->
            program.channelId == channel.tvgId || program.channelId == channel.id
        }

        // Include currently airing program and upcoming programs
        val programsToShow = channelPrograms
            .filter { it.endTime >= currentTime && it.startTime <= twentyFourHoursLater }
            .sortedBy { it.startTime }

        if (programsToShow.isNotEmpty()) {
            epgProgramAdapter.updatePrograms(programsToShow)
            binding.rvEPGPrograms.visibility = View.VISIBLE
            binding.tvNoEPG.visibility = View.GONE

            // Auto-scroll to currently airing program
            val currentIndex = epgProgramAdapter.getCurrentlyAiringIndex()
            if (currentIndex >= 0) {
                binding.rvEPGPrograms.scrollToPosition(currentIndex)
            }
        } else {
            binding.rvEPGPrograms.visibility = View.GONE
            binding.tvNoEPG.visibility = View.VISIBLE
        }
    }

    private fun playChannel(channel: Channel) {
        try {
            // Create Stream object for ExoPlayer
            val stream = com.example.stremiompvplayer.models.Stream(
                name = channel.name,
                description = "Live TV Channel",
                infoHash = null,
                url = channel.url,
                ytId = null,
                behaviorHints = null,
                proxied = false,
                library = false,
                service = null,
                streamType = "live",
                resolution = null,
                size = null,
                seeders = null,
                addon = "Live TV",
                parsedFile = null
            )

            // Create MetaItem for player
            val metaItem = com.example.stremiompvplayer.models.MetaItem(
                id = "livetv:${channel.id}",
                type = "channel",
                name = channel.name,
                poster = channel.logo,
                background = null,
                description = channel.group ?: "Live TV Channel"
            )

            // Launch PlayerActivity with ExoPlayer
            val intent = Intent(requireContext(), com.example.stremiompvplayer.PlayerActivity::class.java).apply {
                putExtra("stream", stream)
                putExtra("meta", metaItem)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error playing channel: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun formatTimeRange(startTime: Long, endTime: Long): String {
        val start = timeFormat.format(Date(startTime))
        val end = timeFormat.format(Date(endTime))
        return "$start - $end"
    }

    private fun calculateProgress(startTime: Long, endTime: Long): Int {
        val currentTime = System.currentTimeMillis()
        if (currentTime < startTime) return 0
        if (currentTime > endTime) return 100

        val duration = endTime - startTime
        val elapsed = currentTime - startTime
        return ((elapsed.toFloat() / duration.toFloat()) * 100).toInt()
    }

    fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    // Consume UP/DOWN if RecyclerView or its children have focus
                    // This prevents event from bubbling to MainActivity after RecyclerView handles it
                    if (_binding != null && (binding.rvChannelGroups.hasFocus() || binding.rvChannelGroups.focusedChild != null)) {
                        return true  // Consume after RecyclerView processes
                    }
                    return false
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    // Allow left navigation to re-open main menu from channel list
                    return false  // Let the event propagate to open the main menu
                }
            }
        }
        return false
    }

    fun focusSidebar(): Boolean {
        binding.root.post {
            val firstView = binding.rvChannelGroups.layoutManager?.findViewByPosition(0)
            if (firstView != null && firstView.isFocusable) {
                firstView.requestFocus()
            } else if (binding.rvChannelGroups.isFocusable) {
                binding.rvChannelGroups.requestFocus()
            } else {
                // Try again after a delay if views aren't ready
                binding.root.postDelayed({
                    // Null check to prevent crash if fragment is destroyed
                    _binding?.rvChannelGroups?.layoutManager?.findViewByPosition(0)?.requestFocus()
                }, 100)
            }
        }
        return true  // Always return true as we've initiated focus attempt
    }

// In LiveTVFragment.kt

    private var autoScrollJob: Job? = null

    private fun startAutoScroll() {
        val recyclerView = binding.rvEPGPrograms// Replace with your actual ID
        autoScrollJob?.cancel()
        autoScrollJob = lifecycleScope.launch {
            while (isActive) {
                recyclerView.smoothScrollBy(8, 0) // Scroll right by 8 pixels
                delay(1000) // Every second (adjust for speed)
            }
        }
    }

    private fun stopAutoScroll() {
        autoScrollJob?.cancel()
    }




    override fun onDestroyView() {
        super.onDestroyView()
        // Remove any pending callbacks to prevent crashes
        _binding?.root?.handler?.removeCallbacksAndMessages(null)
        _binding = null
        stopAutoScroll()
    }
}
