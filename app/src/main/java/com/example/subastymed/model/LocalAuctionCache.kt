package com.example.subastymed.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.subastymed.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LocalAuctionCache {
    private const val PREFS_NAME = "subastymed_auctions_cache"
    private const val KEY_ALL_AUCTIONS = "all_auctions_json"
    private const val KEY_MY_AUCTIONS = "my_auctions_json"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Si no hay datos, inicializamos con los iniciales para que nunca esté vacío
        if (getAllAuctions().isEmpty()) {
            saveAllAuctions(getDefaultInitialAuctions())
        }
    }

    private fun getDefaultInitialAuctions(): List<AuctionItem> {
        return listOf(
            AuctionItem(
                id = "1",
                title = "MacBook Pro 16\" M4",
                description = "Excelente estado, 32GB RAM, 1TB SSD, cargador original y caja.",
                currentBid = 1250.00,
                bidCount = 14,
                timeRemaining = "02h 15m",
                imageRes = R.drawable.img_macbook,
                category = "Electrónica",
                imageUrl = "/uploads/img_macbook.png"
            ),
            AuctionItem(
                id = "2",
                title = "BMW Serie 3 2021",
                description = "Impecable, 35,000 km, mantenimientos al día en concesionario oficial.",
                currentBid = 24800.00,
                bidCount = 45,
                timeRemaining = "1d 04h",
                imageRes = R.drawable.img_bmw,
                category = "Vehículos",
                imageUrl = "/uploads/img_bmw.png"
            ),
            AuctionItem(
                id = "3",
                title = "Chaqueta Cuero Retro",
                description = "Vintage, Talla L, Unisex, 100% cuero genuino, acabado clásico.",
                currentBid = 180.00,
                bidCount = 8,
                timeRemaining = "45m 12s",
                imageRes = R.drawable.img_chaqueta,
                category = "Moda",
                imageUrl = "/uploads/img_chaqueta.png"
            ),
            AuctionItem(
                id = "4",
                title = "Oleo Abstracto",
                description = "Autor firmado, 120x80cm, técnica mixta sobre lienzo con detalles dorados.",
                currentBid = 620.00,
                bidCount = 10,
                timeRemaining = "05h 22m",
                imageRes = R.drawable.img_oleo,
                category = "Arte",
                imageUrl = "/uploads/img_oleo.png"
            )
        )
    }

    fun getAllAuctions(): List<AuctionItem> {
        val p = prefs ?: return getDefaultInitialAuctions()
        val json = p.getString(KEY_ALL_AUCTIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AuctionItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("LocalAuctionCache", "Error reading all auctions: ${e.message}")
            emptyList()
        }
    }

    fun saveAllAuctions(auctions: List<AuctionItem>) {
        val p = prefs ?: return
        try {
            val json = gson.toJson(auctions)
            p.edit().putString(KEY_ALL_AUCTIONS, json).apply()
        } catch (e: Exception) {
            Log.e("LocalAuctionCache", "Error saving all auctions: ${e.message}")
        }
    }

    private fun getUserMyAuctionsKey(userId: String): String {
        return if (userId.isNotBlank()) "${KEY_MY_AUCTIONS}_$userId" else KEY_MY_AUCTIONS
    }

    fun getMyAuctions(userId: String = ""): List<AuctionItem> {
        val p = prefs ?: return emptyList()
        val key = getUserMyAuctionsKey(userId)
        val json = p.getString(key, null)
            ?: if (userId.isNotBlank()) p.getString(KEY_MY_AUCTIONS, null) else null
            ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AuctionItem>>() {}.type
            val list: List<AuctionItem> = gson.fromJson(json, type) ?: emptyList()
            if (userId.isNotBlank()) {
                list.filter { it.creatorId == null || it.creatorId == userId }
            } else {
                list
            }
        } catch (e: Exception) {
            Log.e("LocalAuctionCache", "Error reading my auctions: ${e.message}")
            emptyList()
        }
    }

    fun saveMyAuctions(userId: String = "", auctions: List<AuctionItem>) {
        val p = prefs ?: return
        val key = getUserMyAuctionsKey(userId)
        try {
            val json = gson.toJson(auctions)
            p.edit().putString(key, json).apply()
        } catch (e: Exception) {
            Log.e("LocalAuctionCache", "Error saving my auctions: ${e.message}")
        }
    }

    fun addAuction(auction: AuctionItem, isMine: Boolean = true, userId: String = "") {
        // 1. Agregar a todas las subastas al principio
        val currentAll = getAllAuctions().toMutableList()
        currentAll.removeAll { it.id == auction.id }
        currentAll.add(0, auction)
        saveAllAuctions(currentAll)

        // 2. Si es del usuario actual, agregar a sus subastas aisladas
        if (isMine) {
            val targetUid = if (userId.isNotBlank()) userId else (auction.creatorId ?: "")
            val currentMine = getMyAuctions(targetUid).toMutableList()
            currentMine.removeAll { it.id == auction.id }
            currentMine.add(0, auction)
            saveMyAuctions(targetUid, currentMine)
        }
    }

    fun updateAuction(updated: AuctionItem, userId: String = "") {
        val currentAll = getAllAuctions().toMutableList()
        val indexAll = currentAll.indexOfFirst { it.id == updated.id }
        if (indexAll != -1) {
            currentAll[indexAll] = updated
            saveAllAuctions(currentAll)
        }

        val targetUid = if (userId.isNotBlank()) userId else (updated.creatorId ?: "")
        val currentMine = getMyAuctions(targetUid).toMutableList()
        val indexMine = currentMine.indexOfFirst { it.id == updated.id }
        if (indexMine != -1) {
            currentMine[indexMine] = updated
            saveMyAuctions(targetUid, currentMine)
        }
    }

    fun deleteAuction(auctionId: String, userId: String = "") {
        val currentAll = getAllAuctions().toMutableList()
        currentAll.removeAll { it.id == auctionId }
        saveAllAuctions(currentAll)

        val currentMine = getMyAuctions(userId).toMutableList()
        currentMine.removeAll { it.id == auctionId }
        saveMyAuctions(userId, currentMine)
    }
}
