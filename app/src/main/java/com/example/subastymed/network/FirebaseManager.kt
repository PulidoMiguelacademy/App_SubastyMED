package com.example.subastymed.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.subastymed.R
import androidx.compose.ui.graphics.Color
import com.example.subastymed.model.AuctionItem
import com.example.subastymed.model.AuctionPhase
import com.example.subastymed.model.BidItem
import com.example.subastymed.model.LiveBidEntry
import com.example.subastymed.model.LocalAuctionCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun registerUser(
        context: Context,
        fullName: String,
        username: String,
        email: String,
        pass: String,
        avatarUri: Uri? = null
    ): Result<UserDto> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("No se pudo obtener el UID del usuario")

            var finalAvatar: String? = null
            if (avatarUri != null) {
                finalAvatar = compressImageToBase64(context, null, avatarUri)
            }
            if (finalAvatar == null) {
                finalAvatar = "/uploads/img_perfil.png"
            }

            val userMap = hashMapOf(
                "id" to uid.hashCode(),
                "uid" to uid,
                "full_name" to fullName,
                "username" to username,
                "email" to email,
                "rating" to 5.0,
                "member_since" to "2026",
                "avatar_url" to finalAvatar,
                "phone" to "",
                "city" to "",
                "bio" to "",
                "id_document" to "",
                "is_verified" to false,
                "role" to "user"
            )

            firestore.collection("users").document(uid).set(userMap).await()

            val userDto = UserDto(
                id = uid.hashCode(),
                username = username,
                email = email,
                fullName = fullName,
                avatarUrl = finalAvatar,
                rating = 5.0,
                memberSince = "2026",
                phone = "",
                city = "",
                bio = "",
                idDocument = "",
                isVerified = false,
                role = "user",
                uid = uid
            )

            SessionManager.saveSession(context, uid, userDto)
            Result.success(userDto)
        } catch (e: Exception) {
            Log.e(TAG, "Error en registro Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(
        context: Context,
        emailOrUsername: String,
        pass: String
    ): Result<UserDto> {
        return try {
            var targetEmail = emailOrUsername.trim()
            val isAdminAttempt = targetEmail.equals("admin", ignoreCase = true) ||
                    targetEmail.equals("admin@subastymed.com", ignoreCase = true)

            // Si es intento de admin, usamos el correo oficial de admin
            if (isAdminAttempt) {
                targetEmail = "admin@subastymed.com"
            } else if (!targetEmail.contains("@")) {
                // Si ingresó nombre de usuario en lugar de correo, buscamos el correo en Firestore
                val query = firestore.collection("users")
                    .whereEqualTo("username", targetEmail)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    targetEmail = query.documents[0].getString("email")
                        ?: throw IllegalArgumentException("El usuario no tiene un correo válido asociado")
                } else {
                    throw IllegalArgumentException("El usuario '$targetEmail' no está registrado")
                }
            }

            val authResult = try {
                auth.signInWithEmailAndPassword(targetEmail, pass).await()
            } catch (e: Exception) {
                if (isAdminAttempt && pass == "admin123") {
                    // Si la cuenta admin no existe aún en Firebase Auth, la auto-creamos
                    try {
                        auth.createUserWithEmailAndPassword("admin@subastymed.com", "admin123").await()
                    } catch (createEx: Exception) {
                        throw e
                    }
                } else {
                    throw e
                }
            }

            val uid = authResult.user?.uid ?: throw IllegalStateException("UID nulo")

            val userDocRef = firestore.collection("users").document(uid)
            var userDoc = userDocRef.get().await()

            // Si es admin o no existe el documento, aprovisionamos la información de Administrador
            if (isAdminAttempt || !userDoc.exists()) {
                val adminMap = hashMapOf<String, Any>(
                    "id" to uid.hashCode(),
                    "uid" to uid,
                    "full_name" to (userDoc.getString("full_name") ?: "Administrador SubastyMED"),
                    "username" to (userDoc.getString("username") ?: "admin"),
                    "email" to "admin@subastymed.com",
                    "rating" to 5.0,
                    "member_since" to "2026",
                    "avatar_url" to (userDoc.getString("avatar_url") ?: "/uploads/img_perfil.png"),
                    "phone" to (userDoc.getString("phone") ?: "+57 300 000 0000"),
                    "city" to (userDoc.getString("city") ?: "Medellín"),
                    "bio" to (userDoc.getString("bio") ?: "Cuenta oficial de administración del sistema"),
                    "id_document" to (userDoc.getString("id_document") ?: "ADMIN-001"),
                    "is_verified" to true,
                    "role" to "admin"
                )
                userDocRef.set(adminMap, com.google.firebase.firestore.SetOptions.merge()).await()
                userDoc = userDocRef.get().await()
            }

            val fullName = userDoc.getString("full_name") ?: if (isAdminAttempt) "Administrador SubastyMED" else "Usuario SubastyMED"
            val username = userDoc.getString("username") ?: if (isAdminAttempt) "admin" else targetEmail.substringBefore("@")
            val email = userDoc.getString("email") ?: targetEmail
            val rating = userDoc.getDouble("rating") ?: 5.0
            val memberSince = userDoc.getString("member_since") ?: "2026"
            val avatarUrl = userDoc.getString("avatar_url") ?: "/uploads/img_perfil.png"
            val phone = userDoc.getString("phone")
            val city = userDoc.getString("city")
            val bio = userDoc.getString("bio")
            val idDocument = userDoc.getString("id_document")
            val isVerified = userDoc.getBoolean("is_verified") ?: (isAdminAttempt || !idDocument.isNullOrBlank() || (!phone.isNullOrBlank() && !city.isNullOrBlank()))
            val role = userDoc.getString("role") ?: if (isAdminAttempt || email.equals("admin@subastymed.com", ignoreCase = true) || username.equals("admin", ignoreCase = true)) "admin" else "user"

            val userDto = UserDto(
                id = uid.hashCode(),
                username = username,
                email = email,
                fullName = fullName,
                avatarUrl = avatarUrl,
                rating = rating,
                memberSince = memberSince,
                phone = phone,
                city = city,
                bio = bio,
                idDocument = idDocument,
                isVerified = isVerified,
                role = role,
                uid = uid
            )

            SessionManager.saveSession(context, uid, userDto)
            Result.success(userDto)
        } catch (e: Exception) {
            Log.e(TAG, "Error en inicio de sesión Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        context: Context,
        fullName: String,
        phone: String?,
        city: String?,
        bio: String?,
        idDocument: String?,
        newAvatarUri: Uri? = null
    ): Result<UserDto> {
        return try {
            val uid = auth.currentUser?.uid ?: SessionManager.currentUser?.id?.toString() ?: "current_user"
            val current = SessionManager.currentUser

            var newAvatarUrl: String? = null
            if (newAvatarUri != null) {
                newAvatarUrl = compressImageToBase64(context, null, newAvatarUri)
            }

            val finalAvatar = newAvatarUrl ?: current?.avatarUrl ?: "/uploads/img_perfil.png"
            val isVerified = (!idDocument.isNullOrBlank()) || (!phone.isNullOrBlank() && !city.isNullOrBlank())

            val updates = hashMapOf<String, Any>(
                "full_name" to fullName,
                "phone" to (phone ?: ""),
                "city" to (city ?: ""),
                "bio" to (bio ?: ""),
                "id_document" to (idDocument ?: ""),
                "is_verified" to isVerified,
                "avatar_url" to finalAvatar
            )

            firestore.collection("users").document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()

            val updatedDto = UserDto(
                id = current?.id ?: uid.hashCode(),
                username = current?.username ?: "usuario",
                email = current?.email ?: "usuario@subastymed.com",
                fullName = fullName,
                avatarUrl = finalAvatar,
                rating = current?.rating ?: 5.0,
                memberSince = current?.memberSince ?: "2026",
                phone = phone,
                city = city,
                bio = bio,
                idDocument = idDocument,
                isVerified = isVerified,
                role = current?.role ?: "user",
                uid = uid
            )

            SessionManager.saveSession(context, SessionManager.authToken ?: uid, updatedDto)
            Result.success(updatedDto)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando perfil en Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun logout(context: Context) {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signOut: ${e.message}")
        }
        SessionManager.clearSession(context)
    }

    suspend fun sendPasswordReset(emailOrUsername: String): Result<String> {
        return try {
            var targetEmail = emailOrUsername.trim()
            if (targetEmail.isBlank()) {
                throw IllegalArgumentException("Ingresa un correo o nombre de usuario")
            }

            if (!targetEmail.contains("@")) {
                val query = firestore.collection("users")
                    .whereEqualTo("username", targetEmail)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    targetEmail = query.documents[0].getString("email")
                        ?: throw IllegalArgumentException("No se encontró correo para este usuario")
                } else {
                    throw IllegalArgumentException("El usuario '$targetEmail' no está registrado")
                }
            }

            auth.sendPasswordResetEmail(targetEmail).await()
            Result.success(targetEmail)
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando correo de recuperación: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("No hay sesión activa")
            if (newPassword.length < 6) {
                throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres")
            }
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando contraseña: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Escucha en tiempo real de subastas desde Firestore
    fun listenToAuctions(
        onUpdate: (List<AuctionItem>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        firestore.collection("auctions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar subastas: ${error.message}")
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
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
                            val creatorId = doc.getString("creatorId")
                            val creatorName = doc.getString("creatorName")
                            val status = doc.getString("status") ?: "Activa"
                            val rawStart = doc.getLong("startTimeMillis") ?: 0L
                            val startTimeMillis = if (rawStart > 0L) rawStart else AuctionItem.parseDateStringToMillis(timeRemaining)
                            val rawEnd = doc.getLong("endTimeMillis") ?: 0L
                            val endTimeMillis = if (rawEnd > 0L) rawEnd else if (startTimeMillis > 0L) startTimeMillis + AuctionItem.ONE_HOUR_MILLIS else 0L
                            val highestBidderId = doc.getString("highestBidderId")
                            val highestBidderName = doc.getString("highestBidderName")

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
                                creatorName = creatorName,
                                status = status,
                                startTimeMillis = startTimeMillis,
                                endTimeMillis = endTimeMillis,
                                highestBidderId = highestBidderId,
                                highestBidderName = highestBidderName
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    LocalAuctionCache.saveAllAuctions(list)
                    onUpdate(list)
                } else {
                    // Si Firestore está vacío, sembramos los productos iniciales de ejemplo
                    seedDefaultAuctions()
                }
            }
    }

    private fun seedDefaultAuctions() {
        val initialAuctions = listOf(
            hashMapOf(
                "title" to "MacBook Pro 16\" M4",
                "description" to "Excelente estado, 32GB RAM, 1TB SSD, cargador original y caja.",
                "category" to "Electrónica",
                "startingPrice" to 1000.0,
                "currentBid" to 1250.0,
                "bidCount" to 14,
                "timeRemaining" to "02h 15m",
                "imageUrl" to "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600",
                "createdAt" to System.currentTimeMillis() - 4000000,
                "status" to "Activa",
                "creatorId" to "demo_carlos"
            ),
            hashMapOf(
                "title" to "BMW Serie 3 2021",
                "description" to "Impecable, 35,000 km, mantenimientos al día en concesionario oficial.",
                "category" to "Vehículos",
                "startingPrice" to 20000.0,
                "currentBid" to 24800.0,
                "bidCount" to 45,
                "timeRemaining" to "1d 04h",
                "imageUrl" to "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=600",
                "createdAt" to System.currentTimeMillis() - 3000000,
                "status" to "Activa",
                "creatorId" to "demo_alejandro"
            ),
            hashMapOf(
                "title" to "Chaqueta Cuero Retro",
                "description" to "Vintage, Talla L, Unisex, 100% cuero genuino, acabado clásico.",
                "category" to "Moda",
                "startingPrice" to 120.0,
                "currentBid" to 180.0,
                "bidCount" to 8,
                "timeRemaining" to "45m 12s",
                "imageUrl" to "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600",
                "createdAt" to System.currentTimeMillis() - 2000000,
                "status" to "Activa",
                "creatorId" to "demo_carlos"
            ),
            hashMapOf(
                "title" to "Oleo Abstracto",
                "description" to "Autor firmado, 120x80cm, técnica mixta sobre lienzo con detalles dorados.",
                "category" to "Arte",
                "startingPrice" to 500.0,
                "currentBid" to 620.0,
                "bidCount" to 10,
                "timeRemaining" to "05h 22m",
                "imageUrl" to "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=600",
                "createdAt" to System.currentTimeMillis() - 1000000,
                "status" to "Activa",
                "creatorId" to "demo_alejandro"
            )
        )

        for (auction in initialAuctions) {
            firestore.collection("auctions").add(auction)
        }
    }

    suspend fun createAuction(
        context: Context,
        title: String,
        description: String,
        category: String,
        price: Double,
        startDate: String,
        imageUri: Uri?,
        localImagePath: String? = null,
        formattedPrice: String? = null
    ): Result<String> {
        return try {
            var downloadUrl: String? = null
            var base64Fallback: String? = null

            // Subir imagen a Firebase Storage si está habilitado
            try {
                val bytes: ByteArray? = when {
                    localImagePath != null && java.io.File(localImagePath).exists() -> {
                        java.io.File(localImagePath).readBytes()
                    }
                    imageUri != null -> {
                        context.contentResolver.openInputStream(imageUri)?.readBytes()
                    }
                    else -> null
                }

                if (bytes != null) {
                    val ref = storage.reference.child("auctions/img_${System.currentTimeMillis()}.jpg")
                    ref.putBytes(bytes).await()
                    downloadUrl = ref.downloadUrl.await().toString()
                    Log.d(TAG, "Imagen subida exitosamente a Firebase Storage: $downloadUrl")
                }
            } catch (imgErr: Exception) {
                Log.w(TAG, "Firebase Storage no disponible (pide Plan Blaze). Usando Firestore Base64 gratuito: ${imgErr.message}")
            }

            // Solución 100% GRATIS sin tarjeta de crédito: si Storage no está activo,
            // comprimimos la imagen a ~40KB y la guardamos en Firestore directamente.
            if (downloadUrl == null && (localImagePath != null || imageUri != null)) {
                base64Fallback = compressImageToBase64(context, localImagePath, imageUri)
            }

            val finalImage = downloadUrl ?: base64Fallback ?: localImagePath ?: imageUri?.toString()
            val uid = auth.currentUser?.uid ?: SessionManager.currentUser?.uid ?: SessionManager.currentUser?.username ?: "anonimo"
            val creatorName = SessionManager.currentUser?.fullName ?: "Usuario"
            val parsedStart = AuctionItem.parseDateStringToMillis(startDate)
            val startTimeMillis = if (parsedStart > 0L) parsedStart else System.currentTimeMillis()
            val endTimeMillis = startTimeMillis + AuctionItem.ONE_HOUR_MILLIS

            val auctionData = hashMapOf(
                "title" to title,
                "description" to description,
                "category" to category,
                "startingPrice" to price,
                "currentBid" to price,
                "bidCount" to 0,
                "timeRemaining" to startDate,
                "imageUrl" to finalImage,
                "formattedPrice" to (formattedPrice ?: ""),
                "status" to "Activa",
                "creatorId" to uid,
                "creatorName" to creatorName,
                "createdAt" to System.currentTimeMillis(),
                "startTimeMillis" to startTimeMillis,
                "endTimeMillis" to endTimeMillis
            )

            val docRef = firestore.collection("auctions").add(auctionData).await()
            val newId = docRef.id

            // Guardar también en caché local con la imagen persistente y el precio formateado
            val localItem = AuctionItem(
                id = newId,
                title = title,
                description = description,
                currentBid = price,
                bidCount = 0,
                timeRemaining = startDate,
                imageRes = getFallbackDrawable(category),
                category = category,
                imageUrl = finalImage,
                formattedPrice = formattedPrice,
                creatorId = uid,
                creatorName = creatorName,
                status = "Activa",
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis
            )
            LocalAuctionCache.addAuction(localItem, isMine = true, userId = uid)

            Result.success(newId)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear subasta en Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateAuction(
        auctionId: String,
        title: String,
        description: String,
        newBid: Double,
        category: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "title" to title,
                "description" to description,
                "currentBid" to newBid,
                "startingPrice" to newBid,
                "category" to category
            )
            firestore.collection("auctions").document(auctionId).update(updates).await()

            val current = LocalAuctionCache.getAllAuctions().find { it.id == auctionId }
            if (current != null) {
                val updated = current.copy(
                    title = title,
                    description = description,
                    currentBid = newBid,
                    category = category
                )
                LocalAuctionCache.updateAuction(updated)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando subasta: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteAuction(auctionId: String): Result<Unit> {
        return try {
            firestore.collection("auctions").document(auctionId).delete().await()
            LocalAuctionCache.deleteAuction(auctionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando subasta: ${e.message}")
            Result.failure(e)
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

    private fun compressImageToBase64(context: Context, localPath: String?, uri: Uri?): String? {
        return try {
            val bitmap = when {
                localPath != null && java.io.File(localPath).exists() -> {
                    android.graphics.BitmapFactory.decodeFile(localPath)
                }
                uri != null -> {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                }
                else -> null
            } ?: return null

            val maxDimension = 600
            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > height && width > maxDimension) {
                maxDimension.toFloat() / width
            } else if (height > maxDimension) {
                maxDimension.toFloat() / height
            } else {
                1.0f
            }

            val resizedBitmap = if (scale < 1.0f) {
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

            val baos = java.io.ByteArrayOutputStream()
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
            val bytes = baos.toByteArray()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar Base64 para Firestore: ${e.message}", e)
            null
        }
    }

    // --- MÉTODOS DE ADMINISTRADOR ---

    suspend fun getAllUsers(): Result<List<UserDto>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    val uid = doc.id
                    val fullName = doc.getString("full_name") ?: "Usuario"
                    val username = doc.getString("username") ?: "usuario"
                    val email = doc.getString("email") ?: ""
                    val rating = doc.getDouble("rating") ?: 5.0
                    val memberSince = doc.getString("member_since") ?: "2026"
                    val avatarUrl = doc.getString("avatar_url")
                    val phone = doc.getString("phone")
                    val city = doc.getString("city")
                    val bio = doc.getString("bio")
                    val idDocument = doc.getString("id_document")
                    val isVerified = doc.getBoolean("is_verified") ?: false
                    val role = doc.getString("role") ?: if (email.equals("admin@subastymed.com", ignoreCase = true) || username.equals("admin", ignoreCase = true)) "admin" else "user"

                    UserDto(
                        id = uid.hashCode(),
                        username = username,
                        email = email,
                        fullName = fullName,
                        avatarUrl = avatarUrl,
                        rating = rating,
                        memberSince = memberSince,
                        phone = phone,
                        city = city,
                        bio = bio,
                        idDocument = idDocument,
                        isVerified = isVerified,
                        role = role,
                        uid = uid
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo todos los usuarios: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun toggleUserVerification(userUid: String, isVerified: Boolean): Result<Unit> {
        return try {
            firestore.collection("users").document(userUid).update("is_verified", isVerified).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error alternando verificación de usuario: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserByAdmin(userUid: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userUid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando usuario en Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllAuctionsAdmin(): Result<List<AuctionItem>> {
        return try {
            val snapshot = firestore.collection("auctions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
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
                    val creatorId = doc.getString("creatorId")
                    val creatorName = doc.getString("creatorName")
                    val status = doc.getString("status") ?: "Activa"
                    val rawStart = doc.getLong("startTimeMillis") ?: 0L
                    val startTimeMillis = if (rawStart > 0L) rawStart else AuctionItem.parseDateStringToMillis(timeRemaining)
                    val rawEnd = doc.getLong("endTimeMillis") ?: 0L
                    val endTimeMillis = if (rawEnd > 0L) rawEnd else if (startTimeMillis > 0L) startTimeMillis + AuctionItem.ONE_HOUR_MILLIS else 0L
                    val highestBidderId = doc.getString("highestBidderId")
                    val highestBidderName = doc.getString("highestBidderName")

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
                        creatorName = creatorName,
                        status = status,
                        startTimeMillis = startTimeMillis,
                        endTimeMillis = endTimeMillis,
                        highestBidderId = highestBidderId,
                        highestBidderName = highestBidderName
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo subastas para admin: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Sistema de Pujas (Bids) y Sala en Vivo (1 Hora) con Firebase Firestore ---

    suspend fun startAuctionLiveNow(auctionId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val end = now + AuctionItem.ONE_HOUR_MILLIS
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy - hh:mm a", java.util.Locale.getDefault())
            val nowFormatted = sdf.format(java.util.Date(now))

            firestore.collection("auctions").document(auctionId).update(
                mapOf(
                    "status" to "Activa",
                    "startTimeMillis" to now,
                    "endTimeMillis" to end,
                    "timeRemaining" to nowFormatted
                )
            ).await()

            val localItem = LocalAuctionCache.getAllAuctions().find { it.id == auctionId }
            if (localItem != null) {
                LocalAuctionCache.updateAuction(
                    localItem.copy(
                        status = "Activa",
                        startTimeMillis = now,
                        endTimeMillis = end,
                        timeRemaining = nowFormatted
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando subasta en vivo: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun placeBid(auctionId: String, amount: Double): Result<Unit> {
        return try {
            val user = auth.currentUser
            val uid = user?.uid ?: SessionManager.currentUser?.uid ?: throw IllegalStateException("Debes iniciar sesión para realizar una puja")
            val userName = SessionManager.currentUser?.fullName ?: SessionManager.currentUser?.username ?: "Usuario"

            val auctionDocRef = firestore.collection("auctions").document(auctionId)
            val auctionDoc = auctionDocRef.get().await()
            if (!auctionDoc.exists()) {
                throw IllegalArgumentException("La subasta no existe o fue eliminada")
            }

            val creatorId = auctionDoc.getString("creatorId")
            val creatorName = auctionDoc.getString("creatorName")
            val currentUsername = SessionManager.currentUser?.username
            val isCreator = (!creatorId.isNullOrBlank() && (creatorId == uid || creatorId.equals(currentUsername, ignoreCase = true))) ||
                    (!creatorName.isNullOrBlank() && creatorName.equals(userName, ignoreCase = true))

            if (isCreator) {
                throw IllegalStateException("No puedes realizar pujas en tu propia publicación")
            }

            val currentBid = auctionDoc.getDouble("currentBid") ?: auctionDoc.getDouble("startingPrice") ?: 0.0
            val auctionStatus = auctionDoc.getString("status") ?: "Activa"
            val timeRemaining = auctionDoc.getString("timeRemaining") ?: "24h 00m"
            val rawStart = auctionDoc.getLong("startTimeMillis") ?: 0L
            val startTimeMillis = if (rawStart > 0L) rawStart else AuctionItem.parseDateStringToMillis(timeRemaining)
            val rawEnd = auctionDoc.getLong("endTimeMillis") ?: 0L
            val endTimeMillis = if (rawEnd > 0L) rawEnd else if (startTimeMillis > 0L) startTimeMillis + AuctionItem.ONE_HOUR_MILLIS else 0L
            val now = System.currentTimeMillis()

            if (auctionStatus.equals("Finalizada", ignoreCase = true) || (endTimeMillis > 0L && now >= endTimeMillis)) {
                finalizeAuction(auctionId)
                throw IllegalStateException("El tiempo de 1 hora de la subasta ha finalizado y las pujas están cerradas")
            }

            if (startTimeMillis > 0L && now < startTimeMillis) {
                throw IllegalStateException("La subasta aún no ha comenzado. Inicia el $timeRemaining")
            }

            if (amount <= currentBid) {
                throw IllegalArgumentException("Tu puja ($$amount) debe ser superior a la oferta actual de $$currentBid")
            }

            val bidCount = (auctionDoc.getLong("bidCount")?.toInt() ?: 0) + 1
            val title = auctionDoc.getString("title") ?: "Subasta"
            val imageUrl = auctionDoc.getString("imageUrl")

            // 1. Registrar evento individual en 'live_bids' para que todos vean el historial en vivo completo
            try {
                val liveBidEvent = hashMapOf(
                    "auctionId" to auctionId,
                    "bidderId" to uid,
                    "bidderName" to userName,
                    "amount" to amount,
                    "timestamp" to now
                )
                firestore.collection("live_bids").add(liveBidEvent).await()
            } catch (liveErr: Exception) {
                Log.w(TAG, "Aviso guardando evento live_bids: ${liveErr.message}")
            }

            // 2. Marcar pujas previas de otros usuarios para esta subasta como 'Superado' y actualizar maxBid
            try {
                val previousBids = firestore.collection("bids")
                    .whereEqualTo("auctionId", auctionId)
                    .get()
                    .await()

                for (prevDoc in previousBids.documents) {
                    val updateMap = mutableMapOf<String, Any>(
                        "maxBid" to amount
                    )
                    val prevBidderId = prevDoc.getString("bidderId")
                    if (prevBidderId != uid && prevDoc.getString("status") == "Ganando") {
                        updateMap["status"] = "Superado"
                    }
                    firestore.collection("bids").document(prevDoc.id).update(updateMap).await()
                }
            } catch (exBids: Exception) {
                Log.w(TAG, "Aviso actualizando pujas anteriores: ${exBids.message}")
            }

            // 3. Comprobar si este usuario ya había pujado en esta subasta para actualizarla o crear nueva en 'bids'
            val userExistingBidQuery = firestore.collection("bids")
                .whereEqualTo("auctionId", auctionId)
                .whereEqualTo("bidderId", uid)
                .limit(1)
                .get()
                .await()

            if (!userExistingBidQuery.isEmpty) {
                val userBidDoc = userExistingBidQuery.documents[0]
                firestore.collection("bids").document(userBidDoc.id).update(
                    mapOf(
                        "bidderName" to userName,
                        "amount" to amount,
                        "maxBid" to amount,
                        "status" to "Ganando",
                        "filterGroup" to "Activas",
                        "updatedAt" to now
                    )
                ).await()
            } else {
                val bidData = hashMapOf(
                    "auctionId" to auctionId,
                    "auctionTitle" to title,
                    "bidderId" to uid,
                    "bidderName" to userName,
                    "amount" to amount,
                    "maxBid" to amount,
                    "imageUrl" to imageUrl,
                    "status" to "Ganando",
                    "filterGroup" to "Activas",
                    "createdAt" to now,
                    "updatedAt" to now,
                    "timeRemaining" to timeRemaining
                )
                firestore.collection("bids").add(bidData).await()
            }

            // 4. Actualizar la subasta en Firestore (y asegurar que tenga ventana de 1h activa si no la tenía)
            val auctionUpdates = mutableMapOf<String, Any>(
                "currentBid" to amount,
                "bidCount" to bidCount,
                "highestBidderId" to uid,
                "highestBidderName" to userName
            )
            if (startTimeMillis == 0L) {
                auctionUpdates["startTimeMillis"] = now
                auctionUpdates["endTimeMillis"] = now + AuctionItem.ONE_HOUR_MILLIS
            }
            auctionDocRef.update(auctionUpdates).await()

            // 5. Actualizar en caché local
            val localItem = LocalAuctionCache.getAllAuctions().find { it.id == auctionId }
            if (localItem != null) {
                val updated = localItem.copy(
                    currentBid = amount,
                    bidCount = bidCount,
                    highestBidderId = uid,
                    highestBidderName = userName
                )
                LocalAuctionCache.updateAuction(updated)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error realizando puja: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun listenToAuctionLiveBids(
        auctionId: String,
        onUpdate: (List<LiveBidEntry>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("bids")
            .whereEqualTo("auctionId", auctionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando pujas en vivo de subasta: ${error.message}")
                    return@addSnapshotListener
                }

                // También consultamos la colección live_bids para mostrar el historial completo de pujas
                firestore.collection("live_bids")
                    .whereEqualTo("auctionId", auctionId)
                    .get()
                    .addOnSuccessListener { liveSnap ->
                        val fromLiveEvents = liveSnap.documents.mapNotNull { doc ->
                            try {
                                LiveBidEntry(
                                    id = doc.id,
                                    auctionId = auctionId,
                                    bidderId = doc.getString("bidderId") ?: "",
                                    bidderName = doc.getString("bidderName") ?: "Usuario",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val fromBidsDocs = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                LiveBidEntry(
                                    id = doc.id,
                                    auctionId = auctionId,
                                    bidderId = doc.getString("bidderId") ?: "",
                                    bidderName = doc.getString("bidderName") ?: "Usuario",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    timestamp = doc.getLong("updatedAt") ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()

                        val combined = (fromLiveEvents + fromBidsDocs)
                            .distinctBy { "${it.bidderId}_${it.amount}" }
                            .sortedWith(compareByDescending<LiveBidEntry> { it.amount }.thenByDescending { it.timestamp })

                        onUpdate(combined)
                    }
                    .addOnFailureListener {
                        val fallback = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                LiveBidEntry(
                                    id = doc.id,
                                    auctionId = auctionId,
                                    bidderId = doc.getString("bidderId") ?: "",
                                    bidderName = doc.getString("bidderName") ?: "Usuario",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    timestamp = doc.getLong("updatedAt") ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }?.sortedByDescending { it.amount } ?: emptyList()

                        onUpdate(fallback)
                    }
            }
    }

    suspend fun finalizeAuction(auctionId: String): Result<Unit> {
        return try {
            val auctionDocRef = firestore.collection("auctions").document(auctionId)
            val auctionDoc = auctionDocRef.get().await()
            if (!auctionDoc.exists()) {
                throw IllegalArgumentException("Subasta no encontrada")
            }

            val highestBidderId = auctionDoc.getString("highestBidderId")

            // 1. Actualizar estado de la subasta a 'Finalizada'
            auctionDocRef.update(
                mapOf(
                    "status" to "Finalizada",
                    "timeRemaining" to "Subasta Finalizada"
                )
            ).await()

            // 2. Actualizar todas las pujas asociadas a esta subasta
            val bidsQuery = firestore.collection("bids")
                .whereEqualTo("auctionId", auctionId)
                .get()
                .await()

            for (b in bidsQuery.documents) {
                val bidderId = b.getString("bidderId")
                if (bidderId == highestBidderId && !highestBidderId.isNullOrEmpty()) {
                    firestore.collection("bids").document(b.id).update(
                        mapOf(
                            "status" to "Ganada",
                            "filterGroup" to "Ganadas"
                        )
                    ).await()
                } else {
                    firestore.collection("bids").document(b.id).update(
                        mapOf(
                            "status" to "Perdida",
                            "filterGroup" to "Perdidas"
                        )
                    ).await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizando subasta: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun listenToUserBids(
        userId: String,
        onUpdate: (List<BidItem>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection("bids")
            .whereEqualTo("bidderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando pujas del usuario: ${error.message}")
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val title = doc.getString("auctionTitle") ?: "Subasta"
                            val amount = doc.getDouble("amount") ?: 0.0
                            val maxBid = doc.getDouble("maxBid") ?: amount
                            val status = doc.getString("status") ?: "Ganando"
                            val filterGroup = doc.getString("filterGroup")
                                ?: when (status.lowercase()) {
                                    "ganada" -> "Ganadas"
                                    "perdida" -> "Perdidas"
                                    else -> "Activas"
                                }
                            val imageUrl = doc.getString("imageUrl")

                            val (textColor, bgColor) = when (status) {
                                "Ganando" -> Pair(Color(0xFF4ADE80), Color(0xFF14532D))
                                "Superado" -> Pair(Color(0xFFFBBF24), Color(0xFF78350F))
                                "Ganada" -> Pair(Color(0xFFA78BFA), Color(0xFF312E81))
                                "Perdida" -> Pair(Color(0xFFF87171), Color(0xFF450A0A))
                                else -> Pair(Color(0xFF4ADE80), Color(0xFF14532D))
                            }

                            val yourBidFormatted = "$" + String.format(java.util.Locale.US, "%,.2f", amount)
                            val maxBidFormatted = "$" + String.format(java.util.Locale.US, "%,.2f", maxBid)

                            BidItem(
                                id = id,
                                title = title,
                                yourBid = yourBidFormatted,
                                maxBid = maxBidFormatted,
                                status = status,
                                filterGroup = filterGroup,
                                statusTextColor = textColor,
                                statusBgColor = bgColor,
                                imageRes = getFallbackDrawable(title),
                                imageUrl = imageUrl
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(list)
                }
            }
    }
}
