package com.example.subastymed.model

import android.net.Uri
import com.example.subastymed.R
import com.example.subastymed.network.RetrofitClient
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

enum class AuctionPhase {
    SCHEDULED, // Aún no ha llegado la hora de inicio
    LIVE,      // En vivo (dentro de la 1 hora activa de pujas)
    ENDED      // Subasta cerrada / finalizada
}

data class LiveBidEntry(
    val id: String = "",
    val auctionId: String = "",
    val bidderId: String = "",
    val bidderName: String = "Usuario",
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedAmount(): String {
        return try {
            val format = NumberFormat.getNumberInstance(Locale.US)
            if (amount == amount.toLong().toDouble()) {
                "$" + format.format(amount.toLong())
            } else {
                "$" + format.format(amount)
            }
        } catch (e: Exception) {
            "$$amount"
        }
    }

    fun getRelativeTime(nowMillis: Long = System.currentTimeMillis()): String {
        val diffSec = ((nowMillis - timestamp) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 10 -> "Hace unos segundos"
            diffSec < 60 -> "Hace ${diffSec}s"
            diffSec < 3600 -> "Hace ${diffSec / 60} min"
            else -> "Hace ${diffSec / 3600} h"
        }
    }
}

data class AuctionItem(
    val id: String,
    val title: String,
    val description: String,
    val currentBid: Double,
    val bidCount: Int,
    val timeRemaining: String,
    val imageRes: Int = R.drawable.img_macbook,
    val category: String,
    val imageUrl: String? = null,
    val formattedPrice: String? = null,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val status: String = "Activa",
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val highestBidderId: String? = null,
    val highestBidderName: String? = null,
    val creatorPhone: String? = null,
    val highestBidderPhone: String? = null
) {
    companion object {
        const val ONE_HOUR_MILLIS = 60 * 60 * 1000L // 1 hora de duración de la subasta

        /**
         * Convierte cualquier entrada numérica escrita por el usuario (ej: "24.000", "24,000", "24000", "1.500.000")
         * al valor numérico real (24000.0, 1500000.0), reconociendo puntos y comas de miles.
         */
        fun parsePriceInput(raw: String): Double? {
            val cleaned = raw.trim().replace("$", "").replace(" ", "")
            if (cleaned.isEmpty()) return null

            val dotCount = cleaned.count { it == '.' }
            val commaCount = cleaned.count { it == ',' }

            // Múltiples puntos o comas (ej: "1.250.000" o "1,250,000") -> separadores de miles
            if (dotCount > 1 || commaCount > 1) {
                return cleaned.filter { it.isDigit() }.toDoubleOrNull()
            }

            // Un solo punto (ej: "24.000" -> 24000, mientras que "24.5" -> 24.5)
            if (dotCount == 1 && commaCount == 0) {
                val afterDot = cleaned.substringAfter(".")
                return if (afterDot.length == 3) {
                    cleaned.replace(".", "").toDoubleOrNull()
                } else {
                    cleaned.toDoubleOrNull()
                }
            }

            // Una sola coma (ej: "24,000" -> 24000, mientras que "24,5" -> 24.5)
            if (commaCount == 1 && dotCount == 0) {
                val afterComma = cleaned.substringAfter(",")
                return if (afterComma.length == 3) {
                    cleaned.replace(",", "").toDoubleOrNull()
                } else {
                    cleaned.replace(",", ".").toDoubleOrNull()
                }
            }

            // Punto y coma combinados
            if (dotCount == 1 && commaCount == 1) {
                val lastDot = cleaned.lastIndexOf('.')
                val lastComma = cleaned.lastIndexOf(',')
                return if (lastComma > lastDot) {
                    cleaned.replace(".", "").replace(",", ".").toDoubleOrNull()
                } else {
                    cleaned.replace(",", "").toDoubleOrNull()
                }
            }

            return cleaned.filter { it.isDigit() }.toDoubleOrNull()
        }

        fun formatCurrency(amount: Double): String {
            return try {
                val format = NumberFormat.getNumberInstance(Locale.US)
                if (amount == amount.toLong().toDouble()) {
                    "$" + format.format(amount.toLong())
                } else {
                    "$" + format.format(amount)
                }
            } catch (e: Exception) {
                "$$amount"
            }
        }

        // Caché en memoria para evitar decodificar imágenes Base64 repetidamente en cada segundo del cronómetro
        private val bitmapCache = android.util.LruCache<Int, android.graphics.Bitmap>(80)

        fun resolveImageModel(imageUrl: String?, fallbackRes: Int): Any {
            val url = imageUrl
            if (url.isNullOrBlank()) return fallbackRes
            return when {
                url.startsWith("data:image") -> {
                    val cacheKey = url.hashCode()
                    val cached = bitmapCache.get(cacheKey)
                    if (cached != null && !cached.isRecycled) {
                        return cached
                    }
                    try {
                        val base64Data = url.substringAfter("base64,")
                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bmp != null) {
                            bitmapCache.put(cacheKey, bmp)
                            bmp
                        } else {
                            fallbackRes
                        }
                    } catch (e: Exception) {
                        fallbackRes
                    }
                }
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("content://") || url.startsWith("file://") -> Uri.parse(url)
                url.startsWith("/") && !url.startsWith("/uploads/") -> File(url)
                url.startsWith("/uploads/") -> "${RetrofitClient.BASE_URL.removeSuffix("/")}$url"
                else -> url
            }
        }

        fun parseDateStringToMillis(dateStr: String): Long {
            if (dateStr.isBlank() || !dateStr.contains("/")) return 0L
            val normalized = dateStr
                .replace("a. m.", "AM", ignoreCase = true)
                .replace("p. m.", "PM", ignoreCase = true)
                .replace("a.m.", "AM", ignoreCase = true)
                .replace("p.m.", "PM", ignoreCase = true)
                .replace("a. m", "AM", ignoreCase = true)
                .replace("p. m", "PM", ignoreCase = true)
                .trim()

            val patterns = listOf(
                "dd/MM/yyyy - hh:mm a",
                "dd/MM/yyyy - HH:mm",
                "dd/MM/yyyy hh:mm a"
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    val parsed = sdf.parse(normalized)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}
                try {
                    val sdfDefault = SimpleDateFormat(pattern, Locale.getDefault())
                    val parsed = sdfDefault.parse(dateStr.trim())
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}
            }
            return 0L
        }

        fun formatCountdown(millisRemaining: Long): String {
            val totalSeconds = (millisRemaining / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return if (hours > 0) {
                String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02dm %02ds", minutes, seconds)
            }
        }
    }

    fun getEffectiveStartTimeMillis(): Long {
        if (startTimeMillis > 0L) return startTimeMillis
        return parseDateStringToMillis(timeRemaining)
    }

    fun getEffectiveEndTimeMillis(): Long {
        if (endTimeMillis > 0L) return endTimeMillis
        val start = getEffectiveStartTimeMillis()
        return if (start > 0L) start + ONE_HOUR_MILLIS else 0L
    }

    fun getCurrentPhase(nowMillis: Long = System.currentTimeMillis()): AuctionPhase {
        if (status.equals("Finalizada", ignoreCase = true) ||
            timeRemaining.contains("Finalizada", ignoreCase = true)
        ) {
            return AuctionPhase.ENDED
        }
        val start = getEffectiveStartTimeMillis()
        val end = getEffectiveEndTimeMillis()
        if (start > 0L && end > 0L) {
            return when {
                nowMillis < start -> AuctionPhase.SCHEDULED
                nowMillis < end -> AuctionPhase.LIVE
                else -> AuctionPhase.ENDED
            }
        }
        return AuctionPhase.LIVE
    }

    fun getLiveStatusBadgeText(nowMillis: Long = System.currentTimeMillis()): String {
        return when (getCurrentPhase(nowMillis)) {
            AuctionPhase.ENDED -> "🏁 Finalizada"
            AuctionPhase.SCHEDULED -> {
                val diff = getEffectiveStartTimeMillis() - nowMillis
                if (diff <= 24 * 3600_000L) {
                    "⏳ Inicia en ${formatCountdown(diff)}"
                } else {
                    "📅 $timeRemaining"
                }
            }
            AuctionPhase.LIVE -> {
                val end = getEffectiveEndTimeMillis()
                if (end > 0L) {
                    val remaining = (end - nowMillis).coerceAtLeast(0L)
                    "🔴 EN VIVO • ${formatCountdown(remaining)}"
                } else {
                    "🔴 EN VIVO • 1h"
                }
            }
        }
    }

    fun getDisplayPrice(): String {
        return try {
            val format = NumberFormat.getNumberInstance(Locale.US)
            if (currentBid == currentBid.toLong().toDouble()) {
                "$" + format.format(currentBid.toLong())
            } else {
                "$" + format.format(currentBid)
            }
        } catch (e: Exception) {
            if (!formattedPrice.isNullOrBlank()) {
                val cleaned = formattedPrice.removePrefix("$").trim()
                "$$cleaned"
            } else {
                "$$currentBid"
            }
        }
    }

    fun getResolvedImage(): Any {
        return resolveImageModel(imageUrl, imageRes)
    }
}
