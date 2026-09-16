package com.example.subastymed.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subastymed.network.CreateAuctionDto
import com.example.subastymed.network.LoginRequestDto
import com.example.subastymed.network.RetrofitClient
import com.example.subastymed.network.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateAuctionViewModel : ViewModel() {

    var title = MutableStateFlow("")
    var description = MutableStateFlow("")
    var startPrice = MutableStateFlow("")
    var selectedCategory = MutableStateFlow("Bicicletas")
    
    // Fecha y hora de inicio formateada
    private val nowFormatted: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault())
            return sdf.format(Date())
        }

    var startDate = MutableStateFlow(nowFormatted)

    // URI local de la imagen seleccionada desde la galería
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setStartDateTime(newDateTime: String) {
        startDate.value = newDateTime
    }

    fun createAuction(context: Context, onSuccess: () -> Unit) {
        val currentTitle = title.value.trim()
        val currentDesc = description.value.trim()
        val priceText = startPrice.value.trim()
            .replace("$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
        val price = priceText.toDoubleOrNull()

        if (currentTitle.isEmpty()) {
            _statusMessage.value = "Por favor ingresa el título del artículo"
            return
        }
        if (price == null || price <= 0) {
            _statusMessage.value = "Por favor ingresa un precio inicial válido"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _statusMessage.value = null
            try {
                // 1. Asegurar sesión iniciada en FastAPI
                if (SessionManager.authToken == null) {
                    val loginRes = RetrofitClient.apiService.login(
                        LoginRequestDto(
                            usernameOrEmail = "alejandro",
                            password = "password123"
                        )
                    )
                    SessionManager.authToken = loginRes.accessToken
                    SessionManager.currentUser = loginRes.user
                }

                // 2. Subir imagen al servidor si el usuario seleccionó una foto
                var serverImageUrl: String? = null
                val imageUri = _selectedImageUri.value
                if (imageUri != null) {
                    try {
                        val filePart = uriToMultipart(context, imageUri)
                        val uploadResponse = RetrofitClient.apiService.uploadImage(filePart)
                        serverImageUrl = uploadResponse.url
                        Log.d("CreateAuctionVM", "Imagen subida exitosamente: $serverImageUrl")
                    } catch (uploadErr: Exception) {
                        Log.e("CreateAuctionVM", "Error al subir imagen: ${uploadErr.message}", uploadErr)
                        serverImageUrl = getFallbackImage(selectedCategory.value)
                    }
                } else {
                    serverImageUrl = getFallbackImage(selectedCategory.value)
                }

                // 3. Crear la subasta en la base de datos con la fecha y hora de inicio seleccionada
                val newAuction = CreateAuctionDto(
                    title = currentTitle,
                    description = if (currentDesc.isEmpty()) "Sin descripción detallada" else currentDesc,
                    category = selectedCategory.value,
                    startingPrice = price,
                    timeRemaining = startDate.value.ifEmpty { nowFormatted },
                    imageUrl = serverImageUrl
                )

                val response = RetrofitClient.apiService.createAuction(newAuction)
                Log.d("CreateAuctionVM", "Subasta creada con éxito: id=${response.id}")

                // 4. Limpiar campos
                title.value = ""
                description.value = ""
                startPrice.value = ""
                _selectedImageUri.value = null
                startDate.value = nowFormatted
                _statusMessage.value = "¡Subasta creada con éxito!"

                onSuccess()
            } catch (e: Exception) {
                Log.e("CreateAuctionVM", "Error al crear subasta: ${e.message}", e)
                _statusMessage.value = "Error al crear subasta: ${e.localizedMessage}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("No se pudo leer la imagen")
        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val extension = if (mimeType.contains("png")) "png" else "jpg"
        val fileName = "subasta_${System.currentTimeMillis()}.$extension"

        return MultipartBody.Part.createFormData("file", fileName, requestBody)
    }

    private fun getFallbackImage(category: String): String {
        return when (category.lowercase()) {
            "vehículos", "vehiculos" -> "/uploads/img_bmw.png"
            "moda" -> "/uploads/img_chaqueta.png"
            "arte" -> "/uploads/img_oleo.png"
            else -> "/uploads/img_macbook.png"
        }
    }
}
