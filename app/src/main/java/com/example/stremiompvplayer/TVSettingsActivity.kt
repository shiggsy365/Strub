package com.example.stremiompvplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivityTvSettingsBinding
import com.example.stremiompvplayer.databinding.ItemTvChannelBinding
import com.example.stremiompvplayer.databinding.ItemTvGroupBinding
import com.example.stremiompvplayer.models.Channel
import com.example.stremiompvplayer.models.ChannelGroup
import com.example.stremiompvplayer.models.ChannelMapping
import com.example.stremiompvplayer.utils.EPGParser
import com.example.stremiompvplayer.utils.M3UParser
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class TVSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTvSettingsBinding
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var database: AppDatabase

    private lateinit var groupsAdapter: GroupsAdapter
    private lateinit var channelsAdapter: ChannelsAdapter

    private var currentGroups = mutableListOf<ChannelGroup>()
    private var currentChannels = mutableListOf<ChannelWithMapping>()
    private var selectedGroupId: Long? = null

    // Cache for M3U channels data
    private var m3uChannelsCache = mapOf<String, Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager.getInstance(this)
        database = AppDatabase.getInstance(this)

        setupRecyclerViews()
        setupButtons()
        loadData()
    }

    private fun setupRecyclerViews() {
        // Groups RecyclerView
        groupsAdapter = GroupsAdapter(
            onGroupClick = { group -> onGroupClicked(group) },
            onGroupLongClick = { group -> showGroupOptions(group) },
            onMoveUp = { group -> moveGroup(group, -1) },
            onMoveDown = { group -> moveGroup(group, 1) }
        )
        binding.rvGroups.apply {
            layoutManager = LinearLayoutManager(this@TVSettingsActivity)
            adapter = groupsAdapter
        }

        // Channels RecyclerView
        channelsAdapter = ChannelsAdapter(
            onClick = { channelWithMapping -> showChannelOptions(channelWithMapping) }
        )
        binding.rvChannels.apply {
            layoutManager = LinearLayoutManager(this@TVSettingsActivity)
            adapter = channelsAdapter
        }
    }

    private fun setupButtons() {
        binding.btnCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val userId = prefsManager.getCurrentUserId() ?: return@launch
                val source = getTVGuideSource()

                // Check if data needs initialization
                val existingGroups = withContext(Dispatchers.IO) {
                    database.channelGroupDao().getGroupsForUser(userId, source)
                }

                if (existingGroups.isEmpty()) {
                    // Initialize from M3U
                    initializeFromM3U()
                } else {
                    // Load existing data
                    loadGroups()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private suspend fun initializeFromM3U() {
        try {
            val m3uUrl = prefsManager.getLiveTVM3UUrl()
            if (m3uUrl.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TVSettingsActivity, "No M3U URL configured", Toast.LENGTH_SHORT).show()
                }
                return
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@TVSettingsActivity, "Loading channels from M3U...", Toast.LENGTH_SHORT).show()
            }

            val userId = prefsManager.getCurrentUserId() ?: return
            val source = getTVGuideSource()

            // Parse M3U
            val channels = withContext(Dispatchers.IO) {
                M3UParser.parseM3U(m3uUrl)
            }

            m3uChannelsCache = channels.associateBy { it.id }

            // Group channels by group name
            val channelsByGroup = channels.groupBy { it.group ?: "Ungrouped" }

            withContext(Dispatchers.IO) {
                var displayOrder = 0

                channelsByGroup.forEach { (groupName, groupChannels) ->
                    // Create ChannelGroup
                    val channelGroup = ChannelGroup(
                        userId = userId,
                        name = groupName,
                        originalName = groupName,
                        displayOrder = displayOrder++,
                        isHidden = false,
                        tvGuideSource = source
                    )

                    val groupId = database.channelGroupDao().insert(channelGroup)

                    // Create ChannelMapping for each channel in this group
                    groupChannels.forEach { channel ->
                        val mapping = ChannelMapping(
                            userId = userId,
                            channelId = channel.id,
                            channelName = channel.name,
                            customName = null,
                            groupId = groupId,
                            tvgId = channel.tvgId,
                            isHidden = false,
                            tvGuideSource = source
                        )
                        database.channelMappingDao().insert(mapping)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@TVSettingsActivity, "Initialized ${channels.size} channels in ${channelsByGroup.size} groups", Toast.LENGTH_SHORT).show()
            }

            // Load the newly created data
            loadGroups()

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@TVSettingsActivity, "Error initializing from M3U: ${e.message}", Toast.LENGTH_LONG).show()
            }
            e.printStackTrace()
        }
    }

    private suspend fun loadGroups() {
        try {
            val userId = prefsManager.getCurrentUserId() ?: return
            val source = getTVGuideSource()

            val groups = withContext(Dispatchers.IO) {
                database.channelGroupDao().getGroupsForUser(userId, source)
            }

            // Load channel counts for each group
            val groupsWithCounts = withContext(Dispatchers.IO) {
                groups.map { group ->
                    val channelCount = database.channelMappingDao()
                        .getMappingsByGroup(userId, group.id, source).size
                    group to channelCount
                }
            }

            withContext(Dispatchers.Main) {
                currentGroups.clear()
                currentGroups.addAll(groups)
                groupsAdapter.submitList(groupsWithCounts)
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading groups: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun onGroupClicked(group: ChannelGroup) {
        selectedGroupId = group.id
        loadChannelsForGroup(group)
    }

    private fun loadChannelsForGroup(group: ChannelGroup) {
        lifecycleScope.launch {
            try {
                val userId = prefsManager.getCurrentUserId() ?: return@launch
                val source = getTVGuideSource()

                val mappings = withContext(Dispatchers.IO) {
                    database.channelMappingDao().getMappingsByGroup(userId, group.id, source)
                }

                // Load M3U data if not cached
                if (m3uChannelsCache.isEmpty()) {
                    val m3uUrl = prefsManager.getLiveTVM3UUrl()
                    if (!m3uUrl.isNullOrEmpty()) {
                        val channels = withContext(Dispatchers.IO) {
                            M3UParser.parseM3U(m3uUrl)
                        }
                        m3uChannelsCache = channels.associateBy { it.id }
                    }
                }

                val channelsWithMappings = mappings.mapNotNull { mapping ->
                    val channel = m3uChannelsCache[mapping.channelId]
                    if (channel != null) {
                        ChannelWithMapping(channel, mapping)
                    } else {
                        null
                    }
                }

                withContext(Dispatchers.Main) {
                    currentChannels.clear()
                    currentChannels.addAll(channelsWithMappings)
                    channelsAdapter.submitList(channelsWithMappings)
                    binding.tvChannelsTitle.text = "${group.name} (${channelsWithMappings.size} channels)"
                }

            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error loading channels: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showGroupOptions(group: ChannelGroup) {
        val options = if (group.isHidden) {
            arrayOf("Rename", "Unhide", "Delete")
        } else {
            arrayOf("Rename", "Hide", "Delete")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(group.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Rename" -> showRenameGroupDialog(group)
                    "Hide" -> toggleGroupVisibility(group, true)
                    "Unhide" -> toggleGroupVisibility(group, false)
                    "Delete" -> confirmDeleteGroup(group)
                }
            }
            .show()
    }

    private fun showRenameGroupDialog(group: ChannelGroup) {
        val input = TextInputEditText(this).apply {
            setText(group.name)
            selectAll()
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Rename Group")
            .setView(container)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameGroup(group, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameGroup(group: ChannelGroup, newName: String) {
        lifecycleScope.launch {
            try {
                val updatedGroup = group.copy(name = newName)
                withContext(Dispatchers.IO) {
                    database.channelGroupDao().update(updatedGroup)
                }
                loadGroups()
                Toast.makeText(this@TVSettingsActivity, "Group renamed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error renaming group: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun toggleGroupVisibility(group: ChannelGroup, hide: Boolean) {
        lifecycleScope.launch {
            try {
                val updatedGroup = group.copy(isHidden = hide)
                withContext(Dispatchers.IO) {
                    database.channelGroupDao().update(updatedGroup)
                }
                loadGroups()
                val action = if (hide) "hidden" else "shown"
                Toast.makeText(this@TVSettingsActivity, "Group $action", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error updating group: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun confirmDeleteGroup(group: ChannelGroup) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete '${group.name}'? This will also delete all channel mappings in this group.")
            .setPositiveButton("Delete") { _, _ ->
                deleteGroup(group)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGroup(group: ChannelGroup) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.channelMappingDao().deleteByGroup(group.id)
                    database.channelGroupDao().delete(group)
                }
                loadGroups()
                if (selectedGroupId == group.id) {
                    selectedGroupId = null
                    currentChannels.clear()
                    channelsAdapter.submitList(emptyList())
                    binding.tvChannelsTitle.text = "Select a group"
                }
                Toast.makeText(this@TVSettingsActivity, "Group deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error deleting group: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun moveGroup(group: ChannelGroup, direction: Int) {
        lifecycleScope.launch {
            try {
                val userId = prefsManager.getCurrentUserId() ?: return@launch
                val source = getTVGuideSource()

                withContext(Dispatchers.IO) {
                    val groups = database.channelGroupDao().getGroupsForUser(userId, source)
                    val currentIndex = groups.indexOfFirst { it.id == group.id }

                    if (currentIndex == -1) return@withContext

                    val newIndex = currentIndex + direction
                    if (newIndex < 0 || newIndex >= groups.size) return@withContext

                    // Swap displayOrder
                    val otherGroup = groups[newIndex]
                    val tempOrder = group.displayOrder

                    database.channelGroupDao().update(group.copy(displayOrder = otherGroup.displayOrder))
                    database.channelGroupDao().update(otherGroup.copy(displayOrder = tempOrder))
                }

                loadGroups()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error reordering groups: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showCreateGroupDialog() {
        val input = TextInputEditText(this).apply {
            hint = "Group name"
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Create New Group")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    createGroup(groupName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createGroup(groupName: String) {
        lifecycleScope.launch {
            try {
                val userId = prefsManager.getCurrentUserId() ?: return@launch
                val source = getTVGuideSource()

                withContext(Dispatchers.IO) {
                    val groups = database.channelGroupDao().getGroupsForUser(userId, source)
                    val maxOrder = groups.maxOfOrNull { it.displayOrder } ?: -1

                    val newGroup = ChannelGroup(
                        userId = userId,
                        name = groupName,
                        originalName = groupName,
                        displayOrder = maxOrder + 1,
                        isHidden = false,
                        tvGuideSource = source
                    )
                    database.channelGroupDao().insert(newGroup)
                }

                loadGroups()
                Toast.makeText(this@TVSettingsActivity, "Group created", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error creating group: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showChannelOptions(channelWithMapping: ChannelWithMapping) {
        val mapping = channelWithMapping.mapping
        val options = if (mapping.isHidden) {
            arrayOf("Rename", "Unhide", "Change Group", "Link TV Guide")
        } else {
            arrayOf("Rename", "Hide", "Change Group", "Link TV Guide")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(mapping.customName ?: mapping.channelName)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Rename" -> showRenameChannelDialog(channelWithMapping)
                    "Hide" -> toggleChannelVisibility(mapping, true)
                    "Unhide" -> toggleChannelVisibility(mapping, false)
                    "Change Group" -> showChangeGroupDialog(channelWithMapping)
                    "Link TV Guide" -> showLinkTVGuideDialog(channelWithMapping)
                }
            }
            .show()
    }

    private fun showRenameChannelDialog(channelWithMapping: ChannelWithMapping) {
        val mapping = channelWithMapping.mapping
        val input = TextInputEditText(this).apply {
            setText(mapping.customName ?: mapping.channelName)
            selectAll()
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Rename Channel")
            .setView(container)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameChannel(mapping, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameChannel(mapping: ChannelMapping, newName: String) {
        lifecycleScope.launch {
            try {
                val updatedMapping = mapping.copy(customName = newName)
                withContext(Dispatchers.IO) {
                    database.channelMappingDao().update(updatedMapping)
                }
                // Reload current group
                selectedGroupId?.let { groupId ->
                    val group = currentGroups.find { it.id == groupId }
                    group?.let { loadChannelsForGroup(it) }
                }
                Toast.makeText(this@TVSettingsActivity, "Channel renamed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error renaming channel: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun toggleChannelVisibility(mapping: ChannelMapping, hide: Boolean) {
        lifecycleScope.launch {
            try {
                val updatedMapping = mapping.copy(isHidden = hide)
                withContext(Dispatchers.IO) {
                    database.channelMappingDao().update(updatedMapping)
                }
                // Reload current group
                selectedGroupId?.let { groupId ->
                    val group = currentGroups.find { it.id == groupId }
                    group?.let { loadChannelsForGroup(it) }
                }
                val action = if (hide) "hidden" else "shown"
                Toast.makeText(this@TVSettingsActivity, "Channel $action", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error updating channel: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showChangeGroupDialog(channelWithMapping: ChannelWithMapping) {
        val groupNames = currentGroups.map { it.name }.toTypedArray()
        val currentGroupIndex = currentGroups.indexOfFirst { it.id == channelWithMapping.mapping.groupId }

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Group")
            .setSingleChoiceItems(groupNames, currentGroupIndex) { dialog, which ->
                val newGroup = currentGroups[which]
                changeChannelGroup(channelWithMapping.mapping, newGroup)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changeChannelGroup(mapping: ChannelMapping, newGroup: ChannelGroup) {
        lifecycleScope.launch {
            try {
                val updatedMapping = mapping.copy(groupId = newGroup.id)
                withContext(Dispatchers.IO) {
                    database.channelMappingDao().update(updatedMapping)
                }
                // Reload current group
                selectedGroupId?.let { groupId ->
                    val group = currentGroups.find { it.id == groupId }
                    group?.let { loadChannelsForGroup(it) }
                }
                Toast.makeText(this@TVSettingsActivity, "Channel moved to ${newGroup.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error changing group: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showLinkTVGuideDialog(channelWithMapping: ChannelWithMapping) {
        val epgUrl = prefsManager.getLiveTVEPGUrl()
        if (epgUrl.isNullOrEmpty()) {
            Toast.makeText(this, "No EPG URL configured", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Loading EPG channels...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val epgChannels = withContext(Dispatchers.IO) {
                    EPGParser.parseEPGChannelList(epgUrl)
                }

                if (epgChannels.isEmpty()) {
                    Toast.makeText(this@TVSettingsActivity, "No EPG channels found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val channelNames = epgChannels.map { "${it.displayName} (${it.id})" }.toTypedArray()
                val currentIndex = epgChannels.indexOfFirst { it.id == channelWithMapping.mapping.tvgId }

                MaterialAlertDialogBuilder(this@TVSettingsActivity)
                    .setTitle("Link TV Guide")
                    .setSingleChoiceItems(channelNames, currentIndex) { dialog, which ->
                        val selectedEpgChannel = epgChannels[which]
                        linkChannelToEPG(channelWithMapping.mapping, selectedEpgChannel.id)
                        dialog.dismiss()
                    }
                    .setNeutralButton("Clear Link") { dialog, _ ->
                        linkChannelToEPG(channelWithMapping.mapping, null)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error loading EPG channels: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun linkChannelToEPG(mapping: ChannelMapping, epgChannelId: String?) {
        lifecycleScope.launch {
            try {
                val updatedMapping = mapping.copy(tvgId = epgChannelId)
                withContext(Dispatchers.IO) {
                    database.channelMappingDao().update(updatedMapping)
                }
                // Reload current group
                selectedGroupId?.let { groupId ->
                    val group = currentGroups.find { it.id == groupId }
                    group?.let { loadChannelsForGroup(it) }
                }
                val message = if (epgChannelId != null) {
                    "TV Guide linked"
                } else {
                    "TV Guide link cleared"
                }
                Toast.makeText(this@TVSettingsActivity, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TVSettingsActivity, "Error linking TV guide: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun getTVGuideSource(): String {
        val m3uUrl = prefsManager.getLiveTVM3UUrl() ?: ""
        val epgUrl = prefsManager.getLiveTVEPGUrl() ?: ""
        val combined = "$m3uUrl|$epgUrl"

        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(combined.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            combined.hashCode().toString()
        }
    }

    // Data class to combine channel with mapping
    data class ChannelWithMapping(
        val channel: Channel,
        val mapping: ChannelMapping
    )

    // Groups Adapter
    inner class GroupsAdapter(
        private val onGroupClick: (ChannelGroup) -> Unit,
        private val onGroupLongClick: (ChannelGroup) -> Unit,
        private val onMoveUp: (ChannelGroup) -> Unit,
        private val onMoveDown: (ChannelGroup) -> Unit
    ) : RecyclerView.Adapter<GroupsAdapter.ViewHolder>() {

        private var items = listOf<Pair<ChannelGroup, Int>>()

        fun submitList(newItems: List<Pair<ChannelGroup, Int>>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemTvGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: ItemTvGroupBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Pair<ChannelGroup, Int>) {
                val (group, channelCount) = item

                binding.tvGroupName.text = group.name
                binding.tvGroupStatus.text = if (group.isHidden) {
                    "$channelCount channels (Hidden)"
                } else {
                    "$channelCount channels"
                }

                binding.cardGroup.setOnClickListener { onGroupClick(group) }
                binding.cardGroup.setOnLongClickListener {
                    onGroupLongClick(group)
                    true
                }

                binding.btnMoveUp.setOnClickListener { onMoveUp(group) }
                binding.btnMoveDown.setOnClickListener { onMoveDown(group) }

                // Highlight selected group
                if (group.id == selectedGroupId) {
                    binding.cardGroup.strokeWidth = 4
                } else {
                    binding.cardGroup.strokeWidth = 1
                }
            }
        }
    }

    // Channels Adapter
    inner class ChannelsAdapter(
        private val onClick: (ChannelWithMapping) -> Unit
    ) : RecyclerView.Adapter<ChannelsAdapter.ViewHolder>() {

        private var items = listOf<ChannelWithMapping>()

        fun submitList(newItems: List<ChannelWithMapping>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemTvChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: ItemTvChannelBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(channelWithMapping: ChannelWithMapping) {
                val channel = channelWithMapping.channel
                val mapping = channelWithMapping.mapping

                val displayName = mapping.customName ?: mapping.channelName
                binding.tvChannelName.text = displayName

                // Build info text
                val infoParts = mutableListOf<String>()
                channel.group?.let { infoParts.add(it) }
                if (mapping.tvgId != null) {
                    infoParts.add("EPG: ${mapping.tvgId}")
                }
                if (mapping.isHidden) {
                    infoParts.add("Hidden")
                }
                binding.tvChannelInfo.text = infoParts.joinToString(" • ")

                // Load logo with Glide
                if (!channel.logo.isNullOrEmpty()) {
                    Glide.with(binding.ivChannelLogo.context)
                        .load(channel.logo)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(binding.ivChannelLogo)
                } else {
                    binding.ivChannelLogo.setImageResource(R.drawable.ic_launcher_foreground)
                }

                binding.cardChannel.setOnClickListener { onClick(channelWithMapping) }
            }
        }
    }
}
