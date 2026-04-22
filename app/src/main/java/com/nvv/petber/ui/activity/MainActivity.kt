package com.nvv.petber.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.badge.BadgeDrawable
import com.nvv.petber.R
import com.nvv.petber.data.model.Notification
import com.nvv.petber.databinding.ActivityMainBinding
import com.nvv.petber.ui.auth.login.LoginActivity
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.NotificationHelper
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.viewmodel.MainViewModel
import com.tapadoo.alerter.Alerter
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity(), BottomNavController {
    lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    @Inject
    lateinit var supabaseClient: SupabaseClient
    @Inject
    lateinit var exoPlayer: ExoPlayer
    private lateinit var currentUserId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, 78)
            insets
        }
        currentUserId = SharePrefUtils.getCurrentUserId(this)

        setupBottomNavigation()
        checkSession()
        val lastSeen = SharePrefUtils.getLastSeenNotificationTime(this)
        mainViewModel.fetchNewCount(currentUserId, lastSeen)
        observeNotifications()
    }
    private fun observeNotifications() {
        mainViewModel.startListeningRealtime(currentUserId)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.newNotificationEvent.collect { notification ->
                    if (!isOnNotificationTab()) {
                        showTopBanner(notification)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.unreadCount.collect { count ->
                    if (count > 0) {
                        showNumberBadge(R.id.navigation_notifications, count)
                    } else {
                        hideBadge(R.id.navigation_notifications)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                supabaseClient.realtime.connect()
            } catch (_: Exception) {}
        }
    }

    private fun showTopBanner(notification: Notification) {
        Alerter.hide()
        val rawMessage = NotificationHelper.getMessageText(this, notification)

        val formattedMessage = HtmlCompat.fromHtml(rawMessage, HtmlCompat.FROM_HTML_MODE_COMPACT)
        Alerter.create(this@MainActivity)
            .setTitle(getString(R.string.new_notification))
            .setText(formattedMessage)
            .setIcon(R.drawable.logo_dog_remove_bg)
            .setBackgroundColorRes(R.color.bg_btn)
            .setDuration(5000)
            .setIconColorFilter(0)
            .enableSwipeToDismiss()
            .setOnClickListener {
                if(!isOnNotificationTab()){
                    binding.bottomNavigation.selectedItemId = R.id.navigation_notifications
                }
            }
            .show()
    }

    private fun checkSession() {
        lifecycleScope.launch {
            try {
                supabaseClient.auth.refreshCurrentSession()

                val session = supabaseClient.auth.currentSessionOrNull()

                if (session == null) {
                    handleLogout()
                }
            } catch (e: Exception) {
                if (e.message?.contains("401") == true) {
                    handleLogout()
                }
            }
        }
    }

    private fun handleLogout() {
        mainViewModel.stopRealtime()
        SharePrefUtils.saveCurrentUserId(this, "")

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == navController.currentDestination?.id) return@setOnItemSelectedListener false

            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    inclusive = false,
                    saveState = true
                )

            navController.navigate(item.itemId, null, builder.build())
            true
        }
    }

    fun selectProfileTab() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_profile
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

    fun isOnNotificationTab(): Boolean {
        return binding.bottomNavigation.selectedItemId == R.id.navigation_notifications
    }

    private fun getBadge(menuItemId: Int): BadgeDrawable {
        val badge = binding.bottomNavigation.getOrCreateBadge(menuItemId)
        badge.backgroundColor = ContextCompat.getColor(this, R.color.pet_accent)
        badge.badgeTextColor = ContextCompat.getColor(this, android.R.color.white)
        badge.maxCharacterCount = 3
        badge.isVisible = true
        return badge
    }

    override fun showDotBadge(menuItemId: Int) {
        val badge = getBadge(menuItemId)
        badge.clearNumber()
        badge.isVisible = true
    }

    override fun showNumberBadge(menuItemId: Int, number: Int) {
        val badge = getBadge(menuItemId)
        badge.number = number
        badge.isVisible = true
    }

    override fun hideBadge(menuItemId: Int) {
        binding.bottomNavigation.removeBadge(menuItemId)
    }

    override fun setBottomNavVisible(visible: Boolean) {
        binding.bottomNavigation.isVisible = visible
    }
}

interface BottomNavController {
    fun showDotBadge(menuItemId: Int)
    fun showNumberBadge(menuItemId: Int, number: Int)
    fun hideBadge(menuItemId: Int)
    fun setBottomNavVisible(visible: Boolean)
}