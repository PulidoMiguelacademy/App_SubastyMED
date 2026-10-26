package com.example.subastymed.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subastymed.model.BidItem
import com.example.subastymed.network.FirebaseManager
import com.example.subastymed.network.SessionManager
import com.google.firebase.firestore.ListenerRegistration
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

    // Lista completa de pujas del usuario autenticado
    private val _allBids = MutableStateFlow<List<BidItem>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var bidsListener: ListenerRegistration? = null

    init {
        loadUserBids()
    }

    // Combina la lista total de pujas del usuario con el filtro seleccionado
    val filteredBids: StateFlow<List<BidItem>> = combine(
        _allBids, _selectedFilter
    ) { bids, filter ->
        bids.filter { it.filterGroup.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun refresh() {
        loadUserBids()
    }

    private fun loadUserBids() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val currentFbUser = FirebaseManager.auth.currentUser
            val currentUid = currentFbUser?.uid ?: SessionManager.currentUser?.uid

            if (currentUid.isNullOrBlank()) {
                _allBids.value = emptyList()
                _isLoading.value = false
                return@launch
            }

            bidsListener?.remove()
            try {
                bidsListener = FirebaseManager.listenToUserBids(
                    userId = currentUid,
                    onUpdate = { userBids ->
                        _allBids.value = userBids
                        _isLoading.value = false
                        Log.d("BidsViewModel", "Pujas en tiempo real cargadas para $currentUid: ${userBids.size}")
                    },
                    onError = { err ->
                        Log.e("BidsViewModel", "Error al escuchar pujas de Firestore: ${err.message}")
                        _errorMessage.value = "Error al conectar con el servidor de pujas"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e("BidsViewModel", "Error iniciando listener de pujas: ${e.message}", e)
                _errorMessage.value = "Error: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bidsListener?.remove()
    }
}