package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivityUserSelectionBinding
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class UserSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserSelectionBinding
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager.getInstance(this)

        setupRecyclerView()
        setupListeners()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(
            onUserClick = { user -> selectUser(user) },
            onUserLongClick = { user -> showDeleteUserDialog(user) }
        )

        binding.usersRecycler.apply {
            // Vertical list like sidecar
            layoutManager = LinearLayoutManager(this@UserSelectionActivity)
            adapter = this@UserSelectionActivity.adapter
        }
    }

    private fun setupListeners() {
        binding.btnExit.setOnClickListener {
            finishAffinity()
        }

        binding.btnAddUser.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun loadUsers() {
        val users = prefsManager.getAllUsers().toMutableList()
        adapter.setUsers(users)

        // If no users exist, force create a default one
        if (users.isEmpty()) {
            showAddUserDialog(isFirstUser = true)
        }
    }

    private fun showDeleteUserDialog(user: User) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Profile")
            .setMessage("Are you sure you want to delete '${user.name}'?\n\nThis will permanently remove all watch history, favorites, and catalog customizations for this user.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUserProfile(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserProfile(user: User) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(applicationContext).deleteUserData(user.id)
            withContext(Dispatchers.Main) {
                prefsManager.deleteUser(user.id)
                loadUsers()
            }
        }
    }

    private fun showAddUserDialog(isFirstUser: Boolean = false) {
        val input = TextInputEditText(this).apply {
            hint = "Profile Name"
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        val kidsCheckBox = CheckBox(this).apply {
            text = "Kids Profile (PG content only)"
            setPadding(48, 16, 48, 32)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(input)
            addView(kidsCheckBox)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (isFirstUser) "Create Your Profile" else "Add Profile")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createUser(name, kidsCheckBox.isChecked)
                }
            }
            .apply {
                if (!isFirstUser) {
                    setNegativeButton("Cancel", null)
                }
            }
            .setCancelable(!isFirstUser)
            .show()
    }

    private fun createUser(name: String, isKidsProfile: Boolean = false) {
        val avatarColor = generateAvatarColor()
        val user = prefsManager.createUser(name, avatarColor, isKidsProfile)
        loadUsers()

        if (prefsManager.getAllUsers().size == 1) {
            selectUser(user)
        }
    }

    private fun selectUser(user: User) {
        prefsManager.setCurrentUser(user.id)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun generateAvatarColor(): Int {
        val colors = listOf(
            0xFFE50914.toInt(), 0xFF0080FF.toInt(), 0xFF00C853.toInt(),
            0xFFFFAB00.toInt(), 0xFF9C27B0.toInt(), 0xFFFF6D00.toInt(), 0xFF00BCD4.toInt()
        )
        return colors[Random.nextInt(0, colors.size)]
    }

    // Updated Adapter using item_user_list
    inner class UserAdapter(
        private val onUserClick: (User) -> Unit,
        private val onUserLongClick: (User) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        private val users = mutableListOf<User>()

        fun setUsers(newUsers: List<User>) {
            users.clear()
            users.addAll(newUsers)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = users.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_list, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            holder.bind(users[position])
        }

        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val userName: TextView = itemView.findViewById(R.id.userName)
            // Icon is static in xml, but we could tint it if needed using user.avatarColor
            // private val userIcon: ImageView = itemView.findViewById(R.id.userIcon)

            fun bind(user: User) {
                userName.text = user.name

                // Optional: Tint icon with user color
                // userIcon.setColorFilter(user.avatarColor)

                itemView.setOnClickListener { onUserClick(user) }
                itemView.setOnLongClickListener {
                    onUserLongClick(user)
                    true
                }
            }
        }
    }
}