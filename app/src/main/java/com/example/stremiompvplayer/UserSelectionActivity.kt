package com.example.stremiompvplayer

import android.content.Intent
// ... existing code ...
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivityUserSelectionBinding
import com.example.stremiompvplayer.databinding.DialogUserMenuBinding
import com.example.stremiompvplayer.databinding.ItemUserProfileBinding
import com.example.stremiompvplayer.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlin.random.Random

class UserSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUserSelectionBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: UserAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getInstance(this)
        
        setupRecyclerView()
        setupListeners()
        loadUsers()
    }
    
    private fun setupRecyclerView() {
        adapter = UserAdapter { user ->
            selectUser(user)
        }
        
        binding.usersRecycler.apply {
            layoutManager = GridLayoutManager(this@UserSelectionActivity, 2)
            adapter = this@UserSelectionActivity.adapter
        }
    }
    
    private fun setupListeners() {
        binding.manageProfilesButton.setOnClickListener {
            showAddUserDialog()
        }
    }
    
    private fun loadUsers() {
        val users = database.getAllUsers().toMutableList()
        
        // Add "Add User" button
        adapter.setUsers(users, showAddButton = true)
        
        // If no users exist, create a default one
        if (users.isEmpty()) {
            showAddUserDialog(isFirstUser = true)
        }
    }
    
    private fun showAddUserDialog(isFirstUser: Boolean = false) {
        val input = TextInputEditText(this).apply {
            hint = "Profile Name"
            setPadding(48, 32, 48, 32)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(if (isFirstUser) "Create Your Profile" else "Add Profile")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createUser(name)
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
    
    private fun createUser(name: String) {
        val avatarColor = generateAvatarColor()
        val user = database.createUser(name, avatarColor)
        loadUsers()
        
        // Auto-select if first user
        if (database.getAllUsers().size == 1) {
            selectUser(user)
        }
    }
    
    private fun selectUser(user: User) {
        database.setCurrentUser(user.id)
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun generateAvatarColor(): Int {
        val colors = listOf(
            0xFFE50914.toInt(), // Netflix red
            0xFF0080FF.toInt(), // Blue
            0xFF00C853.toInt(), // Green
            0xFFFFAB00.toInt(), // Orange
            0xFF9C27B0.toInt(), // Purple
            0xFFFF6D00.toInt(), // Deep orange
            0xFF00BCD4.toInt()  // Cyan
        )
        return colors[Random.nextInt(colors.size)]
    }
    
    inner class UserAdapter(
        private val onUserClick: (User) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        
        private val users = mutableListOf<User>()
        private var showAddButton = false
        
        private val TYPE_USER = 0
        private val TYPE_ADD = 1
        
        fun setUsers(newUsers: List<User>, showAddButton: Boolean) {
            users.clear()
            users.addAll(newUsers)
            this.showAddButton = showAddButton
            notifyDataSetChanged()
        }
        
        override fun getItemCount(): Int = users.size + if (showAddButton) 1 else 0
        
        override fun getItemViewType(position: Int): Int {
            return if (position < users.size) TYPE_USER else TYPE_ADD
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_profile, parent, false)
            
            return if (viewType == TYPE_USER) {
                UserViewHolder(view)
            } else {
                AddUserViewHolder(view)
            }
        }
        
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is UserViewHolder && position < users.size) {
                holder.bind(users[position])
            } else if (holder is AddUserViewHolder) {
                holder.bind()
            }
        }
        
        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val avatarContainer: View = itemView.findViewById(R.id.avatarContainer)
            private val avatarInitial: TextView = itemView.findViewById(R.id.avatarInitial)
            private val userName: TextView = itemView.findViewById(R.id.userName)
            
            fun bind(user: User) {
                avatarContainer.setBackgroundColor(user.avatarColor)
                avatarInitial.text = user.name.first().uppercase()
                userName.text = user.name
                
                itemView.setOnClickListener {
                    onUserClick(user)
                }
            }
        }
        
        inner class AddUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val avatarContainer: View = itemView.findViewById(R.id.avatarContainer)
            private val avatarInitial: TextView = itemView.findViewById(R.id.avatarInitial)
            private val userName: TextView = itemView.findViewById(R.id.userName)
            
            fun bind() {
                avatarContainer.setBackgroundColor(0xFF666666.toInt())
                avatarInitial.text = "+"
                userName.text = "Add Profile"
                
                itemView.setOnClickListener {
                    showAddUserDialog()
                }
            }
        }
    }
}
