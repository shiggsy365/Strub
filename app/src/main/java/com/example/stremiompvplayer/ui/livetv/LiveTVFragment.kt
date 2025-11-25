package com.example.stremiompvplayer.ui.livetv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.stremiompvplayer.adapters.TVGuideAdapter
import com.example.stremiompvplayer.adapters.UpcomingProgramAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentLivetvBinding
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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class LiveTVFragment : Fragment() {

    private var _binding: FragmentLivetvBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var tvGuideAdapter: TVGuideAdapter
    private lateinit var upcomingAdapter: UpcomingProgramAdapter
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var allChannels = listOf<Channel>()
    private var allPrograms = listOf<EPGProgram>()
    private var channelsWithPrograms = listOf<ChannelWithPrograms>()
    private var selectedChannel: ChannelWithPrograms? = null
    private var selectedGroup: String? = null

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
        _binding = FragmentLivetvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()

        // Only load if cache is empty (first time or after manual refresh)
        if (cachedChannelsWithPrograms.isEmpty()) {
            loadLiveTVData()
        } else {
            // Use cached data
            allChannels = cachedChannels
            allPrograms = cachedPrograms
            channelsWithPrograms = cachedChannelsWithPrograms
            setupGroupFilters()
            updateChannelList()
            if (channelsWithPrograms.isNotEmpty()) {
                selectedChannel = channelsWithPrograms[0]
                updateDetailsPane(channelsWithPrograms[0])
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
        // TV Guide Adapter - play channel on click
        tvGuideAdapter = TVGuideAdapter { channelWithPrograms ->
            selectedChannel = channelWithPrograms
            updateDetailsPane(channelWithPrograms)
            // Play channel directly on click
            playChannel(channelWithPrograms.channel)
        }

        binding.rvTVGuide.layoutManager = LinearLayoutManager(context)
        binding.rvTVGuide.adapter = tvGuideAdapter

        // Upcoming Programs Adapter
        upcomingAdapter = UpcomingProgramAdapter()
        binding.rvUpcoming.layoutManager = LinearLayoutManager(context)
        binding.rvUpcoming.adapter = upcomingAdapter
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
                val userId = prefsManager.getCurrentUserId()
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
                    setupGroupFilters()
                    updateChannelList()

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

    private fun setupGroupFilters() {
        // Use database ordering if available, otherwise fall back to alphabetical
        val groups = if (groupsOrdering.isNotEmpty()) {
            groupsOrdering.map { it.name }
        } else {
            allChannels.mapNotNull { it.group }.distinct().sorted()
        }

        binding.groupChips.removeAllViews()

        // Add "All" chip
        val allChip = Chip(requireContext()).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                selectedGroup = null
                updateChannelList()
            }
        }
        binding.groupChips.addView(allChip)

        // Add group chips in database order
        groups.forEach { group ->
            val chip = Chip(requireContext()).apply {
                text = group
                isCheckable = true
                setOnClickListener {
                    selectedGroup = group
                    updateChannelList()
                }
            }
            binding.groupChips.addView(chip)
        }
    }

    private fun updateChannelList() {
        val filteredChannels = if (selectedGroup != null) {
            channelsWithPrograms.filter { it.channel.group == selectedGroup }
        } else {
            channelsWithPrograms
        }

        tvGuideAdapter.updateChannels(filteredChannels)
    }

    private fun updateDetailsPane(channelWithPrograms: ChannelWithPrograms) {
        val channel = channelWithPrograms.channel

        // Channel Name
        binding.detailChannelName.text = channel.name

        // Channel Logo
        if (!channel.logo.isNullOrEmpty()) {
            Glide.with(this)
                .load(channel.logo)
                .placeholder(R.mipmap.ic_launcher_foreground)
                .error(R.mipmap.ic_launcher_foreground)
                .into(binding.detailChannelLogo)
            binding.detailChannelLogo.visibility = View.VISIBLE
        } else {
            binding.detailChannelLogo.visibility = View.GONE
        }

        // Background
        if (!channel.logo.isNullOrEmpty()) {
            Glide.with(this)
                .load(channel.logo)
                .into(binding.pageBackground)
        }

        // Current Program
        val currentProgram = channelWithPrograms.currentProgram
        if (currentProgram != null) {
            binding.detailCurrentTitle.text = currentProgram.title
            binding.detailCurrentTime.text = formatTimeRange(currentProgram.startTime, currentProgram.endTime)
            binding.detailCurrentDesc.text = currentProgram.description ?: "No description available"

            // Calculate progress
            val progress = calculateProgress(currentProgram.startTime, currentProgram.endTime)
            binding.detailProgressBar.progress = progress
            binding.detailProgressBar.visibility = View.VISIBLE
        } else {
            binding.detailCurrentTitle.text = "No program information"
            binding.detailCurrentTime.text = ""
            binding.detailCurrentDesc.text = ""
            binding.detailProgressBar.visibility = View.GONE
        }

        // Upcoming Programs
        upcomingAdapter.updatePrograms(channelWithPrograms.nextPrograms)
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

    fun focusSidebar(): Boolean {
        binding.root.post {
            val firstView = binding.rvTVGuide.layoutManager?.findViewByPosition(0)
            if (firstView != null && firstView.isFocusable) {
                firstView.requestFocus()
            } else if (binding.rvTVGuide.isFocusable) {
                binding.rvTVGuide.requestFocus()
            } else if (binding.chipGroupFilter.childCount > 0) {
                // Try to focus on the first chip in the filter group
                binding.chipGroupFilter.getChildAt(0)?.requestFocus()
            } else {
                // Try again after a delay if views aren't ready
                binding.root.postDelayed({
                    binding.rvTVGuide.layoutManager?.findViewByPosition(0)?.requestFocus()
                }, 100)
            }
        }
        return true  // Always return true as we've initiated focus attempt
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
