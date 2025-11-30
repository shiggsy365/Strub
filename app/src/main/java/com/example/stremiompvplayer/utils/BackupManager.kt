package com.example.stremiompvplayer.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.RoomDatabase
import com.example.stremiompvplayer.data.AppDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * BackupManager handles creating and restoring encrypted backups of all app data.
 * 
 * Backup includes:
 * - User profiles and settings
 * - API integrations (TMDB, Trakt, AIOStreams, Live TV)
 * - Full SQLite database (watch progress, library, catalogs, etc.)
 * - App preferences
 */
class BackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_FILE_EXTENSION = ".binge"
        private const val METADATA_FILENAME = "metadata.json"
        private const val DATABASE_FILENAME = "database.db"
        private const val PREFERENCES_FILENAME = "shared_prefs.json"
        private const val CHECKSUM_FILENAME = "checksum.sha256"
        private const val BACKUP_VERSION = 1
        
        // Database name from AppDatabase
        private const val DATABASE_NAME = "stremio_player_database"
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val prefsManager: SharedPreferencesManager by lazy {
        SharedPreferencesManager.getInstance(context)
    }

    /**
     * Callback interface for backup/restore progress updates.
     */
    interface BackupCallback {
        fun onProgress(progress: Int, message: String)
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    /**
     * Backup metadata stored in the backup file.
     */
    data class BackupMetadata(
        val version: Int = BACKUP_VERSION,
        val backupDate: Long = System.currentTimeMillis(),
        val appVersionCode: Int = 1,
        val appVersionName: String = "1.0",
        val deviceModel: String = android.os.Build.MODEL,
        val deviceManufacturer: String = android.os.Build.MANUFACTURER,
        val androidVersion: String = android.os.Build.VERSION.RELEASE,
        val databaseVersion: Int = 7,
        val isPasswordProtected: Boolean = false,
        val checksum: String? = null
    )

    /**
     * Complete backup data structure.
     */
    data class BackupData(
        val metadata: BackupMetadata,
        val preferences: String, // JSON string of all preferences
        val databaseBytes: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as BackupData
            if (metadata != other.metadata) return false
            if (preferences != other.preferences) return false
            if (databaseBytes != null) {
                if (other.databaseBytes == null) return false
                if (!databaseBytes.contentEquals(other.databaseBytes)) return false
            } else if (other.databaseBytes != null) return false
            return true
        }

        override fun hashCode(): Int {
            var result = metadata.hashCode()
            result = 31 * result + preferences.hashCode()
            result = 31 * result + (databaseBytes?.contentHashCode() ?: 0)
            return result
        }
    }

    /**
     * Create a backup file at the specified URI.
     *
     * @param uri The destination URI for the backup file
     * @param password Optional password for encryption (null for no password protection)
     * @param callback Progress callback
     */
    suspend fun createBackup(
        uri: Uri,
        password: String?,
        callback: BackupCallback
    ) = withContext(Dispatchers.IO) {
        try {
            callback.onProgress(0, "Preparing backup...")

            // Step 1: Close database connections and checkpoint WAL
            callback.onProgress(10, "Preparing database...")
            val database = AppDatabase.getInstance(context)
            checkpointDatabase(database)

            // Step 2: Export preferences
            callback.onProgress(20, "Exporting preferences...")
            val preferencesJson = prefsManager.exportProfileSettings()

            // Step 3: Copy database file
            callback.onProgress(40, "Copying database...")
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            val databaseBytes = if (databaseFile.exists()) {
                databaseFile.readBytes()
            } else {
                null
            }

            // Step 4: Create metadata
            callback.onProgress(60, "Creating metadata...")
            val metadata = BackupMetadata(
                isPasswordProtected = password != null,
                checksum = databaseBytes?.let { EncryptionHelper.generateChecksum(it) }
            )

            // Step 5: Create backup data
            val backupData = BackupData(
                metadata = metadata,
                preferences = preferencesJson,
                databaseBytes = databaseBytes
            )

            // Step 6: Create temporary zip file
            callback.onProgress(70, "Creating backup archive...")
            val tempFile = File(context.cacheDir, "backup_temp.zip")
            createZipBackup(tempFile, backupData)

            // Step 7: Encrypt if password provided
            callback.onProgress(85, if (password != null) "Encrypting backup..." else "Finalizing backup...")
            val finalData = if (password != null) {
                val zipBytes = tempFile.readBytes()
                val encryptionResult = EncryptionHelper.encryptWithPassword(zipBytes, password)
                EncryptionHelper.packageEncryptedData(encryptionResult)
            } else {
                tempFile.readBytes()
            }

            // Step 8: Write to destination URI
            callback.onProgress(95, "Saving backup file...")
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(finalData)
            }

            // Cleanup
            tempFile.delete()

            callback.onProgress(100, "Backup complete!")
            callback.onSuccess("Backup created successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            callback.onError("Backup failed: ${e.message}")
        }
    }

    /**
     * Restore a backup from the specified URI.
     *
     * @param uri The source URI of the backup file
     * @param password Optional password for decryption
     * @param callback Progress callback
     */
    suspend fun restoreBackup(
        uri: Uri,
        password: String?,
        callback: BackupCallback
    ) = withContext(Dispatchers.IO) {
        try {
            callback.onProgress(0, "Reading backup file...")

            // Step 1: Read backup file
            val backupBytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw Exception("Failed to read backup file")

            // Step 2: Decrypt if needed
            callback.onProgress(15, "Processing backup data...")
            val zipBytes = try {
                if (password != null) {
                    val encryptionResult = EncryptionHelper.unpackageEncryptedData(backupBytes)
                    if (encryptionResult.salt != null) {
                        EncryptionHelper.decryptWithPassword(
                            encryptionResult.encryptedData,
                            password,
                            encryptionResult.iv,
                            encryptionResult.salt
                        )
                    } else {
                        backupBytes // Not encrypted
                    }
                } else {
                    // Try to detect if file is encrypted
                    if (isEncryptedBackup(backupBytes)) {
                        throw Exception("This backup is password protected. Please enter the password.")
                    }
                    backupBytes
                }
            } catch (e: javax.crypto.AEADBadTagException) {
                throw Exception("Incorrect password")
            }

            // Step 3: Extract zip to temp directory
            callback.onProgress(30, "Extracting backup...")
            val tempDir = File(context.cacheDir, "backup_restore_temp")
            tempDir.mkdirs()
            
            val tempZipFile = File(tempDir, "backup.zip")
            tempZipFile.writeBytes(zipBytes)
            extractZip(tempZipFile, tempDir)

            // Step 4: Read and validate metadata
            callback.onProgress(45, "Validating backup...")
            val metadataFile = File(tempDir, METADATA_FILENAME)
            if (!metadataFile.exists()) {
                throw Exception("Invalid backup file: missing metadata")
            }
            val metadata = gson.fromJson(metadataFile.readText(), BackupMetadata::class.java)
            validateBackupCompatibility(metadata)

            // Step 5: Restore database
            callback.onProgress(60, "Restoring database...")
            val databaseBackupFile = File(tempDir, DATABASE_FILENAME)
            if (databaseBackupFile.exists()) {
                // Verify checksum
                if (metadata.checksum != null) {
                    val actualChecksum = EncryptionHelper.generateChecksum(databaseBackupFile.readBytes())
                    if (actualChecksum != metadata.checksum) {
                        throw Exception("Database integrity check failed")
                    }
                }
                
                // Close current database
                AppDatabase.getInstance(context).close()
                
                // Replace database file
                val currentDbFile = context.getDatabasePath(DATABASE_NAME)
                val currentDbWalFile = File(currentDbFile.path + "-wal")
                val currentDbShmFile = File(currentDbFile.path + "-shm")
                
                // Delete WAL and SHM files
                currentDbWalFile.delete()
                currentDbShmFile.delete()
                
                // Copy backup database
                databaseBackupFile.copyTo(currentDbFile, overwrite = true)
            }

            // Step 6: Restore preferences
            callback.onProgress(80, "Restoring preferences...")
            val preferencesFile = File(tempDir, PREFERENCES_FILENAME)
            if (preferencesFile.exists()) {
                val preferencesJson = preferencesFile.readText()
                prefsManager.importProfileSettings(preferencesJson)
            }

            // Step 7: Cleanup
            callback.onProgress(95, "Cleaning up...")
            tempDir.deleteRecursively()

            callback.onProgress(100, "Restore complete!")
            callback.onSuccess("Backup restored successfully. Please restart the app for changes to take effect.")

        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            callback.onError("Restore failed: ${e.message}")
        }
    }

    /**
     * Read backup metadata without restoring.
     */
    suspend fun readBackupMetadata(uri: Uri, password: String? = null): BackupMetadata? = withContext(Dispatchers.IO) {
        try {
            val backupBytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return@withContext null

            val zipBytes = if (password != null) {
                try {
                    val encryptionResult = EncryptionHelper.unpackageEncryptedData(backupBytes)
                    if (encryptionResult.salt != null) {
                        EncryptionHelper.decryptWithPassword(
                            encryptionResult.encryptedData,
                            password,
                            encryptionResult.iv,
                            encryptionResult.salt
                        )
                    } else {
                        backupBytes
                    }
                } catch (e: Exception) {
                    return@withContext null
                }
            } else {
                backupBytes
            }

            // Extract just metadata from zip
            val tempDir = File(context.cacheDir, "backup_metadata_temp")
            tempDir.mkdirs()
            
            val tempZipFile = File(tempDir, "backup.zip")
            tempZipFile.writeBytes(zipBytes)
            
            ZipInputStream(FileInputStream(tempZipFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == METADATA_FILENAME) {
                        val metadataJson = zipIn.bufferedReader().readText()
                        tempDir.deleteRecursively()
                        return@withContext gson.fromJson(metadataJson, BackupMetadata::class.java)
                    }
                    entry = zipIn.nextEntry
                }
            }
            
            tempDir.deleteRecursively()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read backup metadata", e)
            null
        }
    }

    /**
     * Check if a backup file is encrypted.
     */
    fun isEncryptedBackup(backupBytes: ByteArray): Boolean {
        // Try to detect ZIP signature (PK..)
        // If it's not a ZIP, it's likely encrypted
        return !(backupBytes.size >= 4 &&
                backupBytes[0] == 0x50.toByte() &&
                backupBytes[1] == 0x4B.toByte() &&
                backupBytes[2] == 0x03.toByte() &&
                backupBytes[3] == 0x04.toByte())
    }

    /**
     * Generate a default backup filename.
     */
    fun generateBackupFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "binge_backup_$timestamp$BACKUP_FILE_EXTENSION"
    }

    /**
     * Get the estimated backup size.
     */
    suspend fun getEstimatedBackupSize(): Long = withContext(Dispatchers.IO) {
        var size = 0L
        
        // Database size
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (databaseFile.exists()) {
            size += databaseFile.length()
        }
        
        // Add estimate for preferences (usually small)
        size += 10 * 1024 // 10KB estimate for preferences
        
        size
    }

    // Private helper methods

    private fun checkpointDatabase(database: RoomDatabase) {
        try {
            // Checkpoint WAL to main database file
            database.query("PRAGMA wal_checkpoint(TRUNCATE)", null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to checkpoint database", e)
        }
    }

    private fun createZipBackup(outputFile: File, backupData: BackupData) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            // Add metadata
            zipOut.putNextEntry(ZipEntry(METADATA_FILENAME))
            zipOut.write(gson.toJson(backupData.metadata).toByteArray())
            zipOut.closeEntry()

            // Add preferences
            zipOut.putNextEntry(ZipEntry(PREFERENCES_FILENAME))
            zipOut.write(backupData.preferences.toByteArray())
            zipOut.closeEntry()

            // Add database
            if (backupData.databaseBytes != null) {
                zipOut.putNextEntry(ZipEntry(DATABASE_FILENAME))
                zipOut.write(backupData.databaseBytes)
                zipOut.closeEntry()
            }

            // Add checksum
            if (backupData.metadata.checksum != null) {
                zipOut.putNextEntry(ZipEntry(CHECKSUM_FILENAME))
                zipOut.write(backupData.metadata.checksum.toByteArray())
                zipOut.closeEntry()
            }
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(destDir, entry.name)
                if (!entry.isDirectory) {
                    // Ensure parent directory exists
                    filePath.parentFile?.mkdirs()
                    FileOutputStream(filePath).use { fos ->
                        zipIn.copyTo(fos)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    private fun validateBackupCompatibility(metadata: BackupMetadata) {
        // Check backup version
        if (metadata.version > BACKUP_VERSION) {
            throw Exception("Backup was created with a newer version of the app. Please update the app to restore this backup.")
        }

        // Check database version (allow older versions with fallback migration)
        // The app uses fallbackToDestructiveMigration() so this should be fine
    }
}
