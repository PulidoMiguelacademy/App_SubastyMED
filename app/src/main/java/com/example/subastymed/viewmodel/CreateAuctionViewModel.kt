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
            val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.US)
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

    // Ruta absoluta persistente en el almacenamiento interno privado del celular
    var persistentImagePath: String? = null
        private set

    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun onImageSelected(context: Context, uri: Uri?) {
        _selectedImageUri.value = uri
        if (uri == null) {
            persistentImagePath = null
            return
        }
        try {
            val file = java.io.File(context.filesDir, "subasta_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            persistentImagePath = file.absolutePath
            Log.d("CreateAuctionVM", "Imagen persistida localmente en: $persistentImagePath")
        } catch (e: Exception) {
            Log.e("CreateAuctionVM", "Error al guardar imagen interna: ${e.message}")
        }
    }

    fun setStartDateTime(newDateTime: String) {
        startDate.value = newDateTime
    }

    fun createAuction(context: Context, onSuccess: () -> Unit) {
        val currentTitle = title.value.trim()
        val currentDesc = description.value.trim()
        val rawPriceText = startPrice.value.trim().removePrefix("$").trim()

        // Permite ingresar números con puntos y comas (ej. 2.000.000, 1,500,000, 250000)
        val digits = rawPriceText.filter { it.isDigit() }
        val price = digits.toDoubleOrNull()

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

            val tempId = System.currentTimeMillis().toString()
            val initialImage = persistentImagePath ?: _selectedImageUri.value?.toString() ?: getFallbackImage(selectedCategory.value)

            // 1. Guardar de forma inmediata y persistente en el dispositivo con precio formateado y foto real
            val currentUid = com.example.subastymed.network.FirebaseManager.auth.currentUser?.uid
                ?: com.example.subastymed.network.SessionManager.currentUser?.uid
                ?: "anonimo"
            val currentUserName = com.example.subastymed.network.SessionManager.currentUser?.fullName ?: "Usuario"

            val chosenStart = startDate.value.ifEmpty { nowFormatted }
            val parsedStartMillis = com.example.subastymed.model.AuctionItem.parseDateStringToMillis(chosenStart)
            val startTimeMillis = if (parsedStartMillis > 0L) parsedStartMillis else System.currentTimeMillis()
            val endTimeMillis = startTimeMillis + com.example.subastymed.model.AuctionItem.ONE_HOUR_MILLIS

            var cachedAuction = com.example.subastymed.model.AuctionItem(
                id = tempId,
                title = currentTitle,
                description = if (currentDesc.isEmpty()) "Sin descripción detallada" else currentDesc,
                currentBid = price,
                bidCount = 0,
                timeRemaining = chosenStart,
                category = selectedCategory.value,
                imageUrl = initialImage,
                formattedPrice = rawPriceText,
                creatorId = currentUid,
                creatorName = currentUserName,
                status = "Activa",
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis
            )
            com.example.subastymed.model.LocalAuctionCache.addAuction(cachedAuction, isMine = true, userId = currentUid)

            // 2. Guardar en la Nube con Firebase (Cloud Firestore y Storage)
            try {
                val fbResult = com.example.subastymed.network.FirebaseManager.createAuction(
                    context = context,
                    title = currentTitle,
                    description = if (currentDesc.isEmpty()) "Sin descripción detallada" else currentDesc,
                    category = selectedCategory.value,
                    price = price,
                    startDate = chosenStart,
                    imageUri = _selectedImageUri.value,
                    localImagePath = persistentImagePath,
                    formattedPrice = rawPriceText
                )
                if (fbResult.isSuccess) {
                    val fbId = fbResult.getOrThrow()
                    com.example.subastymed.model.LocalAuctionCache.deleteAuction(tempId, currentUid)
                    cachedAuction = cachedAuction.copy(id = fbId)
                    Log.d("CreateAuctionVM", "Subasta guardada exitosamente en Firebase: $fbId")
                }
            } catch (fbEx: Exception) {
                Log.w("CreateAuctionVM", "Firebase createAuction aviso: ${fbEx.message}")
            }

            try {
                // 3. Intentar sincronizar con Backend FastAPI si está conectado
                if (SessionManager.authToken == null) {
                    val loginRes = RetrofitClient.apiService.login(
                        LoginRequestDto(
                            usernameOrEmail = SessionManager.currentUser?.username ?: "alejandro",
                            password = "password123"
                        )
                    )
                    SessionManager.saveSession(context, loginRes.accessToken, loginRes.user)
                }

                var serverImageUrl: String? = null
                val imageUri = _selectedImageUri.value
                if (imageUri != null) {
                    try {
                        val filePart = uriToMultipart(context, imageUri)
                        val uploadResponse = RetrofitClient.apiService.uploadImage(filePart)
                        serverImageUrl = uploadResponse.url
                        Log.d("CreateAuctionVM", "Imagen subida exitosamente: $serverImageUrl")
                    } catch (uploadErr: Exception) {
                        Log.e("CreateAuctionVM", "Error al subir imagen al servidor: ${uploadErr.message}")
                        serverImageUrl = initialImage
                    }
                } else {
                    serverImageUrl = initialImage
                }

                val newAuctionDto = CreateAuctionDto(
                    title = currentTitle,
                    description = if (currentDesc.isEmpty()) "Sin descripción detallada" else currentDesc,
                    category = selectedCategory.value,
                    startingPrice = price,
                    timeRemaining = startDate.value.ifEmpty { nowFormatted },
                    imageUrl = serverImageUrl
                )

                val response = RetrofitClient.apiService.createAuction(newAuctionDto)
                Log.d("CreateAuctionVM", "Subasta sincronizada con Backend: id=${response.id}")

                // Actualizar item local con ID del servidor
                com.example.subastymed.model.LocalAuctionCache.deleteAuction(tempId, currentUid)
                val syncedAuction = cachedAuction.copy(
                    id = response.id.toString(),
                    imageUrl = response.imageUrl ?: serverImageUrl,
                    creatorId = currentUid,
                    creatorName = currentUserName
                )
                com.example.subastymed.model.LocalAuctionCache.addAuction(syncedAuction, isMine = true, userId = currentUid)

                _statusMessage.value = "¡Subasta creada y guardada con éxito!"
            } catch (e: Exception) {
                Log.w("CreateAuctionVM", "No se pudo sincronizar con backend (${e.message}), guardada localmente.")
                _statusMessage.value = "¡Subasta guardada con éxito en tu teléfono!"
            } finally {
                // Limpiar campos y navegar
                title.value = ""
                description.value = ""
                startPrice.value = ""
                _selectedImageUri.value = null
                startDate.value = nowFormatted
                _isSubmitting.value = false
                onSuccess()
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
