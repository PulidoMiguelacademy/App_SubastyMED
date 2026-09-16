package com.example.subastymed.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.subastymed.R
import com.example.subastymed.model.AuctionItem
import com.example.subastymed.network.RetrofitClient
import com.example.subastymed.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    // Escuchamos los estados del ViewModel
    val subastas by viewModel.filteredAuctions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Estado para mostrar detalle al tocar una subasta
    var selectedAuctionForDetail by remember { mutableStateOf<AuctionItem?>(null) }

    // Refrescar al entrar a la pantalla
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(2) }) { TopHeader() }

            item(span = { GridItemSpan(2) }) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            item(span = { GridItemSpan(2) }) {
                CategoryChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) }
                )
            }

            item(span = { GridItemSpan(2) }) { SectionTitle("Subastas Destacadas") }

            items(subastas) { item ->
                AuctionCard(
                    item = item,
                    onClick = { selectedAuctionForDetail = item }
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

    // Diálogo Detallado al tocar una subasta
    selectedAuctionForDetail?.let { auction ->
        AuctionDetailDialog(
            auction = auction,
            onDismiss = { selectedAuctionForDetail = null }
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
        Text("Ver todo", color = Color(0xFFFF9800), fontSize = 14.sp)
    }
}

@Composable
fun AuctionCard(
    item: AuctionItem,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                if (!item.imageUrl.isNullOrEmpty()) {
                    val fullUrl = if (item.imageUrl.startsWith("http")) item.imageUrl
                    else "${RetrofitClient.BASE_URL.removeSuffix("/")}${item.imageUrl}"
                    AsyncImage(
                        model = fullUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = item.imageRes),
                        error = painterResource(id = item.imageRes),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Etiqueta superior con tiempo / inicio
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(item.timeRemaining, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            // En la tarjeta principal solo mostramos Título y Precio
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text("Oferta actual", color = Color.Gray, fontSize = 11.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${item.currentBid}",
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
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Cabecera con botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detalle de Subasta",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color(0xFF0F172A), CircleShape).size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Imagen grande del producto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    if (!auction.imageUrl.isNullOrEmpty()) {
                        val fullUrl = if (auction.imageUrl.startsWith("http")) auction.imageUrl
                        else "${RetrofitClient.BASE_URL.removeSuffix("/")}${auction.imageUrl}"
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = auction.title,
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = auction.imageRes),
                            error = painterResource(id = auction.imageRes),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = auction.imageRes),
                            contentDescription = auction.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categoría badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(auction.category, color = Color(0xFFFF9800), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Título
                Text(
                    text = auction.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fila de Precio y Pujas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Precio / Oferta actual", color = Color.Gray, fontSize = 12.sp)
                        Text("$${auction.currentBid}", color = Color(0xFFFF9800), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${auction.bidCount} pujas realizadas",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fecha y Hora de Inicio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Fecha",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Fecha y hora de inicio", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = auction.timeRemaining,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Descripción completa / Información
                Text(
                    text = "Información del producto:",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = auction.description,
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}