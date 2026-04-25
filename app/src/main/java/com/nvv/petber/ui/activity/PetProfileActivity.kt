package com.nvv.petber.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.google.android.material.tabs.TabLayoutMediator
import com.nvv.petber.R
import com.nvv.petber.data.model.Pet
import com.nvv.petber.databinding.ActivityPetProfileBinding
import com.nvv.petber.ui.adapter.PetProfilePagerAdapter
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.ui.dialog.PetMoreBottomSheetFragment
import com.nvv.petber.utils.PermissionUtils
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.autoHeight
import com.nvv.petber.utils.ext.formatSocialCount
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.showAvatarOptionDialog
import com.nvv.petber.utils.ext.showCoverOptionDialog
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.PetProfileEvent
import com.nvv.petber.viewmodel.PetProfileViewModel
import com.nvv.petber.viewmodel.UpdatePetState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class PetProfileActivity : BaseActivity() {
    @Inject
    lateinit var exoPlayer: ExoPlayer

    private lateinit var binding: ActivityPetProfileBinding
    private val viewModel: PetProfileViewModel by viewModels()
    private var pendingMediaAction: String = ""
    private var cropTarget: String? = null
    private var currentPet: Pet? = null
    private lateinit var currentUserId: String
    private val editPetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val petId = viewModel.petState.value?.id
            if (petId != null) {
                viewModel.fetchPetById(petId)
            }
        }
    }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uriString = result.data?.getStringExtra(CropImageActivity.EXTRA_RESULT_URI)
                    ?: return@registerForActivityResult
                val uri = uriString.toUri()

                when (cropTarget) {
                    CropImageActivity.TARGET_AVATAR -> viewModel.updateAvatar(uri)
                    CropImageActivity.TARGET_COVER -> viewModel.updateCover(uri)
                }
                cropTarget = null
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.all { it.value }
            if (granted) {
                when (pendingMediaAction) {
                    "avatar" -> openMediaPickerForAvatar()
                    "cover" -> openMediaPickerForCover()
                }
            } else {
                toast(getString(R.string.permission_question))
            }
        }

    private val avatarPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                @Suppress("DEPRECATION")
                val medias =
                    result.data?.getParcelableArrayListExtra<com.nvv.petber.ui.adapter.MediaItem>(
                        MediaPickerActivity.EXTRA_RESULT_MEDIAS
                    )
                medias?.firstOrNull()?.uri?.let { uri ->
                    launchCrop(uri, CropImageActivity.TARGET_AVATAR)
                }
            }
        }

    private val coverPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                @Suppress("DEPRECATION")
                val medias =
                    result.data?.getParcelableArrayListExtra<com.nvv.petber.ui.adapter.MediaItem>(
                        MediaPickerActivity.EXTRA_RESULT_MEDIAS
                    )
                medias?.firstOrNull()?.uri?.let { uri ->
                    launchCrop(uri, CropImageActivity.TARGET_COVER)
                }
            }
        }

    companion object {
        const val EXTRA_PET_JSON = "extra_pet_json"
        private const val EXTRA_PET_ID = "extra_pet_id"

        fun start(context: Context, pet: Pet) {
            val intent = Intent(context, PetProfileActivity::class.java).apply {
                putExtra(EXTRA_PET_JSON, Json.encodeToString(pet))
            }
            context.startActivity(intent)
        }

        fun start(context: Context, petId: String) {
            val intent = Intent(context, PetProfileActivity::class.java).apply {
                putExtra(EXTRA_PET_ID, petId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPetProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        currentUserId = SharePrefUtils.getCurrentUserId(this)
        setupViewPager()
        setupIntentData()
        setupListeners()
        observeViewModel()
        setupFragmentResultListener()
    }

    private fun setupFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener("refresh_key", this) { _, bundle ->
            val isUpdated = bundle.getBoolean("bundle_is_updated", false)
            if (isUpdated) {
                viewModel.petState.value?.id?.let { petId ->
                    viewModel.fetchPetById(petId)
                }
            }
        }
        supportFragmentManager.setFragmentResultListener(
            "post_deleted_key",
            this
        ) { _, bundle ->
            val deletedPostId = bundle.getString("bundle_post_id")
            if (deletedPostId != null) {
                viewModel.removePostById(deletedPostId)
            }
        }
    }

    private fun setupViewPager() {
        binding.viewPager.isUserInputEnabled = false
        val pagerAdapter = PetProfilePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.all_posts)
                1 -> getString(R.string.pet_diary)
                else -> ""
            }
        }.attach()
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.viewPager.autoHeight()
                }
            }
        )
    }

    private fun setupIntentData() {
        val petJson = intent.getStringExtra(EXTRA_PET_JSON)
        val petId = intent.getStringExtra(EXTRA_PET_ID)

        if (petJson != null) {
            try {
                val pet = Json.decodeFromString<Pet>(petJson)
                viewModel.setPetData(pet, currentUserId == pet.ownerId)
            } catch (_: Exception) {
                toast(getString(R.string.fail_to_load_pet_data))
                finish()
            }
        } else if (petId != null) {
            viewModel.fetchPetById(petId)
        } else {
            finish()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFollow.setOnClickListener {
            viewModel.toggleFollow(this)
        }
        binding.root.setOnRefreshListener {
            val currentPet = viewModel.petState.value
            if (currentPet != null) {
                viewModel.fetchPetById(currentPet.id)
            } else {
                binding.root.isRefreshing = false
            }
        }

        binding.avatar.setOnClickListener {
            if(viewModel.isOwner.value){
                val currentPet = viewModel.petState.value ?: return@setOnClickListener

                showAvatarOptionDialog(
                    onViewAvatar = {
                        if (!currentPet.avatarUrl.isNullOrEmpty()) {
                            val mediaItem = com.nvv.petber.ui.adapter.MediaItem(
                                uri = currentPet.avatarUrl.toUri(),
                                isVideo = false,
                                duration = 0L
                            )
                            startActivity(
                                Intent(
                                    this@PetProfileActivity,
                                    MediaPreviewActivity::class.java
                                ).apply {
                                    putExtra(MediaPreviewActivity.EXTRA_MEDIA, mediaItem)
                                })
                        } else {
                            toast(getString(R.string.no_avatar_found))
                        }
                    },
                    onChooseAvatar = {
                        if (PermissionUtils.hasMediaPermissions(this@PetProfileActivity)) {
                            openMediaPickerForAvatar()
                        } else {
                            pendingMediaAction = "avatar"
                            val denied =
                                PermissionUtils.getDeniedPermissions(this@PetProfileActivity)
                            permissionLauncher.launch(denied)
                        }
                    }
                )
            } else{
                viewMediaOnly(currentPet?.avatarUrl)
            }
        }

        binding.imgCover.setOnClickListener {
            if (viewModel.isOwner.value){
                val currentPet = viewModel.petState.value ?: return@setOnClickListener

                showCoverOptionDialog(
                    onViewCover = {
                        if (!currentPet.coverUrl.isNullOrEmpty()) {
                            val mediaItem = com.nvv.petber.ui.adapter.MediaItem(
                                uri = currentPet.coverUrl.toUri(),
                                isVideo = false,
                                duration = 0L
                            )
                            startActivity(
                                Intent(
                                    this@PetProfileActivity,
                                    MediaPreviewActivity::class.java
                                ).apply {
                                    putExtra(MediaPreviewActivity.EXTRA_MEDIA, mediaItem)
                                })
                        } else {
                            toast(getString(R.string.no_cover_found))
                        }
                    },
                    onChooseCover = {
                        if (PermissionUtils.hasMediaPermissions(this@PetProfileActivity)) {
                            openMediaPickerForCover()
                        } else {
                            pendingMediaAction = "cover"
                            val denied =
                                PermissionUtils.getDeniedPermissions(this@PetProfileActivity)
                            permissionLauncher.launch(denied)
                        }
                    }
                )
            } else{
                viewMediaOnly(currentPet?.coverUrl)
            }
        }
        binding.btnEditPet.setOnClickListener {
            val pet = viewModel.petState.value ?: return@setOnClickListener

            val intent = Intent(this, CreateEditPetActivity::class.java).apply {
                putExtra(CreateEditPetActivity.EXTRA_PET, pet)
            }
            editPetLauncher.launch(intent)
        }

        binding.dataContainer.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val isBottomReached = scrollY >= (v.getChildAt(0).measuredHeight - v.measuredHeight - 200)

                if (isBottomReached) {
                    viewModel.petState.value?.id?.let { petId ->
                        viewModel.loadPetPosts(petId, isRefresh = false)
                    }
                }
            }
        )
        binding.btnViewFollowes.setOnClickListener {
            ViewFollowersPetActivity.start(this, viewModel.petState.value?.id ?: "")
        }


        binding.btnMore.apply {
            visibility = View.GONE
            setOnClickListener {
                val sheet = PetMoreBottomSheetFragment.newInstance()
                sheet.onDeleteClick = {
                    viewModel.deletePet()
                }
                sheet.show(supportFragmentManager, "PetMoreSheet")
            }
        }
    }

    private fun viewMediaOnly(url: String?) {
        if (!url.isNullOrEmpty()) {
            val mediaItem = com.nvv.petber.ui.adapter.MediaItem(
                uri = url.toUri(),
                isVideo = false,
                duration = 0L
            )
            startActivity(
                Intent(this@PetProfileActivity, MediaPreviewActivity::class.java).apply {
                    putExtra(MediaPreviewActivity.EXTRA_MEDIA, mediaItem)
                }
            )
        } else {
            toast(getString(R.string.img_not_updated))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isOwner.collect { isOwner ->
                binding.btnEditPet.visibility = if (isOwner) View.VISIBLE else View.GONE
                binding.btnFollow.visibility = if (!isOwner) View.VISIBLE else View.GONE
                binding.btnMore.visibility = if (isOwner) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.isFollowing.collect { isFollowing ->
                if (isFollowing) {
                    binding.btnFollow.text = getString(R.string.unfollow)
                    binding.btnFollow.setBackgroundColor(getColor(R.color.gray_light))
                    binding.btnFollow.setTextColor(getColor(R.color.black))
                } else {
                    binding.btnFollow.text = getString(R.string.follow)
                    binding.btnFollow.setBackgroundColor(getColor(R.color.bg_btn))
                    binding.btnFollow.setTextColor(getColor(R.color.white))
                }
            }
        }
        lifecycleScope.launch {
            viewModel.petState.collect { pet ->
                pet?.let {
                    bindPetData(it)
                    currentPet = it
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.root.isRefreshing = isLoading
                binding.shimmerView.visibility =
                    if (isLoading && viewModel.petState.value == null) View.VISIBLE else View.GONE
                binding.dataContainer.visibility =
                    if (isLoading && viewModel.petState.value == null) View.INVISIBLE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                errorMsg?.let {
                    toast(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UpdatePetState.Loading -> {
                        if (state.style == "avatar") {
                            toast(getString(R.string.updating_avatar))
                        } else if (state.style == "cover") {
                            toast(getString(R.string.updating_cover))
                        }
                    }

                    is UpdatePetState.Success -> {
                        toast(getString(R.string.update_pet_success))
                        viewModel.resetUiState()
                    }

                    is UpdatePetState.Error -> {
                        toast(state.message)
                        viewModel.resetUiState()
                    }

                    is UpdatePetState.Idle -> {}
                }
            }
        }
        lifecycleScope.launch {
            viewModel.followerCount.collect { count ->
                binding.tvFollowCount.text = count.formatSocialCount()
            }
        }
        lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is PetProfileEvent.PetDeleted -> {
                        toast(getString(R.string.delete_pet_success))
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("pet_deleted", true)
                        })
                        finish()
                    }

                    is PetProfileEvent.PetDeleteError -> {
                        toast(getString(R.string.delete_pet_error))
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isDeleting.collect { isDeleting ->
                if (isDeleting) toast(getString(R.string.deleting_pet))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindPetData(pet: Pet) {
        with(binding) {
            tvPetName.text = pet.name

            val speciesTxt = pet.species ?: getString(R.string.not_updated)
            val breedTxt = pet.breed ?: getString(R.string.not_updated)
            tvSpeciesBreed.text = "$speciesTxt • $breedTxt"

            tvDescription.text = if (pet.description.isNullOrEmpty()) {
                getString(R.string.not_desc_yet)
            } else pet.description

            avatar.loadAvatar(pet.avatarUrl)
            pet.coverUrl?.let { url ->
                Glide.with(this@PetProfileActivity)
                    .load(url)
                    .override(600, 600)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .into(imgCover)
            } ?: imgCover.setImageResource(R.color.gray_light) // Default cover

            val na = getString(R.string.value_na)
            tvGender.text = getString(R.string.label_gender, pet.gender ?: na)
            tvWeight.text = getString(R.string.label_weight, pet.weight?.let { "$it kg" } ?: na)
            tvBirthday.text = getString(R.string.label_birthday, pet.birthday ?: na)
            tvNeutered.text = getString(
                R.string.label_neutered,
                if (pet.isNeutered == true) getString(R.string.value_neutered_yes)
                else getString(R.string.value_neutered_no)
            )

            tvBodyCondition.text = getString(R.string.label_body_condition, pet.bodyCondition ?: na)
            tvClinicalStatus.text =
                getString(R.string.label_clinical_status, pet.clinicalStatus ?: na)
            tvMentalState.text =
                getString(R.string.label_mental_state, pet.activityAndMentalState ?: na)
            tvMedicalHistory.text =
                getString(R.string.label_medical_history, pet.medicalHistoryAndTreatment ?: na)
            tvPreventive.text = getString(R.string.label_preventive, pet.preventiveStatus ?: na)
        }
    }

    private fun launchCrop(sourceUri: android.net.Uri, target: String) {
        cropTarget = target
        val intent = Intent(this, CropImageActivity::class.java).apply {
            putExtra(CropImageActivity.EXTRA_SOURCE_URI, sourceUri)
            putExtra(CropImageActivity.EXTRA_TARGET_TYPE, target)
        }
        cropLauncher.launch(intent)
    }

    private fun openMediaPickerForAvatar() {
        val intent = Intent(this, MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_SINGLE)
            putExtra(MediaPickerActivity.EXTRA_MEDIA_KIND, MediaPickerActivity.MEDIA_KIND_IMAGES)
        }
        avatarPickerLauncher.launch(intent)
    }

    private fun openMediaPickerForCover() {
        val intent = Intent(this, MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_SINGLE)
            putExtra(MediaPickerActivity.EXTRA_MEDIA_KIND, MediaPickerActivity.MEDIA_KIND_IMAGES)
        }
        coverPickerLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}