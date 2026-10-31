package com.example.subastymed.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subastymed.R
import com.example.subastymed.model.AuctionItem
import com.example.subastymed.network.LoginRequestDto
import com.example.subastymed.network.RetrofitClient
import com.example.subastymed.network.SessionManager
import com.example.subastymed.network.UpdateAuctionDto
import com.example.subastymed.network.UserDto
import com.example.subastymed.network.UserStatsDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    private val _stats = MutableStateFlow(UserStatsDto(wonCount = 0, activeBidsCount = 0, favoritesCount = 0))
    val stats: StateFlow<UserStatsDto> = _stats.asStateFlow()

    // Subastas creadas por el usuario
    private val _myAuctions = MutableStateFlow<List<AuctionItem>>(emptyList())
    val myAuctions: StateFlow<List<AuctionItem>> = _myAuctions.asStateFlow()

    // Datos del Panel de Administración
    private val _allUsers = MutableStateFlow<List<UserDto>>(emptyList())
    val allUsers: StateFlow<List<UserDto>> = _allUsers.asStateFlow()

    private val _allAuctions = MutableStateFlow<List<AuctionItem>>(emptyList())
    val allAuctions: StateFlow<List<AuctionItem>> = _allAuctions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _notifications = MutableStateFlow<List<com.example.subastymed.model.AppNotification>>(emptyList())
    val notifications: StateFlow<List<com.example.subastymed.model.AppNotification>> = _notifications.asStateFlow()
    private var notifRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadUserProfile()
        loadMyAuctions()
    }

    fun refresh() {
        loadUserProfile()
        loadMyAuctions()
        if (_user.value?.isAdmin == true) {
            loadAdminData()
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _user.value = SessionManager.currentUser

                // Si Firebase Auth tiene un usuario activo, refrescar desde Firestore
                val currentFbUser = com.example.subastymed.network.FirebaseManager.auth.currentUser
                if (currentFbUser != null) {
                    try {
                        val doc = com.example.subastymed.network.FirebaseManager.firestore
                            .collection("users").document(currentFbUser.uid).get().await()
                        if (doc.exists()) {
                            val userEmail = doc.getString("email") ?: _user.value?.email ?: ""
                            val userUsername = doc.getString("username") ?: _user.value?.username ?: "usuario"
                            val userRole = doc.getString("role") ?: if (userEmail.equals("admin@subastymed.com", ignoreCase = true) || userUsername.equals("admin", ignoreCase = true)) "admin" else "user"

                            val refreshed = UserDto(
                                id = currentFbUser.uid.hashCode(),
                                username = userUsername,
                                email = userEmail,
                                fullName = doc.getString("full_name") ?: _user.value?.fullName ?: "Usuario",
                                avatarUrl = doc.getString("avatar_url") ?: _user.value?.avatarUrl,
                                rating = doc.getDouble("rating") ?: 5.0,
                                memberSince = doc.getString("member_since") ?: "2026",
                                phone = doc.getString("phone"),
                                city = doc.getString("city"),
                                bio = doc.getString("bio"),
                                idDocument = doc.getString("id_document"),
                                isVerified = doc.getBoolean("is_verified") ?: false,
                                role = userRole,
                                uid = currentFbUser.uid
                            )
                            _user.value = refreshed
                            SessionManager.currentUser = refreshed
                        }
                    } catch (fbErr: Exception) {
                        Log.w("ProfileViewModel", "Aviso refrescando usuario Firebase: ${fbErr.message}")
                    }
                }

                // Calcular estadísticas 100% reales desde Firestore
                val targetUid = currentFbUser?.uid ?: SessionManager.currentUser?.uid ?: ""
                if (targetUid.isNotBlank()) {
                    try {
                        val bidsSnap = com.example.subastymed.network.FirebaseManager.firestore
                            .collection("bids")
                            .whereEqualTo("bidderId", targetUid)
                            .get()
                            .await()

                        val won = bidsSnap.documents.count { doc ->
                            val s = doc.getString("status") ?: ""
                            val f = doc.getString("filterGroup") ?: ""
                            s.equals("Ganada", ignoreCase = true) || f.equals("Ganadas", ignoreCase = true)
                        }
                        val active = bidsSnap.documents.count { doc ->
                            val s = doc.getString("status") ?: ""
                            val f = doc.getString("filterGroup") ?: ""
                            s.equals("Ganando", ignoreCase = true) || s.equals("Superado", ignoreCase = true) || f.equals("Activas", ignoreCase = true)
                        }
                        _stats.value = UserStatsDto(wonCount = won, activeBidsCount = active, favoritesCount = _myAuctions.value.size)
                    } catch (statEx: Exception) {
                        Log.w("ProfileViewModel", "Aviso calculando estadísticas: ${statEx.message}")
                    }

                    // Escuchar notificaciones reales del usuario
                    notifRegistration?.remove()
                    notifRegistration = com.example.subastymed.network.FirebaseManager.listenToNotifications(targetUid) { list ->
                        _notifications.value = list
                    }
                }

                if (SessionManager.authToken != null) {
                    try {
                        val statsRes = RetrofitClient.apiService.getUserStats()
                        if (_stats.value.wonCount == 0 && _stats.value.activeBidsCount == 0) {
                            _stats.value = statsRes
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error cargando estadísticas de perfil: ${e.message}")
            }
        }
    }

    // Métodos para el Administrador
    fun loadAdminData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val usersRes = com.example.subastymed.network.FirebaseManager.getAllUsers()
                if (usersRes.isSuccess) {
                    _allUsers.value = usersRes.getOrThrow()
                }
                val auctionsRes = com.example.subastymed.network.FirebaseManager.getAllAuctionsAdmin()
                if (auctionsRes.isSuccess) {
                    _allAuctions.value = auctionsRes.getOrThrow()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error cargando datos de admin: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleUserVerification(userDto: UserDto) {
        viewModelScope.launch {
            val targetUid = userDto.uid ?: userDto.id.toString()
            val newStatus = !userDto.isVerified
            val res = com.example.subastymed.network.FirebaseManager.toggleUserVerification(targetUid, newStatus)
            if (res.isSuccess) {
                _allUsers.value = _allUsers.value.map {
                    if ((it.uid == targetUid) || (it.id == userDto.id)) it.copy(isVerified = newStatus) else it
                }
                _actionMessage.value = if (newStatus) "Usuario verificado" else "Verificación removida"
            } else {
                _actionMessage.value = "Error actualizando verificación"
            }
        }
    }

    fun deleteUser(userDto: UserDto) {
        viewModelScope.launch {
            val targetUid = userDto.uid ?: userDto.id.toString()
            val res = com.example.subastymed.network.FirebaseManager.deleteUserByAdmin(targetUid)
            if (res.isSuccess) {
                _allUsers.value = _allUsers.value.filterNot { it.uid == targetUid || it.id == userDto.id }
                _actionMessage.value = "Usuario eliminado con éxito"
            } else {
                _actionMessage.value = "Error al eliminar usuario"
            }
        }
    }

    fun deleteAnyAuction(auctionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            com.example.subastymed.model.LocalAuctionCache.deleteAuction(auctionId)
            try {
                com.example.subastymed.network.FirebaseManager.deleteAuction(auctionId)
                _allAuctions.value = _allAuctions.value.filterNot { it.id == auctionId }
                _myAuctions.value = _myAuctions.value.filterNot { it.id == auctionId }
                _actionMessage.value = "Subasta eliminada con éxito (Admin)"
            } catch (e: Exception) {
                _actionMessage.value = "Error eliminando subasta: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAnyAuction(
        auctionId: String,
        title: String,
        description: String,
        price: Double,
        category: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                com.example.subastymed.network.FirebaseManager.updateAuction(
                    auctionId, title, description, price, category
                )
                _allAuctions.value = _allAuctions.value.map {
                    if (it.id == auctionId) it.copy(
                        title = title,
                        description = description,
                        currentBid = price,
                        category = category
                    ) else it
                }
                _myAuctions.value = _myAuctions.value.map {
                    if (it.id == auctionId) it.copy(
                        title = title,
                        description = description,
                        currentBid = price,
                        category = category
                    ) else it
                }
                _actionMessage.value = "Subasta modificada con éxito (Admin)"
            } catch (e: Exception) {
                _actionMessage.value = "Error modificando subasta: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMyAuctions() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentFbUser = com.example.subastymed.network.FirebaseManager.auth.currentUser
            val currentUid = currentFbUser?.uid ?: SessionManager.currentUser?.uid
            val currentUsername = SessionManager.currentUser?.username
            val userKey = currentUid ?: currentUsername ?: ""

            // 1. Mostrar de inmediato lo que está guardado localmente para este usuario
            val localMine = com.example.subastymed.model.LocalAuctionCache.getMyAuctions(userKey)
            if (localMine.isNotEmpty()) {
                _myAuctions.value = localMine
            }

            // 2. Consultar en Firestore las subastas creadas específicamente por este usuario (sesión dividida)
            if (!currentUid.isNullOrBlank()) {
                try {
                    val snapshot = com.example.subastymed.network.FirebaseManager.firestore
                        .collection("auctions")
                        .whereEqualTo("creatorId", currentUid)
                        .get()
                        .await()

                    val cloudMine = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val title = doc.getString("title") ?: "Sin título"
                            val description = doc.getString("description") ?: ""
                            val category = doc.getString("category") ?: "Otros"
                            val startingPrice = doc.getDouble("startingPrice") ?: 0.0
                            val currentBid = doc.getDouble("currentBid") ?: startingPrice
                            val bidCount = doc.getLong("bidCount")?.toInt() ?: 0
                            val timeRemaining = doc.getString("timeRemaining") ?: "24h 00m"
                            val imageUrl = doc.getString("imageUrl")
                            val formattedPrice = doc.getString("formattedPrice")
                            val creatorId = doc.getString("creatorId") ?: currentUid
                            val creatorName = doc.getString("creatorName") ?: ""

                            AuctionItem(
                                id = id,
                                title = title,
                                description = description,
                                currentBid = currentBid,
                                bidCount = bidCount,
                                timeRemaining = timeRemaining,
                                imageRes = getFallbackDrawable(category),
                                category = category,
                                imageUrl = imageUrl,
                                formattedPrice = formattedPrice,
                                creatorId = creatorId,
                                creatorName = creatorName
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (cloudMine.isNotEmpty() || localMine.isEmpty()) {
                        _myAuctions.value = cloudMine
                        com.example.subastymed.model.LocalAuctionCache.saveMyAuctions(userKey, cloudMine)
                    }
                    _stats.value = _stats.value.copy(favoritesCount = _myAuctions.value.size)
                } catch (fbErr: Exception) {
                    Log.w("ProfileViewModel", "Aviso consultando subastas de Firestore: ${fbErr.message}")
                }
            }

            // 3. Fallback / Sincronización con Backend FastAPI si aplica
            try {
                if (SessionManager.authToken != null) {
                    val dtoList = RetrofitClient.apiService.getMyAuctions()
                    val serverItems = dtoList.map { dto ->
                        AuctionItem(
                            id = dto.id.toString(),
                            title = dto.title,
                            description = dto.description,
                            currentBid = dto.currentBid,
                            bidCount = dto.bidCount,
                            timeRemaining = dto.timeRemaining,
                            imageRes = getFallbackDrawable(dto.category),
                            category = dto.category,
                            imageUrl = dto.imageUrl,
                            creatorId = currentUid
                        )
                    }
                    val merged = (_myAuctions.value + serverItems).distinctBy { it.id }
                    _myAuctions.value = merged
                    com.example.subastymed.model.LocalAuctionCache.saveMyAuctions(userKey, merged)
                }
            } catch (e: Exception) {
                Log.w("ProfileViewModel", "Sin conexión al servidor para mis subastas, usando locales/Firestore: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAuction(
        id: String,
        title: String,
        description: String,
        newOfferPrice: Double,
        category: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentFbUser = com.example.subastymed.network.FirebaseManager.auth.currentUser
            val currentUid = currentFbUser?.uid ?: SessionManager.currentUser?.uid ?: ""

            // 1. Actualizar inmediatamente en almacenamiento local
            val currentItem = _myAuctions.value.find { it.id == id }
            if (currentItem != null) {
                val updated = currentItem.copy(
                    title = title,
                    description = description,
                    currentBid = newOfferPrice,
                    category = category
                )
                com.example.subastymed.model.LocalAuctionCache.updateAuction(updated, currentUid)
            }

            // 2. Actualizar en Firebase Firestore (soporta IDs alfanuméricos)
            try {
                val fbRes = com.example.subastymed.network.FirebaseManager.updateAuction(
                    id, title, description, newOfferPrice, category
                )
                if (fbRes.isSuccess) {
                    _actionMessage.value = "Subasta modificada con éxito"
                }
            } catch (fbEx: Exception) {
                Log.w("ProfileViewModel", "Firebase updateAuction: ${fbEx.message}")
            }

            // 3. Si hay Backend FastAPI y el ID es numérico, sincronizarlo
            val idInt = id.toIntOrNull()
            if (idInt != null && SessionManager.authToken != null) {
                try {
                    val updateRequest = UpdateAuctionDto(
                        title = title,
                        description = description,
                        category = category,
                        currentBid = newOfferPrice,
                        startingPrice = newOfferPrice
                    )
                    RetrofitClient.apiService.updateAuction(idInt, updateRequest)
                } catch (e: Exception) {
                    Log.w("ProfileViewModel", "Aviso sincronización backend: ${e.message}")
                }
            }

            loadMyAuctions()
            _isLoading.value = false
            onSuccess()
        }
    }

    fun deleteAuction(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentFbUser = com.example.subastymed.network.FirebaseManager.auth.currentUser
            val currentUid = currentFbUser?.uid ?: SessionManager.currentUser?.uid ?: ""

            // 1. Eliminar inmediatamente en almacenamiento local
            com.example.subastymed.model.LocalAuctionCache.deleteAuction(id, currentUid)

            // 2. Eliminar en Firebase Firestore (soporta IDs alfanuméricos)
            try {
                val fbRes = com.example.subastymed.network.FirebaseManager.deleteAuction(id)
                if (fbRes.isSuccess) {
                    _actionMessage.value = "Subasta eliminada con éxito"
                }
            } catch (fbEx: Exception) {
                Log.w("ProfileViewModel", "Firebase deleteAuction: ${fbEx.message}")
            }

            // 3. Si hay Backend FastAPI y el ID es numérico, sincronizarlo
            val idInt = id.toIntOrNull()
            if (idInt != null && SessionManager.authToken != null) {
                try {
                    RetrofitClient.apiService.deleteAuction(idInt)
                } catch (e: Exception) {
                    Log.w("ProfileViewModel", "Aviso backend eliminación: ${e.message}")
                }
            }

            loadMyAuctions()
            _isLoading.value = false
            onSuccess()
        }
    }

    fun logout(context: android.content.Context, onSuccess: () -> Unit) {
        com.example.subastymed.network.FirebaseManager.logout(context)
        _user.value = null
        _myAuctions.value = emptyList()
        _actionMessage.value = "Sesión cerrada"
        onSuccess()
    }

    fun updateProfile(
        context: android.content.Context,
        fullName: String,
        phone: String?,
        city: String?,
        bio: String?,
        idDocument: String?,
        newAvatarUri: android.net.Uri?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = com.example.subastymed.network.FirebaseManager.updateUserProfile(
                    context, fullName, phone, city, bio, idDocument, newAvatarUri
                )
                if (res.isSuccess) {
                    val updated = res.getOrThrow()
                    _user.value = updated
                    _actionMessage.value = "Perfil actualizado con éxito"
                    onSuccess()
                } else {
                    _actionMessage.value = "No se pudo actualizar el perfil"
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error actualizando perfil: ${e.message}")
                _actionMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getFallbackDrawable(category: String): Int {
        return when (category.lowercase()) {
            "vehículos", "vehiculos" -> R.drawable.img_bmw
            "moda" -> R.drawable.img_chaqueta
            "arte" -> R.drawable.img_oleo
            else -> R.drawable.img_macbook
        }
    }

    override fun onCleared() {
        super.onCleared()
        notifRegistration?.remove()
    }
}
