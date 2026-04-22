package com.nvv.petber.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivitySetLanguageBinding
import com.nvv.petber.ui.adapter.LanguageAdapter
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.LocaleHelper
import com.nvv.petber.viewmodel.LanguageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetLanguageActivity : BaseActivity() {
    private lateinit var binding: ActivitySetLanguageBinding
    private val viewModel: LanguageViewModel by viewModels()
    private lateinit var adapter: LanguageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySetLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupButtons()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = LanguageAdapter { lang ->
            viewModel.selectLanguage(lang.code)
        }
        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(this@SetLanguageActivity)
            adapter = this@SetLanguageActivity.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnApply.setOnClickListener {
            val lang = viewModel.selectedCode.value
            viewModel.applyLanguage()
            applyLocaleAndRestart(lang)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.displayList .collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    private fun applyLocaleAndRestart(langCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(langCode)
            )
        } else {
            LocaleHelper.setLocale(this, langCode)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}