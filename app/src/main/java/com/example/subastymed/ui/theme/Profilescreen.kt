package com.example.subastymed.ui.theme

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.subastymed.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val myAuctions by viewModel.myAuctions.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    // Estados para controlar los diálogos interactivos
    var showMyAuctionsDialog by remember { mutableStateOf(false) }
    var auctionToEdit by remember { mutableStateOf<AuctionItem?>(null) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showPaymentMethodsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Cabecera del Perfil dinámico
        ProfileHeader(
            fullName = user?.fullName ?: "Dr. Alejandro Silva",
            rating = user?.rating ?: 4.9,
            memberSince = user?.memberSince ?: "2023"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Fila de Estadísticas desde Backend (Ganadas, Activas, Favoritos)
        StatsRow(
            wonCount = stats.wonCount.toString(),
            activeCount = stats.activeBidsCount.toString(),
            favoritesCount = stats.favoritesCount.toString()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 3. Opciones del Menú con Funcionalidad Completa
        MenuOptionItem(
            icon = Icons.Default.Gavel,
            text = "Mis Subastas (${myAuctions.size})",
            onClick = {
                viewModel.loadMyAuctions()
                showMyAuctionsDialog = true
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        MenuOptionItem(
            icon = Icons.Default.Notifications,
            text = "Notificaciones",
            onClick = { showNotificationsDialog = true }
        )
        Spacer(modifier = Modifier.height(12.dp))

        MenuOptionItem(
            icon = Icons.Default.ShoppingCart,
            text = "Métodos de Pago",
            onClick = { showPaymentMethodsDialog = true }
        )
        Spacer(modifier = Modifier.height(12.dp))

        MenuOptionItem(
            icon = Icons.Default.DateRange,
            text = "Historial de Subastas",
            onClick = { showHistoryDialog = true }
        )
        Spacer(modifier = Modifier.height(12.dp))

        MenuOptionItem(
            icon = Icons.Default.Info,
            text = "Ayuda y Soporte",
            onClick = { showHelpDialog = true }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Botón de Cerrar Sesión
        MenuOptionItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            text = "Cerrar Sesión",
            textColor = Color(0xFFEF4444),
            iconColor = Color(0xFFEF4444),
            onClick = { showLogoutDialog = true }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- DIÁLOGOS INTERACTIVOS ---

    // 1. Diálogo "Mis Subastas" con opción de modificar y eliminar
    if (showMyAuctionsDialog) {
        Dialog(onDismissRequest = { showMyAuctionsDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mis Subastas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showMyAuctionsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (myAuctions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No has publicado subastas todavía.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(myAuctions) { auction ->
                                MyAuctionCardItem(
                                    auction = auction,
                                    onEdit = {
                                        auctionToEdit = auction
                                    },
                                    onDelete = {
                                        val idInt = auction.id.toIntOrNull()
                                        if (idInt != null) {
                                            viewModel.deleteAuction(idInt) {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Diálogo para Modificar Oferta y Datos de la Subasta
    auctionToEdit?.let { auction ->
        EditAuctionDialog(
            auction = auction,
            onDismiss = { auctionToEdit = null },
            onSave = { newTitle, newDesc, newPrice, newCategory ->
                val idInt = auction.id.toIntOrNull()
                if (idInt != null) {
                    viewModel.updateAuction(
                        id = idInt,
                        title = newTitle,
                        description = newDesc,
                        newOfferPrice = newPrice,
                        category = newCategory
                    ) {
                        auctionToEdit = null
                    }
                }
            }
        )
    }

    // 3. Diálogo "Notificaciones"
    if (showNotificationsDialog) {
        SimpleInfoDialog(
            title = "Notificaciones",
            onDismiss = { showNotificationsDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NotificationItem("🔔 Oferta recibida", "Tu subasta 'BMW Serie 3' tiene una nueva puja de $24,800.00", "Hace 10 min")
                NotificationItem("🏆 ¡Subasta Ganada!", "Has ganado la subasta 'Chaqueta Cuero Retro' por $180.00", "Hace 2 horas")
                NotificationItem("⚠️ Oferta superada", "Alguien ofertó más en 'MacBook Pro 16\" M4'. Oferta actual: $1,250.00", "Ayer")
            }
        }
    }

    // 4. Diálogo "Métodos de Pago"
    if (showPaymentMethodsDialog) {
        SimpleInfoDialog(
            title = "Métodos de Pago",
            onDismiss = { showPaymentMethodsDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentMethodItem("Visa Débito", "•••• 4242", "Principal", Icons.Default.CreditCard)
                PaymentMethodItem("Nequi / Daviplata", "312 ••• 4589", "Activo", Icons.Default.PhoneAndroid)
                PaymentMethodItem("Efectivo", "Pago contra entrega", "Disponible", Icons.Default.Payments)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Opción para agregar tarjeta o cuenta bancaria", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir Método de Pago", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 5. Diálogo "Historial de Subastas"
    if (showHistoryDialog) {
        SimpleInfoDialog(
            title = "Historial de Subastas",
            onDismiss = { showHistoryDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryItem("Chaqueta Cuero Retro", "Ganada • $180.00", "15 Sept 2026", Color(0xFF4ADE80))
                HistoryItem("Monitor 4K Dell", "Finalizada • $450.00", "02 Sept 2026", Color(0xFF94A3B8))
                HistoryItem("iPad Pro 11\"", "Superada • $720.00", "28 Ago 2026", Color(0xFFEF4444))
            }
        }
    }

    // 6. Diálogo "Ayuda y Soporte"
    if (showHelpDialog) {
        SimpleInfoDialog(
            title = "Ayuda y Soporte",
            onDismiss = { showHelpDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("¿Cómo funciona SubastyMED?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Publica tus artículos o puja en subastas activas. Cuando el tiempo expire, el usuario con la oferta más alta se adjudica el artículo.", color = Color.Gray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))
                Text("¿Tienes dudas o problemas?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Button(
                    onClick = {
                        Toast.makeText(context, "Abriendo soporte WhatsApp (+57 300 000 0000)...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contactar por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Correo de soporte: contacto@subastymed.com", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Correo Electrónico", color = Color.White)
                }
            }
        }
    }

    // 7. Diálogo de confirmación "Cerrar Sesión"
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas cerrar sesión en SubastyMED?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout {
                            Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Cerrar Sesión", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun MyAuctionCardItem(
    auction: AuctionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = auction.imageRes),
                    contentDescription = auction.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(auction.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text("Oferta: $${auction.currentBid}", color = Color(0xFFFF9800), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Categoría: ${auction.category}", color = Color.Gray, fontSize = 11.sp)
            }

            // Botón Modificar Subasta / Oferta
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Modificar", tint = Color(0xFFFF9800))
            }

            // Botón Eliminar
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun EditAuctionDialog(
    auction: AuctionItem,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, price: Double, category: String) -> Unit
) {
    var editTitle by remember { mutableStateOf(auction.title) }
    var editDesc by remember { mutableStateOf(auction.description) }
    var editPrice by remember { mutableStateOf(auction.currentBid.toString()) }
    var editCategory by remember { mutableStateOf(auction.category) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modificar Subasta / Oferta", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Título", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it },
                    label = { Text("Oferta / Precio Actual ($)", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editCategory,
                    onValueChange = { editCategory = it },
                    label = { Text("Categoría", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editDesc,
                    onValueChange = { editDesc = it },
                    label = { Text("Descripción", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val parsedPrice = editPrice.toDoubleOrNull() ?: auction.currentBid
                        onSave(editTitle, editDesc, parsedPrice, editCategory)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SimpleInfoDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun NotificationItem(title: String, desc: String, time: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(time, color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun PaymentMethodItem(title: String, subtitle: String, status: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Text(status, color = Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
fun HistoryItem(title: String, subtitle: String, date: String, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = statusColor, fontSize = 12.sp)
        }
        Text(date, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ProfileHeader(
    fullName: String,
    rating: Double,
    memberSince: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_perfil),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = fullName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Calificación",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$rating • Miembro desde $memberSince",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun StatsRow(
    wonCount: String,
    activeCount: String,
    favoritesCount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox(number = wonCount, label = "Ganadas", modifier = Modifier.weight(1f))
        StatBox(number = activeCount, label = "Activas", modifier = Modifier.weight(1f))
        StatBox(number = favoritesCount, label = "Favoritos", modifier = Modifier.weight(1f))
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
    iconColor: Color = Color.Gray,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .clickable { onClick() }
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Ir",
            tint = Color.Gray
        )
    }
}