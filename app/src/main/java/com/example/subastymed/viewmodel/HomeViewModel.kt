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
        fetchAuctionsFromApi()
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
        fetchAuctionsFromApi()
    }

    // Carga las subastas desde el Backend FastAPI en Python
    fun fetchAuctionsFromApi() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val dtoList = RetrofitClient.apiService.getAuctions()
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
                _allAuctions.value = items
                Log.d("HomeViewModel", "Cargadas ${items.size} subastas desde el Backend")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error al conectar con Backend: ${e.message}", e)
                _errorMessage.value = "No se pudo conectar al servidor: ${e.localizedMessage}"
                // Si falla la conexión, dejamos datos de respaldo para no romper la vista
                if (_allAuctions.value.isEmpty()) {
                    loadMockDataFallback()
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

    private fun loadMockDataFallback() {
        _allAuctions.value = listOf(
            AuctionItem("1", "MacBook Pro 16\" M4", "Excelente estado, 32GB...", 1250.00, 14, "02h 15m", R.drawable.img_macbook, "Electrónica"),
            AuctionItem("2", "BMW Serie 3 2021", "Impecable, 35,000 km", 24800.00, 45, "1d 04h", R.drawable.img_bmw, "Vehículos"),
            AuctionItem("3", "Chaqueta Cuero Retro", "Vintage, Talla L, Unisex", 180.00, 8, "45m 12s", R.drawable.img_chaqueta, "Moda"),
            AuctionItem("4", "Oleo Abstracto", "Autor firmado, 120x80cm", 620.00, 10, "05h 22m", R.drawable.img_oleo, "Arte")
        )
    }
}