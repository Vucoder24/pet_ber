package com.nvv.petber.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchFilter(
    val type: FilterType,
    // User filters
    var userGender: String? = null,
    var userAddress: String? = null,
    var userPhoneNumber: String? = null,
    // Pet filters
    var petSpecies: String? = null,
    var petGender: String? = null,
    var isNeutered: Boolean? = null,
    // Post filters
    var postSortBy: String = "created_at",
    var postDateFrom: String? = null,
    var postDateTo: String? = null,
) : Parcelable

enum class FilterType { USER, PET, POST }