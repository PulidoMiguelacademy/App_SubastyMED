package com.example.subastymed.model

data class AuctionItem(
    val id: String,
    val title: String,
    val description: String,
    val currentBid: Double,
    val bidCount: Int,
    val timeRemaining: String,
    val imageRes: Int,
    val category: String,
)
