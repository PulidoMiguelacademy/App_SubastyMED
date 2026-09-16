package com.example.subastymed.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.subastymed.model.BidItem
import com.example.subastymed.network.RetrofitClient
import com.example.subastymed.viewmodel.BidsViewModel

@Composable
fun BidsScreen(viewModel: BidsViewModel = viewModel()) {
    val misPujas by viewModel.filteredBids.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Recargar datos al entrar
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val filters = listOf("Activas", "Ganadas", "Perdidas")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Mis Pujas",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Filtros funcionales (Activas, Ganadas, Perdidas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filterText ->
                    FilterChip(
                        text = filterText,
                        isSelected = filterText.equals(selectedFilter, ignoreCase = true),
                        onClick = { viewModel.onFilterSelected(filterText) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (misPujas.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes pujas en la sección '$selectedFilter'",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Lista de Pujas filtradas
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(misPujas) { puja ->
                        BidCard(item = puja)
                    }
                }
            }
        }

        if (isLoading && misPujas.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1E293B))
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun BidCard(item: BidItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.imageUrl.isNullOrEmpty()) {
                val fullUrl = if (item.imageUrl.startsWith("http")) item.imageUrl
                else "${RetrofitClient.BASE_URL.removeSuffix("/")}${item.imageUrl}"
                AsyncImage(
                    model = fullUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = item.imageRes),
                    error = painterResource(id = item.imageRes),
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Tu puja", color = Color.Gray, fontSize = 11.sp)
                        Text(item.yourBid, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Column {
                        Text("Máx actual", color = Color.Gray, fontSize = 11.sp)
                        Text(item.maxBid, color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(item.statusBgColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.status,
                    color = item.statusTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}