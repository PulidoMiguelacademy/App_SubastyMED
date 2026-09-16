package com.example.subastymed.network

import com.google.gson.annotations.SerializedName

// --- Modelos de Subasta ---
data class AuctionDto(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("starting_price") val startingPrice: Double,
    @SerializedName("current_bid") val currentBid: Double,
    @SerializedName("bid_count") val bidCount: Int,
    @SerializedName("time_remaining") val timeRemaining: String,
    @SerializedName("image_url") val imageUrl: String?,
    val status: String,
    @SerializedName("creator_id") val creatorId: Int?
)

data class CreateAuctionDto(
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("starting_price") val startingPrice: Double,
    @SerializedName("time_remaining") val timeRemaining: String = "24h 00m",
    @SerializedName("image_url") val imageUrl: String? = null
)

data class UpdateAuctionDto(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    @SerializedName("starting_price") val startingPrice: Double? = null,
    @SerializedName("current_bid") val currentBid: Double? = null,
    @SerializedName("time_remaining") val timeRemaining: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    val status: String? = null
)

// --- Modelos de Pujas ---
data class PlaceBidDto(
    val amount: Double
)

data class BidDto(
    val id: Int,
    @SerializedName("auction_id") val auctionId: Int,
    @SerializedName("bidder_id") val bidderId: Int,
    val amount: Double,
    @SerializedName("created_at") val createdAt: String
)

data class UserBidDto(
    val id: String,
    @SerializedName("auction_id") val auctionId: Int,
    val title: String,
    @SerializedName("your_bid") val yourBid: String,
    @SerializedName("max_bid") val maxBid: String,
    val status: String, // "Ganando", "Superado", "Ganada", "Perdida"
    @SerializedName("filter_group") val filterGroup: String, // "Activas", "Ganadas", "Perdidas"
    @SerializedName("image_url") val imageUrl: String?
)

// --- Modelos de Autenticación y Perfil ---
data class LoginRequestDto(
    @SerializedName("username_or_email") val usernameOrEmail: String,
    val password: String
)

data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val rating: Double,
    @SerializedName("member_since") val memberSince: String
)

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserDto
)

data class UserStatsDto(
    @SerializedName("won_count") val wonCount: Int,
    @SerializedName("active_bids_count") val activeBidsCount: Int,
    @SerializedName("favorites_count") val favoritesCount: Int
)

data class UploadResponseDto(
    val url: String,
    val filename: String
)

