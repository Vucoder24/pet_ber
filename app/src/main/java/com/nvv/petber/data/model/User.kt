package com.nvv.petber.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,

    val email: String? = null,

    val username: String? = null,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    val bio: String? = null,

    val phone: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,

    val gender: String? = null,

    val hobbies: String? = null,

    val birthday: String? = null,

    val address: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    val postCount: Long = 0,

    val followerCount: Long = 0,

    val followingCount: Long = 0
): Parcelable
