package com.example.subastymed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun BidsScreen() {
    // Agregamos la referencia a las imágenes (imageRes)
    val misPujas = listOf(
        BidItem(
            title = "MacBook Pro 16\" M4",
            yourBid = "$1,250.00",
            maxBid = "$1,250.00",
            status = "Ganando",
            statusTextColor = Color(0xFF4ADE80), // Verde claro
            statusBgColor = Color(0xFF14532D),   // Verde oscuro
            imageRes = R.drawable.img_macbook    // Tu imagen
        ),
        BidItem(
            title = "BMW Serie 3 2021",
            yourBid = "$24,200.00",
            maxBid = "$24,800.00",
            status = "Superado",
            statusTextColor = Color(0xFFFBBF24), // Amarillo/Naranja claro
            statusBgColor = Color(0xFF78350F),   // Marrón/Naranja oscuro
            imageRes = R.drawable.img_bmw        // Tu imagen
        ),
        BidItem(
            title = "Chaqueta Cuero Retro",
            yourBid = "$180.00",
            maxBid = "$180.00",
            status = "Ganada",
            statusTextColor = Color(0xFFA78BFA), // Morado claro
            statusBgColor = Color(0xFF312E81),   // Morado oscuro
            imageRes = R.drawable.img_chaqueta   // Tu imagen
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Fondo azul oscuro
            .padding(16.dp)
    ) {
        // Título de la pantalla
        Text(
            text = "Mis Pujas",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Filtros (Activas, Ganadas, Perdidas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(text = "Activas", isSelected = true, modifier = Modifier.weight(1f))
            FilterChip(text = "Ganadas", isSelected = false, modifier = Modifier.weight(1f))
            FilterChip(text = "Perdidas", isSelected = false, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lista de Pujas
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(misPujas) { puja ->
                BidCard(item = puja)
            }
        }
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1E293B))
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            )
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
        border = BorderStroke(1.dp, Color(0xFF334155)), // Borde sutil
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reemplazamos el Box gris por la Imagen
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop, // Recorta para llenar el cuadrado sin deformar
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Textos centrales
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
                        Text(item.maxBid, color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) // Naranja
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Etiqueta de estado (Ganando, Superado, etc.)
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

// Modelo de datos actualizado con la imagen
data class BidItem(
    val title: String,
    val yourBid: String,
    val maxBid: String,
    val status: String,
    val statusTextColor: Color,
    val statusBgColor: Color,
    val imageRes: Int // Agregamos este parámetro
)