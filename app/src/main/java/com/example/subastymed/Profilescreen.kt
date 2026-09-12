package com.example.subastymed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Fondo azul oscuro
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Cabecera del Perfil
        ProfileHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Fila de Estadísticas (Ganadas, Activas, Favoritos)
        StatsRow()

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Opciones del Menú
        MenuOptionItem(icon = Icons.Default.Notifications, text = "Notificaciones")
        Spacer(modifier = Modifier.height(12.dp))
        MenuOptionItem(icon = Icons.Default.ShoppingCart, text = "Métodos de Pago")
        Spacer(modifier = Modifier.height(12.dp))
        MenuOptionItem(icon = Icons.Default.DateRange, text = "Historial de Subastas")
        Spacer(modifier = Modifier.height(12.dp))
        MenuOptionItem(icon = Icons.Default.Info, text = "Ayuda y Soporte")
        Spacer(modifier = Modifier.height(12.dp))

        // Botón de Cerrar Sesión con color rojo/naranja
        MenuOptionItem(
            icon = Icons.Default.ExitToApp,
            text = "Cerrar Sesión",
            textColor = Color(0xFFEF4444),
            iconColor = Color(0xFFEF4444)
        )
    }
}

@Composable
fun ProfileHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Reemplazamos el Box gris por la foto de perfil
        Image(
            painter = painterResource(id = R.drawable.img_perfil), // Aquí llama a tu imagen
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop, // Esto hace que la imagen llene el círculo perfectamente
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Textos del perfil
        Column {
            Text(
                text = "Dr. Alejandro Silva",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Calificación",
                    tint = Color(0xFFFF9800), // Naranja
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "4.9 • Miembro desde 2023",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox(number = "14", label = "Ganadas", modifier = Modifier.weight(1f))
        StatBox(number = "3", label = "Activas", modifier = Modifier.weight(1f))
        StatBox(number = "28", label = "Favoritos", modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatBox(number: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(vertical = 16.dp)
    ) {
        Text(text = number, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun MenuOptionItem(
    icon: ImageVector,
    text: String,
    textColor: Color = Color.White,
    iconColor: Color = Color.Gray
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .clickable { /* Acción al presionar */ }
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        // Icono de flecha a la derecha
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Ir",
            tint = Color.Gray
        )
    }
}