package com.example.subastymed.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subastymed.model.AuctionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel : ViewModel() {

    // 1. La lista original de todos los productos
    private val _allAuctions = MutableStateFlow<List<AuctionItem>>(emptyList())

    // 2. Lo que el usuario escribe en el buscador
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 3. La categoría seleccionada ("Todos" por defecto)
    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        loadMockData()
    }

    // 4. LA MAGIA: Combina la lista, la búsqueda y la categoría
    val filteredAuctions: StateFlow<List<AuctionItem>> = combine(
        _allAuctions, _searchQuery, _selectedCategory
    ) { auctions, query, category ->
        auctions.filter { item ->
            // Filtra por categoría (si no es "Todos") Y por el texto de búsqueda
            val matchesCategory = category == "Todos" || item.category == category
            val matchesQuery = item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Funciones para actualizar los estados
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(newCategory: String) {
        _selectedCategory.value = newCategory
    }

    private fun loadMockData() {
        _allAuctions.value = listOf(
            AuctionItem("1", "MacBook Pro 16\" M4", "Excelente estado, 32GB...", 1250.00, 14, "02h 15m", com.example.subastymed.R.drawable.img_macbook, "Electrónica"),
            AuctionItem("2", "BMW Serie 3 2021", "Impecable, 35,000 km", 24800.00, 45, "1d 04h", com.example.subastymed.R.drawable.img_bmw, "Vehículos"),
            AuctionItem("3", "Chaqueta Cuero Retro", "Vintage, Talla L, Unisex", 180.00, 8, "45m 12s", com.example.subastymed.R.drawable.img_chaqueta, "Moda"),
            AuctionItem("4", "Oleo Abstracto", "Autor firmado, 120x80cm", 620.00, 10, "05h 22m", com.example.subastymed.R.drawable.img_oleo, "Arte")
        )
    }
}