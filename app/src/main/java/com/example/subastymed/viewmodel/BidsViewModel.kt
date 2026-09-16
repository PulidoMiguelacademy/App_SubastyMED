package com.example.subastymed.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subastymed.R
import com.example.subastymed.model.BidItem
import com.example.subastymed.network.LoginRequestDto
import com.example.subastymed.network.RetrofitClient
import com.example.subastymed.network.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BidsViewModel : ViewModel() {

    // Estado del filtro seleccionado ("Activas" por defecto)
    private val _selectedFilter = MutableStateFlow("Activas")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Lista completa de pujas del usuario
    private val _allBids = MutableStateFlow<List<BidItem>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        ensureAuthAndFetchBids()
    }

    // Combina la lista total con el filtro seleccionado
    val filteredBids: StateFlow<List<BidItem>> = combine(
        _allBids, _selectedFilter
    ) { bids, filter ->
        bids.filter { it.filterGroup.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun refresh() {
        ensureAuthAndFetchBids()
    }

    private fun ensureAuthAndFetchBids() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Si aún no tenemos sesión iniciada, hacemos login automático con la cuenta de prueba
                if (SessionManager.authToken == null) {
                    val loginRes = RetrofitClient.apiService.login(
                        LoginRequestDto(
                            usernameOrEmail = "alejandro",
                            password = "password123"
                        )
                    )
                    SessionManager.authToken = loginRes.accessToken
                    SessionManager.currentUser = loginRes.user
                    Log.d("BidsViewModel", "Sesión iniciada con: ${loginRes.user.fullName}")
                }

                // Consultar las pujas del usuario
                val dtoList = RetrofitClient.apiService.getMyBids()
                val items = dtoList.map { dto ->
                    val (textColor, bgColor) = when (dto.status) {
                        "Ganando" -> Pair(Color(0xFF4ADE80), Color(0xFF14532D))
                        "Superado" -> Pair(Color(0xFFFBBF24), Color(0xFF78350F))
                        "Ganada" -> Pair(Color(0xFFA78BFA), Color(0xFF312E81))
                        else -> Pair(Color(0xFFF87171), Color(0xFF450A0A))
                    }

                    BidItem(
                        id = dto.id,
                        title = dto.title,
                        yourBid = dto.yourBid,
                        maxBid = dto.maxBid,
                        status = dto.status,
                        filterGroup = dto.filterGroup,
                        statusTextColor = textColor,
                        statusBgColor = bgColor,
                        imageRes = getFallbackDrawable(dto.title),
                        imageUrl = dto.imageUrl
                    )
                }
                _allBids.value = items
                Log.d("BidsViewModel", "Cargadas ${items.size} pujas del usuario")
            } catch (e: Exception) {
                Log.e("BidsViewModel", "Error al obtener pujas: ${e.message}", e)
                _errorMessage.value = "Error al obtener pujas: ${e.localizedMessage}"
                if (_allBids.value.isEmpty()) {
                    loadMockBidsFallback()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getFallbackDrawable(title: String): Int {
        return when {
            title.contains("BMW", ignoreCase = true) -> R.drawable.img_bmw
            title.contains("Chaqueta", ignoreCase = true) -> R.drawable.img_chaqueta
            title.contains("Oleo", ignoreCase = true) -> R.drawable.img_oleo
            else -> R.drawable.img_macbook
        }
    }

    private fun loadMockBidsFallback() {
        _allBids.value = listOf(
            BidItem(
                id = "1",
                title = "MacBook Pro 16\" M4",
                yourBid = "$1,250.00",
                maxBid = "$1,250.00",
                status = "Ganando",
                filterGroup = "Activas",
                statusTextColor = Color(0xFF4ADE80),
                statusBgColor = Color(0xFF14532D),
                imageRes = R.drawable.img_macbook
            ),
            BidItem(
                id = "2",
                title = "BMW Serie 3 2021",
                yourBid = "$24,200.00",
                maxBid = "$24,800.00",
                status = "Superado",
                filterGroup = "Activas",
                statusTextColor = Color(0xFFFBBF24),
                statusBgColor = Color(0xFF78350F),
                imageRes = R.drawable.img_bmw
            ),
            BidItem(
                id = "3",
                title = "Chaqueta Cuero Retro",
                yourBid = "$180.00",
                maxBid = "$180.00",
                status = "Ganada",
                filterGroup = "Ganadas",
                statusTextColor = Color(0xFFA78BFA),
                statusBgColor = Color(0xFF312E81),
                imageRes = R.drawable.img_chaqueta
            )
        )
    }
}