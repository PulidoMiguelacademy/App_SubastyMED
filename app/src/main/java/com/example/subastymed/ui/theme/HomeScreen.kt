package com.example.subastymed.ui.theme

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.subastymed.R
import com.example.subastymed.model.AuctionItem
import com.example.subastymed.model.AuctionPhase
import com.example.subastymed.model.LiveBidEntry
import com.example.subastymed.network.FirebaseManager
import com.example.subastymed.network.SessionManager
import com.example.subastymed.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val subastas by viewModel.filteredAuctions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Guardamos el item seleccionado y siempre lo sincronizamos con la lista en tiempo real
    var selectedAuctionSnapshot by remember { mutableStateOf<AuctionItem?>(null) }
    val liveSelectedAuction = selectedAuctionSnapshot?.let { snap ->
        subastas.find { it.id == snap.id } ?: snap
    }

    // Reloj global que actualiza cada segundo las cuentas regresivas de 1 hora
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val finalizedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        viewModel.refresh()
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    // Cierre automático cuando una subasta cumple su 1 hora en vivo
    LaunchedEffect(nowMillis, subastas) {
        subastas.forEach { item ->
            val end = item.getEffectiveEndTimeMillis()
            val isAlreadyMarkedEnded = item.status.equals("Finalizada", ignoreCase = true) ||
                    item.timeRemaining.contains("Finalizada", ignoreCase = true)
            if (end > 0L && nowMillis >= end && !isAlreadyMarkedEnded && !finalizedIds.contains(item.id)) {
                finalizedIds.add(item.id)
                FirebaseManager.finalizeAuction(item.id)
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp
    val columns = when {
        screenWidth >= 900 -> 4
        screenWidth >= 600 || isLandscape -> 3
        else -> 2
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(columns) }) { TopHeader() }

            item(span = { GridItemSpan(columns) }) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            item(span = { GridItemSpan(columns) }) {
                CategoryChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) }
                )
            }

            item(span = { GridItemSpan(columns) }) { SectionTitle("Subastas en Tiempo Real") }

            items(subastas) { item ->
                AuctionCard(
                    item = item,
                    nowMillis = nowMillis,
                    onClick = { selectedAuctionSnapshot = item }
                )
            }
        }

        if (isLoading && subastas.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFFF9800)
            )
        }
    }

    // Sala de Subasta en Vivo al tocar una subasta
    liveSelectedAuction?.let { auction ->
        AuctionDetailDialog(
            auction = auction,
            nowMillis = nowMillis,
            onDismiss = { selectedAuctionSnapshot = null }
        )
    }
}

