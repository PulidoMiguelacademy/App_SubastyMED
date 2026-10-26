# 📱 Documentación Técnica del Frontend - SubastyMED (Android)

## 📌 1. Visión General
El frontend de **SubastyMED** es una aplicación móvil nativa para Android desarrollada con **Kotlin** y **Jetpack Compose**. La aplicación implementa la arquitectura moderna **MVVM (Model-View-ViewModel)** con flujo unidireccional de datos (UDF), garantizando alta reactividad, fluidez visual y una experiencia de usuario inmersiva en modo oscuro (*Dark Slate Luxury Theme*).

La app cuenta con soporte nativo y optimizado tanto para **orientación vertical (Portrait)** como **orientación horizontal (Landscape y Tablets)**.

---

## 🏗️ 2. Stack Tecnológico y Dependencias

| Componente | Tecnología | Versión / Detalle |
| :--- | :--- | :--- |
| **Lenguaje** | Kotlin | Versión 2.0+ con Coroutines & StateFlow |
| **Toolkit de UI** | Jetpack Compose | Material Design 3 (BoM) |
| **Android SDK** | Compile / Target SDK 37 | Min SDK 24 (Compatible con Android 7.0 hasta Android 15) |
| **Arquitectura** | MVVM | StateFlow, ViewModel, LiveState |
| **Navegación** | Navigation Compose | Rutas tipadas y navegación por BackStack |
| **Carga de Imágenes** | Coil Compose | Renderizado asíncrono de URLs remotas, URIs locales y Base64 |
| **Base de Datos & Auth** | Google Firebase | Firebase Auth, Cloud Firestore, Cloud Storage |
| **Networking Local** | Retrofit 2 + OkHttp 3 | Cliente HTTP inteligente con interceptor de auto-fallback |
| **Persistencia Local** | SharedPreferences | `SessionManager` y `LocalAuctionCache` |

---

## 📂 3. Estructura de Paquetes (`com.example.subastymed`)

```text
app/src/main/java/com/example/subastymed/
├── MainActivity.kt                  # Actividad principal, barra de navegación inferior y rutas
├── model/
│   ├── AuctionItem.kt               # Modelo de dominio para las subastas
│   ├── BidItem.kt                   # Modelo de dominio para las pujas
│   └── LocalAuctionCache.kt         # Caché en memoria y persistencia local sin conexión
├── network/
│   ├── ApiService.kt                # Interfaces Retrofit para el backend Python
│   ├── FirebaseManager.kt           # Gestor maestro de Firebase Auth, Firestore y Storage
│   ├── NetworkModels.kt             # DTOs de red (UserDto, AuctionDto, BidDto, etc.)
│   └── RetrofitClient.kt            # Cliente HTTP con SessionManager integrado
├── ui/theme/
│   ├── Color.kt                     # Paleta de colores oficial (Azul Pizarra, Naranja Dorado, etc.)
│   ├── Theme.kt                     # Configuración del tema Material 3
│   ├── Type.kt                      # Tipografía del sistema
│   ├── AuthScreens.kt               # Pantallas de Iniciar Sesión, Registro y Olvido de Contraseña
│   ├── HomeScreen.kt                # Pantalla principal (Feed adaptativo y Detalle de Subasta)
│   ├── CreateAuctionScreen.kt       # Pantalla guiada para crear subastas con fotos reales
│   ├── BidsScreen.kt                # Pantalla con historial y estado de pujas (Filtros en tiempo real)
│   └── Profilescreen.kt             # Perfil, edición de credibilidad y Panel de Administración
└── viewmodel/
    ├── HomeViewModel.kt             # Lógica del feed, búsqueda en vivo y filtros de categoría
    ├── CreateAuctionViewModel.kt    # Lógica de subida, compresión y creación de subasta
    ├── BidsViewModel.kt             # Lógica de filtrado de pujas (Activas, Ganadas, Perdidas)
    └── ProfileViewModel.kt          # Gestión de usuario, contraseñas y operaciones de Administrador
```

---

## 🖥️ 4. Módulos y Pantallas Principales

### 🏠 1. Pantalla de Inicio (`HomeScreen.kt`)
* **Cuadrícula Adaptativa (Responsive Grid):**
  * En modo **Vertical**: Organiza los productos en **2 columnas** proporcionales.
  * En modo **Horizontal / Tablet**: Detecta la orientación (`LocalConfiguration.current`) y expande la cuadrícula automáticamente a **3 o 4 columnas**.
* **Proporción de Imagen (`Aspect Ratio 16:11`):**
  * Las fotos no se recortan en tiras horizontales; se ajustan manteniendo una proporción fotográfica natural.
* **Barra de Búsqueda y Filtros:** Búsqueda en tiempo real por título/descripción y carrusel de categorías (*Electrónica, Vehículos, Moda, Arte, Hogar, Bicicletas*).
* **Diálogo de Detalle (`AuctionDetailDialog`):**
  * Muestra imagen ampliada, descripción completa, fecha y hora de inicio, oferta actual y cantidad de pujas.
  * Si el usuario es **Administrador**, habilita el botón rojo de **"Eliminar Subasta (Admin)"** directamente desde el feed.

