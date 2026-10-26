package com.example.subastymed.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subastymed.R
import com.example.subastymed.model.AuctionItem
import com.example.subastymed.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    // 1. La lista original de todos los productos
    private val _allAuctions = MutableStateFlow<List<AuctionItem>>(emptyList())

    // 2. Lo que el usuario escribe en el buscador
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 3. La categoría seleccionada ("Todos" por defecto)
    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // 4. Estados de carga y error
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Carga inmediata desde almacenamiento local en el teléfono
        val cached = com.example.subastymed.model.LocalAuctionCache.getAllAuctions()
        if (cached.isNotEmpty()) {
            _allAuctions.value = cached
        }
        listenToFirebaseAuctions()
        fetchAuctionsFromApi()
    }

    private fun listenToFirebaseAuctions() {
        try {
            com.example.subastymed.network.FirebaseManager.listenToAuctions(
                onUpdate = { fbList ->
                    if (fbList.isNotEmpty()) {
                        _allAuctions.value = fbList
                    }
                },
                onError = { err ->
                    Log.w("HomeViewModel", "Firestore listener: ${err.message}")
                }
            )
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Error iniciando Firestore listener: ${e.message}")
        }
    }

    // 5. Combina la lista, la búsqueda y la categoría en memoria para filtrado instantáneo
    val filteredAuctions: StateFlow<List<AuctionItem>> = combine(
        _allAuctions, _searchQuery, _selectedCategory
    ) { auctions, query, category ->
        auctions.filter { item ->
            val matchesCategory = category == "Todos" || item.category.equals(category, ignoreCase = true)
            val matchesQuery = item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(newCategory: String) {
        _selectedCategory.value = newCategory
    }

    fun refresh() {
        // Primero refrescamos con lo que hay guardado localmente
        val localItems = com.example.subastymed.model.LocalAuctionCache.getAllAuctions()
        if (localItems.isNotEmpty()) {
            _allAuctions.value = localItems
        }
        fetchAuctionsFromApi()
    }

    // Carga las subastas desde el Backend FastAPI en Python o mantiene las locales guardadas
    fun fetchAuctionsFromApi() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val dtoList = RetrofitClient.apiService.getAuctions()
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
                        imageUrl = dto.imageUrl
                    )
                }

                // Fusionar con subastas locales que no estén en el servidor
                val myLocal = com.example.subastymed.model.LocalAuctionCache.getAllAuctions()
                val merged = (myLocal + serverItems).distinctBy { it.id }

                _allAuctions.value = merged
                com.example.subastymed.model.LocalAuctionCache.saveAllAuctions(merged)
                Log.d("HomeViewModel", "Cargadas ${merged.size} subastas sincronizadas")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error al conectar con Backend: ${e.message}", e)
                _errorMessage.value = "Modo sin conexión: mostrando subastas guardadas"
                // Si falla la conexión, aseguramos cargar lo que está guardado en el teléfono
                val localItems = com.example.subastymed.model.LocalAuctionCache.getAllAuctions()
                if (localItems.isNotEmpty()) {
                    _allAuctions.value = localItems
                }
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
}