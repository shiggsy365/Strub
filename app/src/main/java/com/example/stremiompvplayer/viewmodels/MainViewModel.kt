package com.example.stremiompvplayer.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
// NEW: Import for .map
import androidx.lifecycle.map
import com.example.stremiompvplayer.models.FeedList
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.network.StremioClient
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val stremioClient = StremioClient

    // NEW: User-specific properties
    private var currentUserAuthKey: String? = null
    // This will be set by setCurrentUser, e.g., "manifests_abc123xyz"
    private var userManifestsPrefsName: String = "manifests_default"

    val feedList = MutableLiveData<List<FeedList>>()
    val meta = MutableLiveData<Meta?>()
    val streams = MutableLiveData<List<Stream>>()

    // The "master list" of all catalogs from all user-added addons
    val discoverSections = MutableLiveData<List<FeedList>>()

    // NEW: Filtered lists for the Movie and Series tabs
    val movieSections = discoverSections.map { sections ->
        sections.filter { it.type == "movie" }.sortedBy { it.orderIndex }
    }
    val seriesSections = discoverSections.map { sections ->
        sections.filter { it.type == "series" }.sortedBy { it.orderIndex }
    }

    /**
     * NEW: Call this from your Activity (e.g., MainActivity or UserSelectionActivity)
     * when the user is selected or logs in.
     */
    fun setCurrentUser(authKey: String) {
        // If it's the same user, do nothing
        if (currentUserAuthKey == authKey) return

        currentUserAuthKey = authKey
        // Create a unique preference file name for this user's manifests
        // This assumes your SettingsActivity also saves to this unique name
        userManifestsPrefsName = "manifests_$authKey"

        // Now that the user is set, we can fetch their data
        getFeed()
        fetchCatalogs()
    }


    fun getFeed() {
        val key = currentUserAuthKey
        if (key == null) {
            Log.w("MainViewModel", "No authKey set, cannot fetch feed.")
            feedList.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                feedList.value = stremioClient.api.getFeed(key)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching feed", e)
                // Handle error
            }
        }
    }

    /**
     * NEW: Fetches catalogs directly from an add-on, based on index.html logic.
     * This will populate the `discoverSections` LiveData.
     */
    fun fetchCatalogs() {
        // We can only fetch catalogs if we know *which* user's catalogs to fetch
        if (currentUserAuthKey == null) {
            Log.w("MainViewModel", "No authKey set, cannot fetch catalogs.")
            discoverSections.postValue(emptyList())
            return // Removed @fetchCatalogs
        }

        viewModelScope.launch {
            // NEW: Get user-specific SharedPreferences
            val sharedPrefs = getApplication<Application>().getSharedPreferences(
                userManifestsPrefsName, // e.g., "manifests_abc123xyz"
                Context.MODE_PRIVATE
            )
            // NEW: Load the manifest URLs from settings.
            // I'm assuming it's a Set<String> with this key "manifest_urls".
            // Your SettingsActivity must save to this same key and file.
            val manifestUrls = sharedPrefs.getStringSet("manifest_urls", emptySet()) ?: emptySet()

            // If no URLs, post an empty list
            if (manifestUrls.isEmpty()) {
                Log.w("MainViewModel", "No manifest URLs found in $userManifestsPrefsName")
                discoverSections.postValue(emptyList())
                return@launch
            }

            val allSections = mutableListOf<FeedList>()
            var currentOrderIndex = 0

            // NEW: Loop through every user-provided manifest URL
            for (manifestUrl in manifestUrls) {
                if (manifestUrl.isBlank()) continue

                val baseUrl: String
                try {
                    // This gets the "http://10.0.2.2:7878" part from the full URL
                    baseUrl = manifestUrl.substringBeforeLast('/')
                    if (baseUrl == manifestUrl) { // Check if substringBeforeLast did anything
                        Log.e("MainViewModel", "Invalid manifest URL (no /): $manifestUrl")
                        continue
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing manifest URL: $manifestUrl", e)
                    continue
                }

                try {
                    // 1. Fetch the manifest
                    val manifest = stremioClient.api.getManifest(manifestUrl)

                    // 2. Loop through all catalogs in this manifest
                    for (catalog in manifest.catalogs) {
                        try {
                            val catalogUrl = "$baseUrl/catalog/${catalog.type}/${catalog.id}.json"
                            // 3. Fetch the content (metas) for this catalog
                            val catalogResponse = stremioClient.api.getCatalog(catalogUrl)

                            // 4. Create the FeedList object
                            val feedList = FeedList(
                                id = catalog.id,
                                userId = "", // Dummy value, or use currentUserAuthKey
                                name = "${manifest.name} - ${catalog.name}", // e.g., "Torrentio - Top Movies"
                                catalogUrl = catalogUrl,
                                type = catalog.type,
                                catalogId = catalog.id,
                                orderIndex = currentOrderIndex++ // Increment index
                            ).apply {
                                content = catalogResponse.metas
                            }
                            allSections.add(feedList)
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to fetch catalog: ${catalog.id} from $baseUrl", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to fetch manifest: $manifestUrl", e)
                }
            }

            // 4. Post the final combined list to the LiveData
            discoverSections.postValue(allSections)
        }
    }


    fun getMeta(type: String, id: String) {
        val key = currentUserAuthKey
        if (key == null) {
            Log.w("MainViewModel", "No authKey set, cannot fetch meta.")
            meta.value = null
            return
        }

        viewModelScope.launch {
            try {
                meta.value = stremioClient.api.getMeta(key, type, id).meta
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching meta", e)
                meta.value = null // Post null on error
            }
        }
    }

    fun getStreams(type: String, id: String) {
        val key = currentUserAuthKey
        if (key == null) {
            Log.w("MainViewModel", "No authKey set, cannot fetch streams.")
            streams.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                streams.value = stremioClient.api.getStreams(key, type, id).streams
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching streams", e)
                streams.value = emptyList() // Post empty list on error
            }
        }
    }

    // Call this when leaving the details screen to avoid showing old data
    fun clearMetaAndStreams() {
        meta.value = null
        streams.value = emptyList()
    }
}