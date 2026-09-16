package com.example.subastymed.model

import androidx.compose.ui.graphics.Color

data class BidItem(
    val id: String,
    val title: String,
    val yourBid: String,
    val maxBid: String,
    val status: String,            // "Ganando", "Superado", "Ganada", "Perdida"
    val filterGroup: String,       // "Activas", "Ganadas", "Perdidas"
    val statusTextColor: Color,
    val statusBgColor: Color,
    val imageRes: Int
)