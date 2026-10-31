package com.example.subastymed.ui.theme

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.subastymed.R
import com.example.subastymed.model.AuctionItem
import com.example.subastymed.network.RetrofitClient
import com.example.subastymed.network.UserDto
import com.example.subastymed.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val myAuctions by viewModel.myAuctions.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allAuctions by viewModel.allAuctions.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    // Estados para controlar los diálogos interactivos
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showMyAuctionsDialog by remember { mutableStateOf(false) }
    var showAdminPanelDialog by remember { mutableStateOf(false) }
    var adminAuctionToEdit by remember { mutableStateOf<AuctionItem?>(null) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 820.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

        // 1. Cabecera del Perfil dinámico con foto y datos de credibilidad
        ProfileHeader(
            user = user,
            onEditProfileClick = { showEditProfileDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Fila de Estadísticas con Datos 100% Reales (Ganadas y Activas)
        StatsRow(
            wonCount = stats.wonCount.toString(),
            activeCount = stats.activeBidsCount.toString(),
            onWonClick = {
                Toast.makeText(context, "Tienes ${stats.wonCount} subasta(s) ganada(s)", Toast.LENGTH_SHORT).show()
            },
            onActiveClick = {
                Toast.makeText(context, "Tienes ${stats.activeBidsCount} puja(s) activa(s)", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Opciones del Menú con Funcionalidad Completa
        if (user?.isAdmin == true) {
            MenuOptionItem(
                icon = Icons.Default.AdminPanelSettings,
                text = "Panel de Administración (Control Total)",
                textColor = Color(0xFFFF9800),
                iconColor = Color(0xFFFF9800),
                onClick = {
                    viewModel.loadAdminData()
                    showAdminPanelDialog = true
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        MenuOptionItem(
            icon = Icons.Default.Person,
            text = "Editar Perfil y Credibilidad",
            onClick = { showEditProfileDialog = true }
        )
        Spacer(modifier = Modifier.height(12.dp))

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

        MenuOptionItem(
            icon = Icons.Default.Lock,
            text = "Cambiar Contraseña",
            onClick = { showChangePasswordDialog = true }
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
    }

    // --- DIÁLOGOS INTERACTIVOS ---

    // 0. Diálogo "Editar Perfil y Credibilidad"
    if (showEditProfileDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditProfileDialog = false },
            onSave = { fullName, phone, city, bio, idDocument, newAvatarUri ->
                viewModel.updateProfile(
                    context = context,
                    fullName = fullName,
                    phone = phone,
                    city = city,
                    bio = bio,
                    idDocument = idDocument,
                    newAvatarUri = newAvatarUri
                ) {
                    showEditProfileDialog = false
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(onDismiss = { showChangePasswordDialog = false })
    }

    // 1. Diálogo "Mis Subastas" con opción de modificar y eliminar
    if (showMyAuctionsDialog) {
        Dialog(
            onDismissRequest = { showMyAuctionsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f)
                    .padding(4.dp)
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
                                        viewModel.deleteAuction(auction.id) {}
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
                viewModel.updateAuction(
                    id = auction.id,
                    title = newTitle,
                    description = newDesc,
                    newOfferPrice = newPrice,
                    category = newCategory
                ) {
                    auctionToEdit = null
                }
            }
        )
    }

    // Diálogo del Panel de Administración
    if (showAdminPanelDialog) {
        AdminPanelDialog(
            allAuctions = allAuctions,
            allUsers = allUsers,
            onDismiss = { showAdminPanelDialog = false },
            onEditAuction = { auction ->
                adminAuctionToEdit = auction
            },
            onDeleteAuction = { auction ->
                viewModel.deleteAnyAuction(auction.id)
            },
            onToggleUserVerification = { userDto ->
                viewModel.toggleUserVerification(userDto)
            },
            onDeleteUser = { userDto ->
                viewModel.deleteUser(userDto)
            }
        )
    }

    adminAuctionToEdit?.let { auction ->
        EditAuctionDialog(
            auction = auction,
            onDismiss = { adminAuctionToEdit = null },
            onSave = { newTitle, newDesc, newPrice, newCategory ->
                viewModel.updateAnyAuction(
                    auctionId = auction.id,
                    title = newTitle,
                    description = newDesc,
                    price = newPrice,
                    category = newCategory
                )
                adminAuctionToEdit = null
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
                        viewModel.logout(context) {
                            Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                            onLogout()
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
            val imageModel = remember(auction.imageUrl, auction.imageRes) {
                auction.getResolvedImage()
            }
            AsyncImage(
                model = imageModel,
                contentDescription = auction.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = auction.imageRes),
                error = painterResource(id = auction.imageRes),
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(auction.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text("Oferta: ${auction.getDisplayPrice()}", color = Color(0xFFFF9800), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
    var editPrice by remember { mutableStateOf(auction.currentBid.toLong().toString()) }
    var editCategory by remember { mutableStateOf(auction.category) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.94f)
                .padding(4.dp)
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
                        val parsedPrice = AuctionItem.parsePriceInput(editPrice) ?: auction.currentBid
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.94f)
                .padding(4.dp)
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
    user: UserDto?,
    onEditProfileClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Foto de perfil real con botón para cambiar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFF9800), CircleShape)
                        .clickable { onEditProfileClick() }
                ) {
                    AsyncImage(
                        model = user?.getResolvedAvatar() ?: R.drawable.img_perfil,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.img_perfil),
                        error = painterResource(id = R.drawable.img_perfil),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user?.fullName ?: "Usuario SubastyMED",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (user?.isAdmin == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF9800))
                            ) {
                                Text(
                                    text = "ADMIN",
                                    color = Color(0xFFFF9800),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (user?.isVerified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verificado",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "@${user?.username ?: "usuario"}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Calificación",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${user?.rating ?: 5.0} • Miembro desde ${user?.memberSince ?: "2026"}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Datos de Credibilidad: Ubicación y Teléfono
            if (!user?.city.isNullOrBlank() || !user?.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!user?.city.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(user!!.city!!, color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                    if (!user?.phone.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(user!!.phone!!, color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Biografía / Descripción personal
            if (!user?.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${user?.bio}\"",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Barra inferior de verificación y botón editar
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user?.isAdmin == true) {
                    Surface(
                        color = Color(0x33FF9800),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Administrador Oficial (Control Total)", color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (user?.isVerified == true) {
                    Surface(
                        color = Color(0x224ADE80),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF4ADE80))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Usuario Verificado", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        color = Color(0x22F59E0B),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añade datos para credibilidad", color = Color(0xFFF59E0B), fontSize = 11.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onEditProfileClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar Perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    user: UserDto?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, phone: String, city: String, bio: String, idDocument: String, avatarUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(user?.fullName ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var city by remember { mutableStateOf(user?.city ?: "") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var idDocument by remember { mutableStateOf(user?.idDocument ?: "") }
    var newAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newAvatarUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Editar Perfil",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector de Foto
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFF9800), CircleShape)
                        .clickable { avatarLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (newAvatarUri != null) {
                        AsyncImage(
                            model = newAvatarUri,
                            contentDescription = "Nueva foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = user?.getResolvedAvatar() ?: R.drawable.img_perfil,
                            contentDescription = "Foto actual",
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.img_perfil),
                            error = painterResource(id = R.drawable.img_perfil),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Cambiar",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (newAvatarUri != null) "Nueva foto seleccionada" else "Toca para cambiar foto",
                    color = Color(0xFFFF9800),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Nombre Completo (Obligatorio)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre Completo (Obligatorio) *", color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFF9800)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Banner Informativo de Credibilidad
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Credibilidad y Confianza",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Completar estos datos opcionales te otorga la insignia de Usuario Verificado y mayor confianza en tus subastas.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Teléfono / WhatsApp (Obligatorio)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono / WhatsApp (Obligatorio) *", color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("Ej. 300 123 4567 (Para contacto de entrega)", color = Color.DarkGray) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFFF9800)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Ciudad / Ubicación (Obligatorio)
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Ciudad / Ubicación (Obligatorio) *", color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("Ej. Medellín, Antioquia", color = Color.DarkGray) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF9800)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Descripción / Biografía
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biografía / Sobre Mí (Opcional)", color = Color.Gray) },
                    placeholder = { Text("Ej. Vendedor de tecnología confiable...", color = Color.DarkGray) },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFFF9800)) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Documento de Identidad / Cédula
                OutlinedTextField(
                    value = idDocument,
                    onValueChange = { idDocument = it },
                    label = { Text("N° Documento / Cédula (Opcional)", color = Color.Gray) },
                    placeholder = { Text("Ej. C.C. 1020304050", color = Color.DarkGray) },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFFF9800)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val trimmedName = fullName.trim()
                            val trimmedPhone = phone.trim()
                            val trimmedCity = city.trim()
                            if (trimmedName.isBlank() || trimmedPhone.isBlank() || trimmedCity.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "⚠️ Nombre completo, Teléfono y Ciudad son OBLIGATORIOS para la credibilidad de tu perfil.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                onSave(trimmedName, trimmedPhone, trimmedCity, bio.trim(), idDocument.trim(), newAvatarUri)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(
    wonCount: String,
    activeCount: String,
    onWonClick: () -> Unit = {},
    onActiveClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatBox(number = wonCount, label = "Ganadas", onClick = onWonClick, modifier = Modifier.weight(1f))
        StatBox(number = activeCount, label = "Activas", onClick = onActiveClick, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatBox(
    number: String,
    label: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Text(text = number, color = Color(0xFFFF9800), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.94f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cambiar Contraseña",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva Contraseña", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF9800)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Contraseña", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF9800)) },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = if (isSuccess) Color(0xFF4ADE80) else Color(0xFFF87171),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val pass = newPassword.trim()
                        val conf = confirmPassword.trim()
                        if (pass.length < 6) {
                            statusMessage = "La contraseña debe tener al menos 6 caracteres"
                            isSuccess = false
                            return@Button
                        }
                        if (pass != conf) {
                            statusMessage = "Las contraseñas no coinciden"
                            isSuccess = false
                            return@Button
                        }

                        isUpdating = true
                        statusMessage = null
                        coroutineScope.launch {
                            val res = com.example.subastymed.network.FirebaseManager.changePassword(pass)
                            isUpdating = false
                            if (res.isSuccess) {
                                isSuccess = true
                                statusMessage = "¡Contraseña actualizada exitosamente!"
                            } else {
                                isSuccess = false
                                statusMessage = res.exceptionOrNull()?.message ?: "Error al actualizar contraseña"
                            }
                        }
                    },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Actualizar Contraseña", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- COMPONENTES DEL PANEL DE ADMINISTRACIÓN ---

@Composable
fun AdminPanelDialog(
    allAuctions: List<AuctionItem>,
    allUsers: List<UserDto>,
    onDismiss: () -> Unit,
    onEditAuction: (AuctionItem) -> Unit,
    onDeleteAuction: (AuctionItem) -> Unit,
    onToggleUserVerification: (UserDto) -> Unit,
    onDeleteUser: (UserDto) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var auctionToDelete by remember { mutableStateOf<AuctionItem?>(null) }
    var userToDelete by remember { mutableStateOf<UserDto?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp
    val isTabletOrWide = screenWidth >= 600 || isLandscape
    val gridColumns = if (isTabletOrWide) 2 else 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .widthIn(max = if (isTabletOrWide) 860.dp else 500.dp)
                .fillMaxWidth(if (isTabletOrWide) 0.92f else 0.95f)
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Panel de Control Admin",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs (Subastas / Usuarios)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFFFF9800)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Subastas (${allAuctions.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Color(0xFFFF9800) else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Usuarios (${allUsers.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Color(0xFFFF9800) else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                if (selectedTab == 0) {
                    // TAB: SUBASTAS
                    if (allAuctions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay subastas registradas en la plataforma.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(allAuctions) { auction ->
                                AdminAuctionCardItem(
                                    auction = auction,
                                    onEdit = { onEditAuction(auction) },
                                    onDelete = { auctionToDelete = auction }
                                )
                            }
                        }
                    }
                } else {
                    // TAB: USUARIOS
                    if (allUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron usuarios registrados.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(allUsers) { userDto ->
                                AdminUserCardItem(
                                    user = userDto,
                                    onToggleVerification = { onToggleUserVerification(userDto) },
                                    onDelete = { userToDelete = userDto }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmación para eliminar subasta
    auctionToDelete?.let { auction ->
        AlertDialog(
            onDismissRequest = { auctionToDelete = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Eliminar Subasta", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar permanentemente la subasta \"${auction.title}\" de la plataforma?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAuction(auction)
                        auctionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { auctionToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // Confirmación para eliminar usuario
    userToDelete?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Eliminar Usuario", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar al usuario ${targetUser.fullName} (@${targetUser.username}) de la plataforma?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(targetUser)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar Usuario", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AdminAuctionCardItem(
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = auction.getResolvedImage(),
                    contentDescription = auction.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = auction.imageRes),
                    error = painterResource(id = auction.imageRes),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(auction.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    Text("Precio: ${auction.getDisplayPrice()}", color = Color(0xFFFF9800), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${auction.category} • ${auction.bidCount} pujas", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modificar", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminUserCardItem(
    user: UserDto,
    onToggleVerification: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = user.getResolvedAvatar(),
                    contentDescription = user.fullName,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.img_perfil),
                    error = painterResource(id = R.drawable.img_perfil),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFFF9800), CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.fullName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        if (user.isAdmin) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF9800))
                            ) {
                                Text("ADMIN", color = Color(0xFFFF9800), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        } else if (user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                        }
                    }
                    Text("@${user.username} • ${user.email}", color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                    if (!user.phone.isNullOrBlank() || !user.city.isNullOrBlank()) {
                        Text(
                            listOfNotNull(user.city.takeIf { !it.isNullOrBlank() }, user.phone.takeIf { !it.isNullOrBlank() }).joinToString(" • "),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Cambiar Verificación
                if (user.isVerified) {
                    OutlinedButton(
                        onClick = onToggleVerification,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Quitar Verificado", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onToggleVerification,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verificar Usuario", fontSize = 11.sp, color = Color.White)
                    }
                }

                // Botón Eliminar Usuario (si no es admin)
                if (!user.isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Usuario", tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}