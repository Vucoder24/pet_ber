//package com.nvv.petber.data.dto
//
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//
//@Serializable
//data class PostResponse(
//    val id: String,
//    @SerialName("user_id") val userId: String,
//    @SerialName("pet_id") val petId: String? = null,
//    val caption: String? = null,
//    val location: String? = null,
//    @SerialName("like_count") val likeCount: Int = 0,
//    @SerialName("comment_count") val commentCount: Int = 0,
//    @SerialName("created_at") val createdAt: String,
//    val users: UserMini? = null,
//    @SerialName("post_images") val postImages: List<PostImageResponse>? = null
//)
//
//@Serializable
//data class StoryResponse(
//    val id: String,
//    @SerialName("user_id") val userId: String,
//    @SerialName("image_url") val imageUrl: String,
//    @SerialName("created_at") val createdAt: String,
//    @SerialName("expires_at") val expiresAt: String? = null,
//    val user: UserMini? = null
//)
//
//@Serializable
//data class UserMini(
//    val id: String,
//    val username: String? = null,
//    @SerialName("avatar_url") val avatarUrl: String? = null
//)
//
//@Serializable
//data class PostImageResponse(
//    val id: String,
//    @SerialName("post_id") val postId: String,
//    @SerialName("image_url") val imageUrl: String
//)
//
//@Serializable
//data class PostLikeResponse(
//    val id: String,
//    @SerialName("post_id") val postId: String,
//    @SerialName("user_id") val userId: String
//)
//
//@Serializable
//data class SearchResult(
//    val id: String,
//    val title: String,
//    val subTitle: String? = null,
//    val imageUrl: String? = null,
//    val type: SearchType
//)
//
//@Serializable
//data class PetResponse(
//    val id: String,
//    @SerialName("owner_id") val ownerId: String,
//    val name: String,
//    val avatarUrl: String? = null,
//    val breed: String? = null,
//    val species: String? = null,
//    val location: String? = null,
//    val description: String? = null
//)
//
//@Serializable
//data class HashtagResponse(
//    val id: String,
//    val name: String,
//    @SerialName("created_at") val createdAt: String? = null
//)
//
//enum class SearchType { USER, PET, HASHTAG }
//
