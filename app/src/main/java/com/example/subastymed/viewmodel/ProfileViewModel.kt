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

class ProfileViewModel : ViewModel() {

    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    private val _stats = MutableStateFlow(UserStatsDto(wonCount = 14, activeBidsCount = 3, favoritesCount = 28))
    val stats: StateFlow<UserStatsDto> = _stats.asStateFlow()

    // Subastas creadas por el usuario
    private val _myAuctions = MutableStateFlow<List<AuctionItem>>(emptyList())
    val myAuctions: StateFlow<List<AuctionItem>> = _myAuctions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadUserProfile()
        loadMyAuctions()
    }

    fun refresh() {
        loadUserProfile()
        loadMyAuctions()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                if (SessionManager.authToken == null) {
                    val loginRes = RetrofitClient.apiService.login(
                        LoginRequestDto(usernameOrEmail = "alejandro", password = "password123")
                    )
                    SessionManager.authToken = loginRes.accessToken
                    SessionManager.currentUser = loginRes.user
                }

                _user.value = SessionManager.currentUser

                val statsRes = RetrofitClient.apiService.getUserStats()
                _stats.value = statsRes
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error cargando perfil: ${e.message}")
            }
        }
    }

    fun loadMyAuctions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (SessionManager.authToken == null) {
                    val loginRes = RetrofitClient.apiService.login(
                        LoginRequestDto(usernameOrEmail = "alejandro", password = "password123")
                    )
                    SessionManager.authToken = loginRes.accessToken
                    SessionManager.currentUser = loginRes.user
                }

                val dtoList = RetrofitClient.apiService.getMyAuctions()
                val items = dtoList.map { dto ->
                    AuctionItem(
                        id = dto.id.toString(),
                        title = dto.title,
                        description = dto.description,
                        currentBid = dto.currentBid,
                        bidCount = dto.bidCount,
                        timeRemaining = dto.timeRemaining,
                        imageRes = getFallbackDrawable(dto.category),
                        category = dto.category,
                        imageUrl = dto.imageUrl
                    )
                }
                _myAuctions.value = items
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al cargar mis subastas: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAuction(
        id: Int,
        title: String,
        description: String,
        newOfferPrice: Double,
        category: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updateRequest = UpdateAuctionDto(
                    title = title,
                    description = description,
                    category = category,
                    currentBid = newOfferPrice,
                    startingPrice = newOfferPrice
                )
                RetrofitClient.apiService.updateAuction(id, updateRequest)
                _actionMessage.value = "Subasta modificada con éxito"
                loadMyAuctions()
                loadUserProfile()
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al modificar subasta: ${e.message}")
                _actionMessage.value = "Error al modificar: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAuction(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                RetrofitClient.apiService.deleteAuction(id)
                _actionMessage.value = "Subasta eliminada con éxito"
                loadMyAuctions()
                loadUserProfile()
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al eliminar subasta: ${e.message}")
                _actionMessage.value = "Error al eliminar: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        SessionManager.authToken = null
        SessionManager.currentUser = null
        _user.value = null
        _myAuctions.value = emptyList()
        _actionMessage.value = "Sesión cerrada"
        onSuccess()
    }

    private fun getFallbackDrawable(category: String): Int {
        return when (category.lowercase()) {
            "vehículos", "vehiculos" -> R.drawable.img_bmw
            "moda" -> R.drawable.img_chaqueta
            "arte" -> R.drawable.img_oleo
            else -> R.drawable.img_macbook
        }
    }
}