---

### ➕ 2. Crear Subasta (`CreateAuctionScreen.kt`)
* **Paso 1 - Selección de Categoría:** Selector interactivo de chips con íconos temáticos.
* **Paso 2 - Selección de Imagen:** Integración directa con la galería del celular mediante `ActivityResultContracts.GetContent()`.
* **Paso 3 - Detalles:** Campos para Título, Descripción y Precio inicial (permite libremente puntos y comas, ej. `100,000` o `100.000`).
* **Paso 4 - Selector de Fecha y Hora:** Ventanas de diálogo nativas secuenciales (`DatePickerDialog` -> `TimePickerDialog`) para fijar la hora exacta de la subasta.
* **Diseño Responsivo:** En modo horizontal, el formulario se autocentra (`widthIn(max = 680.dp)`), evitando campos excesivamente anchos.

---

### 🏷️ 3. Mis Pujas (`BidsScreen.kt`)
* Muestra el estado de las pujas del usuario agrupadas mediante chips de filtro:
  * **Activas:** Subastas en curso donde el usuario tiene ofertas.
  * **Ganadas:** Artículos adjudicados al usuario.
  * **Perdidas:** Subastas donde su oferta fue superada al cierre.
* **Responsividad:** Muestra una columna en vertical y se divide automáticamente en **2 columnas en modo horizontal**.

---

### 👤 4. Perfil y Credibilidad (`Profilescreen.kt`)
* **Cabecera Dinámica:** Muestra la foto de perfil real (cargada desde nube o galería), nombre completo, nombre de usuario `@`, reputación y fecha de ingreso.
* **Insignia Oficial de Verificación:** Muestra el sello verde `✔ Usuario Verificado` para usuarios con credenciales validadas.
* **Editar Perfil y Credibilidad:** Diálogo interactivo donde el usuario puede subir una nueva foto de perfil, agregar teléfono, ciudad de residencia, biografía personal y documento de identidad opcional.
* **Cambio de Contraseña:** Diálogo interactivo que actualiza de forma segura la clave en Firebase Auth mediante `updatePassword`.
* **Mis Subastas:** Gestión de las subastas publicadas por el usuario, con opciones para **Modificar** datos o **Eliminar**.

---

## 🛡️ 5. Módulo de Administración General (Admin)

El sistema incluye una cuenta y rol de Administrador con privilegios globales sobre toda la plataforma:

### 🔑 Credenciales por Defecto
* **Usuario:** `admin` o correo: `admin@subastymed.com`
* **Contraseña:** `admin123`

### ⚙️ Capacidades del Administrador:
1. **Insignia Especial:** En el perfil se exhibe el distintivo dorado `⭐ ADMIN • Administrador Oficial (Control Total)`.
2. **Acceso al Panel de Control:** Opción exclusiva en el menú del perfil: `🛡️ Panel de Administración (Control Total)`.
3. **Pestaña Subastas Globales:**
   * Lista **todas las subastas** de la plataforma, sin importar quién las haya creado.
   * Permite **Modificar** (título, precio, descripción, categoría) cualquier subasta.
   * Permite **Eliminar** permanentemente cualquier subasta con diálogo de confirmación.
4. **Pestaña Usuarios Registrados:**
   * Muestra la totalidad de usuarios registrados con sus datos de contacto (nombre, correo, teléfono, ciudad).
   * **Alternar Verificación:** El Administrador puede conceder o revocar el sello de verificación con un solo toque (`toggleUserVerification`).
   * **Eliminar Usuario:** Puede eliminar cuentas de usuario problemáticas o inactivas.

---

## 🔄 6. Sincronización y Tiempo Real

* **Listener de Firestore (`listenToAuctions`):**  
  Implementa `addSnapshotListener` sobre la colección `auctions` de Cloud Firestore. Cualquier subasta creada, modificada o eliminada se refleja instantáneamente en las pantallas de todos los usuarios sin recargar la aplicación.
* **Manejo de Imágenes Gratuito y Resiliente:**  
  La aplicación intenta subir la imagen a Firebase Cloud Storage. Si la cuenta opera bajo el plan gratuito Spark (sin tarjeta de crédito), comprime la foto inteligentemente a formato JPEG liviano (~30-50 KB) y la codifica en **Base64** directamente dentro del documento de Firestore, garantizando que **las fotos siempre se guarden y se vean en todos los dispositivos al 100% gratis**.

---

## 📦 7. Compilación y Generación del APK

Para compilar el proyecto y generar el archivo instalable:
```powershell
.\gradlew assembleDebug
```
El archivo ejecutable resultante se ubica en:
* `app/build/outputs/apk/debug/app-debug.apk`
* Copia directa lista para instalar: `SubastyMED_v2.apk`
