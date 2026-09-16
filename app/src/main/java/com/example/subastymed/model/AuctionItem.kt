package com.example.subastymed.model

import com.example.subastymed.R

data class AuctionItem(
    val id: String,
    val title: String,
    val description: String,
    val currentBid: Double,
    val bidCount: Int,
    val timeRemaining: String,
    val imageRes: Int = R.drawable.img_macbook,
    val category: String,
    val imageUrl: String? = null
)
