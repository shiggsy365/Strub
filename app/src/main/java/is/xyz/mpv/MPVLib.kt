package `is`.xyz.mpv

import android.content.Context
import android.util.Log
import android.view.Surface
import java.util.ArrayList

class MPVLib {

    interface EventObserver {
        fun eventProperty(property: String)
        fun eventProperty(property: String, value: Long)
        fun eventProperty(property: String, value: Boolean)
        fun eventProperty(property: String, value: String)
        fun eventProperty(property: String, value: Double)
        fun event(eventId: Int)
    }

    companion object {
        init {
            System.loadLibrary("mpv")
            System.loadLibrary("player")
        }

        @JvmStatic external fun create(context: Context)
        @JvmStatic external fun init()
        @JvmStatic external fun destroy()
        @JvmStatic external fun attachSurface(surface: Surface)
        @JvmStatic external fun detachSurface()
        @JvmStatic external fun command(cmd: Array<String>)
        @JvmStatic external fun setOptionString(name: String, value: String): Int
        @JvmStatic external fun getPropertyString(property: String): String?
        @JvmStatic external fun setPropertyString(property: String, value: String): Int
        @JvmStatic external fun getPropertyInt(property: String): Int?
        @JvmStatic external fun setPropertyInt(property: String, value: Int): Int
        @JvmStatic external fun observeProperty(property: String, format: Int)

        private val observers = ArrayList<EventObserver>()

        fun addObserver(o: EventObserver) {
            synchronized(observers) { observers.add(o) }
        }

        fun removeObserver(o: EventObserver) {
            synchronized(observers) { observers.remove(o) }
        }

        @JvmStatic
        fun eventProperty(property: String, value: Long) {
            synchronized(observers) { observers.forEach { it.eventProperty(property, value) } }
        }

        @JvmStatic
        fun eventProperty(property: String, value: Boolean) {
            synchronized(observers) { observers.forEach { it.eventProperty(property, value) } }
        }

        @JvmStatic
        fun eventProperty(property: String, value: String) {
            synchronized(observers) { observers.forEach { it.eventProperty(property, value) } }
        }

        @JvmStatic
        fun eventProperty(property: String) {
            synchronized(observers) { observers.forEach { it.eventProperty(property) } }
        }

        @JvmStatic
        fun eventProperty(property: String, value: Double) {
            synchronized(observers) { observers.forEach { it.eventProperty(property, value) } }
        }

        @JvmStatic
        fun event(eventId: Int) {
            synchronized(observers) { observers.forEach { it.event(eventId) } }
        }

        // --- ADDED THIS FUNCTION TO FIX THE CRASH ---
        @JvmStatic
        fun logMessage(prefix: String, level: Int, text: String) {
            // This method is called by native code for logging.
            // We print it to Android Logcat for debugging purposes.
            Log.d("MPV_Native", "[$prefix] $text")
        }
        // --------------------------------------------

        const val MPV_EVENT_NONE = 0
        const val MPV_EVENT_SHUTDOWN = 1
        const val MPV_EVENT_LOG_MESSAGE = 2
        const val MPV_EVENT_GET_PROPERTY_REPLY = 3
        const val MPV_EVENT_SET_PROPERTY_REPLY = 4
        const val MPV_EVENT_COMMAND_REPLY = 5
        const val MPV_EVENT_START_FILE = 6
        const val MPV_EVENT_END_FILE = 7
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_IDLE = 11
        const val MPV_EVENT_TICK = 14
        const val MPV_EVENT_PLAYBACK_RESTART = 21
    }
}