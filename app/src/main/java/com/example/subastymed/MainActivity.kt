package com.example.subastymed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.subastymed.ui.theme.SubastyMedTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubastyMedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootNavigation()
                }
            }
        }
    }
}

@Composable
fun RootNavigation() {
    val rootNavController = rememberNavController()

    NavHost(navController = rootNavController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = rootNavController)
        }
        composable("main") {
            MainAppScreen()
        }
    }
}

@Composable
fun SplashScreen(navController: NavHostController) {
    // Pasa a la pantalla principal después de 2.5 segundos
    LaunchedEffect(key1 = true) {
        delay(2500)
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true }
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_subastymed),
                contentDescription = "Logo SubastyMED",
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SubastyMED",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.width(140.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(3.dp).background(Color(0xFFFF9800)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF9800), shape = CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f).height(3.dp).background(Color(0xFFFF9800)))
            }
        }
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            color = Color(0xFF475569)
        )
    }
}

@Composable
fun MainAppScreen() {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(bottomNavController) },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "inicio",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Aquí mandamos a llamar a la función que guardaste en HomeScreen.kt
            composable("inicio") { HomeScreen() }

            composable("buscar") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Búsqueda", color = Color.White)
                }
            }
            composable("pujas") { BidsScreen() }

            composable("perfil") { ProfileScreen() }

            }
        }
    }

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Triple("Inicio", Icons.Default.Home, "inicio"),
        Triple("Buscar", Icons.Default.Search, "buscar"),
        Triple("Mis Pujas", Icons.Default.List, "pujas"),
        Triple("Perfil", Icons.Default.Person, "perfil")
    )

    NavigationBar(
        containerColor = Color(0xFF0F172A),
        contentColor = Color.Gray
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { (title, icon, route) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFFF9800),
                    selectedTextColor = Color(0xFFFF9800),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}