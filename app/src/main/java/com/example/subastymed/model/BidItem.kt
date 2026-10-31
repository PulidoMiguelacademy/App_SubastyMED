package com.example.subastymed.model

import androidx.compose.ui.graphics.Color
import com.example.subastymed.R

data class BidItem(
    val id: String,
    val title: String,
    val yourBid: String,
    val maxBid: String,
    val status: String,            // "Ganando", "Superado", "Ganada", "Perdida"
    val filterGroup: String,       // "Activas", "Ganadas", "Perdidas"
    val statusTextColor: Color,
    val statusBgColor: Color,
    val imageRes: Int = R.drawable.img_macbook,
    val imageUrl: String? = null
)

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "info"
) {
    fun getRelativeTime(nowMillis: Long = System.currentTimeMillis()): String {
        val diffSec = ((nowMillis - timestamp) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60 -> "Hace unos segundos"
            diffSec < 3600 -> "Hace ${diffSec / 60} min"
            diffSec < 86400 -> "Hace ${diffSec / 3600} h"
            else -> "Hace ${diffSec / 86400} d"
        }
    }
}
