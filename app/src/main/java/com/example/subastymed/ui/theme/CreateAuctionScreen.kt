package com.example.subastymed.ui.theme

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.subastymed.viewmodel.CreateAuctionViewModel
import com.example.subastymed.network.SessionManager
import java.util.Calendar

@Composable
fun CreateAuctionScreen(
    viewModel: CreateAuctionViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val startPrice by viewModel.startPrice.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(context, uri)
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var currentUserSnapshot by remember { mutableStateOf(SessionManager.currentUser) }
    val isCredibilityComplete = !currentUserSnapshot?.fullName.isNullOrBlank() &&
            !currentUserSnapshot?.phone.isNullOrBlank() &&
            !currentUserSnapshot?.city.isNullOrBlank()

    // Selector secuencial de Fecha y Hora
    val calendar = Calendar.getInstance()
    var tempDateStr by remember { mutableStateOf("") }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val amPm = if (hourOfDay >= 12) "PM" else "AM"
            val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            val timeFormatted = String.format("%02d:%02d %s", hour12, minute, amPm)
            val fullDateTime = "$tempDateStr - $timeFormatted"
            viewModel.setStartDateTime(fullDateTime)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            tempDateStr = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
            HeaderSection(onBack = onBack)

            Spacer(modifier = Modifier.height(16.dp))

            // REQUISITO OBLIGATORIO DE CREDIBILIDAD Y CONFIANZA
            if (!isCredibilityComplete) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Requisito Obligatorio de Credibilidad",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Para garantizar la seguridad de la comunidad en SubastyMED, los vendedores deben registrar Nombre Completo, Teléfono/WhatsApp y Ciudad antes de publicar un artículo.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Completar mis datos ahora", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

        // PASO 1: Categoría
        StepHeader(stepNumber = "1", title = "Categoría", subtitle = "Selecciona la categoría de tu artículo")
        Spacer(modifier = Modifier.height(12.dp))
        CategorySelector(selectedCategory) { viewModel.selectedCategory.value = it }

        Spacer(modifier = Modifier.height(24.dp))

        // PASO 2: Fotos reales con selección de galería
        StepHeader(stepNumber = "2", title = "Fotos del artículo", subtitle = "Selecciona una foto real de tu galería")
        Spacer(modifier = Modifier.height(12.dp))
        PhotoUploadBox(
            selectedUri = selectedImageUri,
            onPickImage = { photoPickerLauncher.launch("image/*") },
            onClearImage = { viewModel.onImageSelected(context, null) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Formulario de Detalles
        CustomTextField(
            value = title,
            onValueChange = { viewModel.title.value = it },
            label = "Título del artículo",
            placeholder = "Ej: iPhone 14 Pro, Bicicleta de montaña...",
            icon = Icons.Default.Sell
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = description,
            onValueChange = { viewModel.description.value = it },
            label = "Descripción",
            placeholder = "Describe el estado, características y detalles importantes del artículo...",
            icon = Icons.Default.Description,
            isMultiline = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo Espacioso para Precio Inicial (admite puntos y comas)
        CustomTextField(
            value = startPrice,
            onValueChange = { viewModel.startPrice.value = it },
            label = "Precio inicial ($)",
            placeholder = "Ej: 2.000.000 o 1,500,000",
            icon = Icons.Default.AttachMoney,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector rápido: Iniciar Ahora vs Programar Fecha y Hora (Duración: 1 Hora)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val nowCal = Calendar.getInstance()
                    val amPm = if (nowCal.get(Calendar.HOUR_OF_DAY) >= 12) "PM" else "AM"
                    val hour12 = if (nowCal.get(Calendar.HOUR_OF_DAY) % 12 == 0) 12 else nowCal.get(Calendar.HOUR_OF_DAY) % 12
                    val nowStr = String.format(
                        java.util.Locale.US,
                        "%02d/%02d/%04d - %02d:%02d %s",
                        nowCal.get(Calendar.DAY_OF_MONTH),
                        nowCal.get(Calendar.MONTH) + 1,
                        nowCal.get(Calendar.YEAR),
                        hour12,
                        nowCal.get(Calendar.MINUTE),
                        amPm
                    )
                    viewModel.setStartDateTime(nowStr)
                    Toast.makeText(context, "La subasta iniciará en vivo de inmediato (1 hora de duración)", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(42.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("🔴 Iniciar Ahora (1h)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { datePickerDialog.show() },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)),
                modifier = Modifier.weight(1f).height(42.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("📅 Programar Hora", color = Color(0xFFFF9800), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Campo para Fecha y Hora de Inicio con click garantizado por overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            CustomTextField(
                value = startDate,
                onValueChange = {},
                label = "Fecha y hora de inicio (Duración en vivo: 1 Hora)",
                placeholder = "Toca para configurar fecha y hora",
                icon = Icons.Default.CalendarToday,
                trailingIcon = Icons.Default.AccessTime,
                readOnly = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { datePickerDialog.show() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Al llegar la hora de inicio, la sala de pujas se abrirá en vivo durante 1 hora para todos los usuarios.",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón Crear Subasta
        Button(
            onClick = {
                if (!isCredibilityComplete) {
                    Toast.makeText(
                        context,
                        "⚠️ Debes completar Nombre completo, Teléfono y Ciudad en tu perfil antes de publicar.",
                        Toast.LENGTH_LONG
                    ).show()
                    showEditProfileDialog = true
                } else {
                    viewModel.createAuction(context, onSuccess = onBack)
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800),
                disabledContainerColor = Color(0xFF78350F)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Subiendo y creando...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("Crear subasta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        }

        if (showEditProfileDialog) {
            val profileVm: com.example.subastymed.viewmodel.ProfileViewModel = viewModel()
            EditProfileDialog(
                user = currentUserSnapshot,
                onDismiss = { showEditProfileDialog = false },
                onSave = { fullName, phone, city, bio, idDocument, newAvatarUri ->
                    profileVm.updateProfile(
                        context = context,
                        fullName = fullName,
                        phone = phone,
                        city = city,
                        bio = bio,
                        idDocument = idDocument,
                        newAvatarUri = newAvatarUri
                    ) {
                        showEditProfileDialog = false
                        currentUserSnapshot = SessionManager.currentUser
                        Toast.makeText(context, "✅ ¡Datos de credibilidad guardados con éxito!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun HeaderSection(onBack: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(Color(0xFF1E293B), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Crear Subasta", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Publica tu artículo y comienza la puja. ¡Es fácil y rápido!", color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
        }
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
        Pair("Electrónica", Icons.Default.Computer),
        Pair("Moda", Icons.Default.Checkroom),
        Pair("Hogar", Icons.Default.Home),
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
fun PhotoUploadBox(
    selectedUri: Uri?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit
) {
    if (selectedUri != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = selectedUri,
                contentDescription = "Foto seleccionada",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Button(
                    onClick = onPickImage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Cambiar", tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cambiar", color = Color.White, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onClearImage,
                    modifier = Modifier.background(Color(0xFFEF4444).copy(alpha = 0.85f), CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    } else {
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
                .clickable { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Cámara", tint = Color(0xFFFF9800), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Toca para añadir fotos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Sube una foto real desde tu galería", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
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
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = !isMultiline,
        keyboardOptions = keyboardOptions,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isMultiline) Modifier.height(120.dp) else Modifier.height(64.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF1E293B),
            focusedBorderColor = Color(0xFFFF9800),
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFFFF9800),
            unfocusedLabelColor = Color(0xFF94A3B8)
        ),
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF475569), fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}