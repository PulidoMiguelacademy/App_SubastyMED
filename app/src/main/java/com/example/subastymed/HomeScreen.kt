package com.example.subastymed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    // Aquí es donde vinculas el nombre de la imagen (ej: R.drawable.img_macbook)
    val subastas = listOf(
        AuctionItem("MacBook Pro 16\" M4", "Excelente estado, 32GB...", "$1,250.00", "02h 15m", "14 pujas", R.drawable.img_macbook),
        AuctionItem("BMW Serie 3 2021", "Impecable, 35,000 km", "$24,800.00", "1d 04h", "45 pujas", R.drawable.img_bmw),
        AuctionItem("Chaqueta Cuero Retro", "Vintage, Talla L, Unisex", "$180.00", "45m 12s", "8 pujas", R.drawable.img_chaqueta),
        AuctionItem("Oleo Abstracto", "Autor firmado, 120x80cm", "$620.00", "05h 22m", "10 pujas", R.drawable.img_oleo)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))
    ) {
        item(span = { GridItemSpan(2) }) { TopHeader() }
        item(span = { GridItemSpan(2) }) { SearchBar() }
        item(span = { GridItemSpan(2) }) { CategoryChips() }
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
            // Reemplazamos la caja gris por tu logo
            Image(
                painter = painterResource(id = R.drawable.logo_subastymed),
                contentDescription = "Logo SubastyMED",
                contentScale = ContentScale.Crop, // Esto asegura que llene el círculo
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
fun SearchBar() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
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
fun CategoryChips() {
    val categories = listOf("Electrónica", "Vehículos", "Hogar", "Arte", "Moda")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        items(categories.size) { index ->
            val isSelected = index == 0
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(categories[index], color = if (isSelected) Color.White else Color.LightGray)
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
            // Reemplazamos la caja oscura por la imagen del producto
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Image(
                    painter = painterResource(id = item.imageRes), // Carga la imagen de la lista
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop, // Recorta la imagen para que llene el espacio sin deformarse
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
                    Text(item.time, color = Color.White, fontSize = 12.sp)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(item.subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Oferta actual", color = Color.Gray, fontSize = 10.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item.price, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(item.bids, color = Color(0xFFFF9800), fontSize = 10.sp, modifier = Modifier.background(Color(0xFF2D1F16), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// Actualizamos el modelo para que reciba la imagen (imageRes)
data class AuctionItem(
    val title: String,
    val subtitle: String,
    val price: String,
    val time: String,
    val bids: String,
    val imageRes: Int
)