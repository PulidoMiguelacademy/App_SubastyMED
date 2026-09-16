package com.example.subastymed.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.subastymed.R
import com.example.subastymed.model.AuctionItem
import com.example.subastymed.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    // Escuchamos los estados del ViewModel
    val subastas by viewModel.filteredAuctions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))
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
            AuctionCard(item)
        }
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
                painter = painterResource(id = R.drawable.logo_subastymed), // Asegúrate de tener este logo en res/drawable
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
        onValueChange = onQueryChange, // Actualiza el estado en el ViewModel al escribir
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
    val categories = listOf("Todos", "Electrónica", "Vehículos", "Hogar", "Arte", "Moda")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        items(categories.size) { index ->
            val categoryName = categories[index]
            val isSelected = categoryName == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1E293B))
                    .clickable { onCategorySelected(categoryName) } // Avisa al ViewModel
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
fun AuctionCard(item: AuctionItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Image(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Etiqueta de tiempo por encima de la imagen
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(item.timeRemaining, color = Color.White, fontSize = 12.sp)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(item.description, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Oferta actual", color = Color.Gray, fontSize = 10.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$${item.currentBid}", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${item.bidCount} pujas", color = Color(0xFFFF9800), fontSize = 10.sp, modifier = Modifier.background(Color(0xFF2D1F16), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
        }
    }
}