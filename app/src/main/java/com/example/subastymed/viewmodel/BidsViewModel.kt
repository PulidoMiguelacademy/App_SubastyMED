package com.example.subastymed.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subastymed.R
import com.example.subastymed.model.BidItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BidsViewModel : ViewModel() {

    // Estado del filtro seleccionado ("Activas" por defecto)
    private val _selectedFilter = MutableStateFlow("Activas")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Lista completa de pujas del usuario
    private val _allBids = MutableStateFlow<List<BidItem>>(emptyList())

    init {
        loadMockBids()
    }

    // Combina la lista total con el filtro seleccionado
    val filteredBids: StateFlow<List<BidItem>> = combine(
        _allBids, _selectedFilter
    ) { bids, filter ->
        bids.filter { it.filterGroup == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    private fun loadMockBids() {
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