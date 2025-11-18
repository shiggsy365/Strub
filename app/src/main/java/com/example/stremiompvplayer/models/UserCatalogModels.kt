import androidx.room.PrimaryKey

data class UserCatalog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val catalogId: String,
    val catalogType: String,
    val catalogName: String, // REQUIRED
    val customName: String?, // REQUIRED (can be null)
    val displayOrder: Int,
    val pageType: String,
    val addonUrl: String,    // REQUIRED
    val manifestId: String,
    val dateAdded: Long = System.currentTimeMillis()
)