@Composable
fun TopHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.logo_subastymed),
                contentDescription = "Logo SubastyMED",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("SubastyMED", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = { }, modifier = Modifier.background(Color(0xFF1E293B), CircleShape)) {
            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = Color.White)
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar artículos...", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, tint = Color.Gray, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFF1E293B),
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun CategoryChips(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("Todos", "Electrónica", "Vehículos", "Hogar", "Arte", "Moda", "Bicicletas")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        items(categories.size) { index ->
            val categoryName = categories[index]
            val isSelected = categoryName == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1E293B))
                    .clickable { onCategorySelected(categoryName) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(categoryName, color = if (isSelected) Color.White else Color.LightGray)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("En vivo (1h)", color = Color(0xFFFF9800), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuctionCard(
    item: AuctionItem,
    nowMillis: Long,
    onClick: () -> Unit = {}
) {
    val phase = item.getCurrentPhase(nowMillis)
    val badgeBgColor = when (phase) {
        AuctionPhase.LIVE -> Color(0xFFDC2626).copy(alpha = 0.90f)
        AuctionPhase.SCHEDULED -> Color(0xFFD97706).copy(alpha = 0.90f)
        AuctionPhase.ENDED -> Color(0xFF312E81).copy(alpha = 0.90f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = if (phase == AuctionPhase.LIVE) BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 11f)
            ) {
                val imageModel = remember(item.imageUrl, item.imageRes) {
                    item.getResolvedImage()
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = item.imageRes),
                    error = painterResource(id = item.imageRes),
                    modifier = Modifier.fillMaxSize()
                )

                // Etiqueta superior con estado en vivo / cuenta regresiva de 1h
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.getLiveStatusBadgeText(nowMillis),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (phase == AuctionPhase.ENDED) "Oferta final" else "Oferta actual",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.getDisplayPrice(),
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${item.bidCount} pujas",
                        color = Color(0xFFFF9800),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(Color(0xFF2D1F16), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AuctionDetailDialog(
    auction: AuctionItem,
    nowMillis: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    val isAdmin = SessionManager.currentUser?.isAdmin == true
    val currentUserId = FirebaseManager.auth.currentUser?.uid ?: SessionManager.currentUser?.uid
    val currentUsername = SessionManager.currentUser?.username
    val currentFullName = SessionManager.currentUser?.fullName
    val isCreator = (!currentUserId.isNullOrBlank() && currentUserId == auction.creatorId) ||
            (!currentUsername.isNullOrBlank() && currentUsername.equals(auction.creatorId, ignoreCase = true)) ||
            (!currentFullName.isNullOrBlank() && !auction.creatorName.isNullOrBlank() && currentFullName.equals(auction.creatorName, ignoreCase = true))

    val phase = auction.getCurrentPhase(nowMillis)
    val startMillis = auction.getEffectiveStartTimeMillis()
    val endMillis = auction.getEffectiveEndTimeMillis()

    val minIncrement = if (auction.currentBid >= 1000) 100.0 else 20.0
    var customBidText by remember(auction.currentBid) {
        mutableStateOf((auction.currentBid + minIncrement).toLong().toString())
    }
    var isBidding by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFinalizeConfirm by remember { mutableStateOf(false) }

    // Lista en tiempo real de las pujas que todos los usuarios están haciendo en esta subasta
    var liveBids by remember { mutableStateOf<List<LiveBidEntry>>(emptyList()) }

    DisposableEffect(auction.id) {
        val registration = FirebaseManager.listenToAuctionLiveBids(auction.id) { entries ->
            liveBids = entries
        }
        onDispose {
            registration.remove()
        }
    }

    val leadingBid = liveBids.firstOrNull()
    val leaderName = leadingBid?.bidderName
        ?: auction.highestBidderName
        ?: if (auction.bidCount > 0) "Postor Activo" else "Sin ofertas aún"

    val screenWidth = configuration.screenWidthDp
    val isTabletOrWide = screenWidth >= 640

    val onStartLiveAction: () -> Unit = {
        coroutineScope.launch {
            val res = FirebaseManager.startAuctionLiveNow(auction.id)
            if (res.isSuccess) {
                Toast.makeText(context, "🔴 ¡Subasta iniciada en vivo por 1 hora!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onPlaceBidAction: (String) -> Unit = { rawBidText ->
        if (isCreator) {
            Toast.makeText(
                context,
                "No puedes hacer pujas en una subasta creada por ti mismo",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val amount = AuctionItem.parsePriceInput(rawBidText)
            if (amount == null || amount <= auction.currentBid) {
                Toast.makeText(
                    context,
                    "La puja debe ser superior a ${auction.getDisplayPrice()}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                isBidding = true
                coroutineScope.launch {
                    val res = FirebaseManager.placeBid(auction.id, amount)
                    isBidding = false
                    if (res.isSuccess) {
                        Toast.makeText(
                            context,
                            "✅ ¡Puja de ${AuctionItem.formatCurrency(amount)} publicada en vivo!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val errMsg = res.exceptionOrNull()?.message ?: "Error al realizar puja"
                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .widthIn(max = if (isTabletOrWide) 920.dp else 500.dp)
                .fillMaxWidth(if (isTabletOrWide) 0.94f else 0.95f)
                .fillMaxHeight(if (isLandscape && !isTabletOrWide) 0.95f else 0.92f)
                .padding(vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isTabletOrWide) 22.dp else 16.dp)
            ) {
                // Cabecera con indicador en vivo y botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (phase) {
                                AuctionPhase.LIVE -> "Sala de Subasta EN VIVO"
                                AuctionPhase.SCHEDULED -> "Subasta Programada"
                                AuctionPhase.ENDED -> "Resultados de Subasta"
                            },
                            color = Color.White,
                            fontSize = if (isTabletOrWide) 20.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFF0F172A), CircleShape)
                            .size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BANNER DE CRONÓMETRO EN VIVO (1 HORA DE PUJAS)
                LiveTimerBanner(
                    phase = phase,
                    startMillis = startMillis,
                    endMillis = endMillis,
                    nowMillis = nowMillis,
                    scheduledText = auction.timeRemaining,
                    canStartEarly = isAdmin,
                    onStartLiveNow = onStartLiveAction
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isTabletOrWide) {
                    // DISEÑO DE 2 COLUMNAS PARA TABLET Y PANTALLAS GRANDES / HORIZONTALES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Columna Izquierda: Foto, Detalles, Precio actual y Acciones
                        Column(modifier = Modifier.weight(1f)) {
                            AuctionProductSummarySection(
                                auction = auction,
                                phase = phase,
                                leaderName = leaderName,
                                totalBidsCount = maxOf(auction.bidCount, liveBids.size),
                                hasWinner = liveBids.isNotEmpty() || !auction.highestBidderName.isNullOrBlank(),
                                imageHeight = 220.dp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            AuctionFooterActionsSection(
                                phase = phase,
                                isCreator = isCreator,
                                isAdmin = isAdmin,
                                onFinalizeClick = { showFinalizeConfirm = true },
                                onDeleteClick = { showDeleteConfirm = true },
                                onDismiss = onDismiss
                            )
                        }

                        // Columna Derecha: Tablero en vivo + Panel para Pujar
                        Column(modifier = Modifier.weight(1.08f)) {
                            LiveBidsFeedSection(
                                liveBids = liveBids,
                                currentUserId = currentUserId,
                                isEnded = phase == AuctionPhase.ENDED
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            AuctionInteractiveBidSection(
                                auction = auction,
                                phase = phase,
                                isCreator = isCreator,
                                leaderName = leaderName,
                                hasBids = liveBids.isNotEmpty() || !auction.highestBidderName.isNullOrBlank(),
                                customBidText = customBidText,
                                onCustomBidChange = { customBidText = it },
                                isBidding = isBidding,
                                onPlaceBid = onPlaceBidAction
                            )
                        }
                    }
                } else {
                    // DISEÑO OPTIMIZADO DE 1 COLUMNA PARA CELULAR
                    AuctionProductSummarySection(
                        auction = auction,
                        phase = phase,
                        leaderName = leaderName,
                        totalBidsCount = maxOf(auction.bidCount, liveBids.size),
                        hasWinner = liveBids.isNotEmpty() || !auction.highestBidderName.isNullOrBlank(),
                        imageHeight = 185.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LiveBidsFeedSection(
                        liveBids = liveBids,
                        currentUserId = currentUserId,
                        isEnded = phase == AuctionPhase.ENDED
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AuctionInteractiveBidSection(
                        auction = auction,
                        phase = phase,
                        isCreator = isCreator,
                        leaderName = leaderName,
                        hasBids = liveBids.isNotEmpty() || !auction.highestBidderName.isNullOrBlank(),
                        customBidText = customBidText,
                        onCustomBidChange = { customBidText = it },
                        isBidding = isBidding,
                        onPlaceBid = onPlaceBidAction
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AuctionFooterActionsSection(
                        phase = phase,
                        isCreator = isCreator,
                        isAdmin = isAdmin,
                        onFinalizeClick = { showFinalizeConfirm = true },
                        onDeleteClick = { showDeleteConfirm = true },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Eliminar Subasta", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar permanentemente esta subasta como Administrador?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        coroutineScope.launch {
                            FirebaseManager.deleteAuction(auction.id)
                            Toast.makeText(context, "Subasta eliminada por el Administrador", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    if (showFinalizeConfirm) {
        AlertDialog(
            onDismissRequest = { showFinalizeConfirm = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Finalizar Subasta", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas cerrar las pujas ahora? El participante con la mayor oferta será declarado Ganador (pasará a 'Ganadas') y las demás pujas pasarán a 'Perdidas'.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showFinalizeConfirm = false
                        coroutineScope.launch {
                            val res = FirebaseManager.finalizeAuction(auction.id)
                            if (res.isSuccess) {
                                Toast.makeText(context, "¡Subasta finalizada! Ganador asignado.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Confirmar Cierre", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizeConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AuctionProductSummarySection(
    auction: AuctionItem,
    phase: AuctionPhase,
    leaderName: String,
    totalBidsCount: Int,
    hasWinner: Boolean,
    imageHeight: Dp
) {
    // Imagen del producto
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
    ) {
        val detailImageModel = remember(auction.imageUrl, auction.imageRes) {
            auction.getResolvedImage()
        }
        AsyncImage(
            model = detailImageModel,
            contentDescription = auction.title,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = auction.imageRes),
            error = painterResource(id = auction.imageRes),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(auction.category, color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Título y Descripción
    Text(
        text = auction.title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = auction.description,
        color = Color(0xFFCBD5E1),
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Tarjeta de Precio Actual y Líder en Tiempo Real
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = if (phase == AuctionPhase.ENDED) "Precio Final de Cierre" else "Oferta Más Alta Actual",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = auction.getDisplayPrice(),
                color = Color(0xFFFF9800),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (phase == AuctionPhase.ENDED && hasWinner) {
                        "Ganador: $leaderName"
                    } else {
                        "Líder: $leaderName"
                    },
                    color = Color(0xFF4ADE80),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$totalBidsCount pujas",
                color = Color(0xFFFF9800),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun AuctionInteractiveBidSection(
    auction: AuctionItem,
    phase: AuctionPhase,
    isCreator: Boolean,
    leaderName: String,
    hasBids: Boolean,
    customBidText: String,
    onCustomBidChange: (String) -> Unit,
    isBidding: Boolean,
    onPlaceBid: (String) -> Unit
) {
    when (phase) {
        AuctionPhase.ENDED -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF312E81))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🏁 Pujas Cerradas • Subasta Finalizada",
                        color = Color(0xFFA78BFA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (hasBids) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Adjudicada a $leaderName por ${auction.getDisplayPrice()}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        AuctionPhase.SCHEDULED -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFFD97706), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⏳ Las pujas se habilitarán en la hora de inicio",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Inicio: ${auction.timeRemaining} (Duración: 1 hora)",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        AuctionPhase.LIVE -> {
            if (isCreator) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Esta es tu publicación",
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Como creador de esta subasta no puedes realizar pujas sobre tu propio artículo. Aquí puedes monitorear en tiempo real las ofertas de los demás participantes.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔨 Hacer una Puja en Tiempo Real",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supera la oferta actual de ${auction.getDisplayPrice()} antes de que termine la hora:",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botones rápidos de incremento (opcionales)
                        val parsedCurrentInput = AuctionItem.parsePriceInput(customBidText)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(20, 50, 100, 500).forEach { inc ->
                                val targetAmount = (auction.currentBid + inc).toLong()
                                val incVal = targetAmount.toString()
                                val isSelectedQuick = parsedCurrentInput?.toLong() == targetAmount
                                OutlinedButton(
                                    onClick = { onCustomBidChange(incVal) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelectedQuick) Color(0xFFFF9800) else Color(0xFF334155)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelectedQuick) Color(0xFFFF9800).copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text(
                                        text = "+$$inc",
                                        color = if (isSelectedQuick) Color(0xFFFF9800) else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customBidText,
                            onValueChange = { newText ->
                                onCustomBidChange(newText.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })
                            },
                            label = { Text("Escribe cualquier monto mayor a ${auction.getDisplayPrice()}", color = Color.Gray, fontSize = 12.sp) },
                            placeholder = { Text("Ej: 24000 o 24.000", color = Color.DarkGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )

                        if (parsedCurrentInput != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val isValidBid = parsedCurrentInput > auction.currentBid
                            Text(
                                text = if (isValidBid) {
                                    "✓ Oferta lista para enviar: ${AuctionItem.formatCurrency(parsedCurrentInput)}"
                                } else {
                                    "⚠ La oferta (${AuctionItem.formatCurrency(parsedCurrentInput)}) debe superar ${auction.getDisplayPrice()}"
                                },
                                color = if (isValidBid) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onPlaceBid(customBidText) },
                            enabled = !isBidding,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isBidding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Pujar en Vivo Ahora",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuctionFooterActionsSection(
    phase: AuctionPhase,
    isCreator: Boolean,
    isAdmin: Boolean,
    onFinalizeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    // Opciones de control de la subasta exclusivas para el Administrador
    if (phase != AuctionPhase.ENDED && isAdmin) {
        Button(
            onClick = onFinalizeClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Cerrar Pujas y Declarar Ganador",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    // Opción exclusiva para el Administrador
    if (isAdmin) {
        Button(
            onClick = onDeleteClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Eliminar Subasta (Admin)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF475569))
    ) {
        Text("Salir de la Sala", color = Color.LightGray, fontSize = 14.sp)
    }
}

@Composable
fun LiveTimerBanner(
    phase: AuctionPhase,
    startMillis: Long,
    endMillis: Long,
    nowMillis: Long,
    scheduledText: String,
    canStartEarly: Boolean,
    onStartLiveNow: () -> Unit
) {
    when (phase) {
        AuctionPhase.LIVE -> {
            val remainingMillis = if (endMillis > 0L) (endMillis - nowMillis).coerceAtLeast(0L) else AuctionItem.ONE_HOUR_MILLIS
            val progress = (remainingMillis.toFloat() / AuctionItem.ONE_HOUR_MILLIS.toFloat()).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF450A0A))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EN VIVO • PUJAS (1H)",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF7F1D1D))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AuctionItem.formatCountdown(remainingMillis),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFEF4444),
                    trackColor = Color(0xFF7F1D1D)
                )
            }
        }

        AuctionPhase.SCHEDULED -> {
            val waitMillis = (startMillis - nowMillis).coerceAtLeast(0L)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF451A03))
                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "⏳ PROGRAMADA PARA INICIAR",
                            color = Color(0xFFFCD34D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Faltan ${AuctionItem.formatCountdown(waitMillis)} ($scheduledText)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (canStartEarly) {
                        Button(
                            onClick = onStartLiveNow,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Iniciar Ya (1h)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        AuctionPhase.ENDED -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1B4B))
                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏁 TIEMPO DE SUBASTA FINALIZADO",
                    color = Color(0xFFA5B4FC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                if (canStartEarly) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onStartLiveNow,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Reabrir 1h en Vivo",
                            color = Color(0xFFFF9800),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveBidsFeedSection(
    liveBids: List<LiveBidEntry>,
    currentUserId: String?,
    isEnded: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Pujas en Tiempo Real",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (liveBids.isEmpty()) "Sin ofertas" else "${liveBids.size} registradas",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (liveBids.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no se han realizado pujas en esta sala.\n¡Haz la primera oferta y toma el liderato!",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                liveBids.take(8).forEachIndexed { index, bid ->
                    val isTop = index == 0
                    val isMine = !currentUserId.isNullOrBlank() && bid.bidderId == currentUserId
                    val rowBg = when {
                        isTop -> Color(0xFF14532D).copy(alpha = 0.55f)
                        isMine -> Color(0xFF1E293B)
                        else -> Color(0xFF1E293B).copy(alpha = 0.6f)
                    }
                    val rowBorder = when {
                        isTop -> Color(0xFF4ADE80)
                        isMine -> Color(0xFFFF9800).copy(alpha = 0.5f)
                        else -> Color(0xFF334155)
                    }
                    val statusLabel = when {
                        isTop && isEnded -> "GANADOR"
                        isTop -> "GANANDO"
                        else -> "SUPERADO"
                    }
                    val statusColor = if (isTop) Color(0xFF4ADE80) else Color(0xFFFBBF24)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(rowBg)
                            .border(1.dp, rowBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isTop) Color(0xFF4ADE80) else Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isTop) "🏆" else "#${index + 1}",
                                    color = if (isTop) Color(0xFF052E16) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMine) "${bid.bidderName} (Tú)" else bid.bidderName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(statusColor.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            color = statusColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bid.getRelativeTime(),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Text(
                            text = bid.getFormattedAmount(),
                            color = if (isTop) Color(0xFF4ADE80) else Color(0xFFFF9800),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
