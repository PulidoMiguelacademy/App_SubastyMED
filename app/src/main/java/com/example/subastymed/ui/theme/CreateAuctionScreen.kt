package com.example.subastymed.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateAuctionScreen() {
    // Variables de estado para los campos de texto (luego los moveremos al ViewModel)
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startPrice by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Bicicletas") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HeaderSection()

        Spacer(modifier = Modifier.height(24.dp))

        // PASO 1: Categoría
        StepHeader(stepNumber = "1", title = "Categoría", subtitle = "Selecciona la categoría de tu artículo")
        Spacer(modifier = Modifier.height(12.dp))
        CategorySelector(selectedCategory) { selectedCategory = it }

        Spacer(modifier = Modifier.height(24.dp))

        // PASO 2: Fotos
        StepHeader(stepNumber = "2", title = "Fotos del artículo", subtitle = "Agrega al menos 1 foto (máx. 5)")
        Spacer(modifier = Modifier.height(12.dp))
        PhotoUploadBox()

        Spacer(modifier = Modifier.height(24.dp))

        // Formulario
        CustomTextField(
            value = title,
            onValueChange = { title = it },
            label = "Título del artículo",
            placeholder = "Ej: iPhone 14 Pro, Bicicleta de montaña...",
            icon = Icons.Default.Sell // Reemplazo de icono de etiqueta
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = description,
            onValueChange = { description = it },
            label = "Descripción",
            placeholder = "Describe el estado, características y detalles importantes del artículo...",
            icon = Icons.Default.Description,
            isMultiline = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                CustomTextField(
                    value = startPrice,
                    onValueChange = { startPrice = it },
                    label = "Precio inicial",
                    placeholder = "Ej: 500.000",
                    icon = Icons.Default.AttachMoney
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                // Simulación de Dropdown para duración
                CustomTextField(
                    value = "",
                    onValueChange = {},
                    label = "Duración de la subasta",
                    placeholder = "Selecciona",
                    icon = Icons.Default.CalendarToday,
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    readOnly = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón Crear Subasta
        Button(
            onClick = { /* Lógica de crear subasta */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Crear subasta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp)) // Espacio para la barra de navegación inferior
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { /* Volver atrás */ },
            modifier = Modifier.background(Color(0xFF1E293B), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Crear Subasta", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Publica tu artículo y comienza la puja. ¡Es fácil y rápido!", color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
        }
        // Aquí iría tu imagen del mazo. Usamos un ícono de reemplazo por ahora
        Icon(Icons.Default.Gavel, contentDescription = "Mazo", tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp))
    }
}

@Composable
fun StepHeader(stepNumber: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFFF9800), CircleShape)
        ) {
            Text(stepNumber, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
    }
}

@Composable
fun CategorySelector(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf(
        Pair("Bicicletas", Icons.AutoMirrored.Filled.DirectionsBike),
        Pair("Videojuegos", Icons.Default.SportsEsports),
        Pair("Electrodomés...", Icons.Default.LocalLaundryService), // Acortado para ajustar
        Pair("Consolas", Icons.Default.Computer),
        Pair("Otros", Icons.Default.MoreHoriz)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(categories) { (name, icon) ->
            val isSelected = name == selectedCategory
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFFFF9800) else Color(0xFF1E293B))
                    .clickable { onCategorySelected(name) }
                    .padding(vertical = 16.dp)
            ) {
                Icon(icon, contentDescription = name, tint = if (isSelected) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(name, color = if (isSelected) Color.White else Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun PhotoUploadBox() {
    // Efecto de borde punteado
    val stroke = Stroke(
        width = 4f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFF334155),
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .clickable { /* Abrir galería */ },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PhotoCamera, contentDescription = "Cámara", tint = Color(0xFFFF9800), modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Toca para añadir fotos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Puedes subir varias imágenes", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    trailingIcon: ImageVector? = null,
    isMultiline: Boolean = false,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isMultiline) Modifier.height(120.dp) else Modifier), // Da más altura si es multilínea
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF1E293B),
            focusedBorderColor = Color(0xFF334155),
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        placeholder = { Text(placeholder, color = Color(0xFF475569), fontSize = 12.sp) },
        leadingIcon = {
            // Acomodamos el ícono y el título del campo (label) dentro del TextField
            Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        },
        trailingIcon = trailingIcon?.let {
            { Icon(it, contentDescription = null, tint = Color.White) }
        }
    )
}