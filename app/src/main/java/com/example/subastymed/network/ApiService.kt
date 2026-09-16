package com.example.subastymed.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part

import retrofit2.http.DELETE
import retrofit2.http.PUT

interface ApiService {

    @GET("api/auctions")
    suspend fun getAuctions(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): List<AuctionDto>

    @GET("api/auctions/my-auctions")
    suspend fun getMyAuctions(): List<AuctionDto>

    @GET("api/auctions/{id}")
    suspend fun getAuctionDetail(
        @Path("id") id: Int
    ): AuctionDto

    @POST("api/auctions")
    suspend fun createAuction(
        @Body request: CreateAuctionDto
    ): AuctionDto

    @PUT("api/auctions/{id}")
    suspend fun updateAuction(
        @Path("id") id: Int,
        @Body request: UpdateAuctionDto
    ): AuctionDto

    @DELETE("api/auctions/{id}")
    suspend fun deleteAuction(
        @Path("id") id: Int
    )

    @Multipart
    @POST("api/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): UploadResponseDto

    @POST("api/auctions/{id}/bid")
    suspend fun placeBid(
        @Path("id") id: Int,
        @Body request: PlaceBidDto
    ): BidDto

    @GET("api/bids/my-bids")
    suspend fun getMyBids(
        @Query("filter_group") filterGroup: String? = null
    ): List<UserBidDto>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): LoginResponseDto

    @GET("api/auth/stats")
    suspend fun getUserStats(): UserStatsDto
}
