package com.nvv.petber.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityViewFollowersPetBinding
import com.nvv.petber.ui.adapter.PetFollowerUserAdapter
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.PetFollowersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewFollowersPetActivity : BaseActivity() {
    private lateinit var binding: ActivityViewFollowersPetBinding
    private val viewModel: PetFollowersViewModel by viewModels()
    private lateinit var adapter: PetFollowerUserAdapter

    companion object {
        private const val EXTRA_PET_ID = "extra_pet_id"
        fun start(context: Context, petId: String) {
            val intent = Intent(context, ViewFollowersPetActivity::class.java).apply {
                putExtra(EXTRA_PET_ID, petId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewFollowersPetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val petId = intent.getStringExtra(EXTRA_PET_ID) ?: ""
        if (petId.isEmpty()) {
            toast(R.string.error_get_arg)
            finish()
        }

        setupRecyclerView()
        observeViewModel()

        binding.btnBack.setOnClickListener { finish() }
        viewModel.loadFollowers(petId)
    }

    private fun setupRecyclerView() {
        adapter = PetFollowerUserAdapter { user ->
            UserProfileActivity.start(this, user.id)
        }
        binding.rvFollowers.apply {
            layoutManager = LinearLayoutManager(this@ViewFollowersPetActivity)
            adapter = this@ViewFollowersPetActivity.adapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.followers.collect { list ->
                adapter.submitList(list)
                binding.tvEmpty.visibility =
                    if (list.isEmpty() && !viewModel.isLoading.value) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

}