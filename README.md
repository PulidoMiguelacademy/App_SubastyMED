<div align="center">
  <img src="app/src/main/res/drawable/logo_subastymed.png" alt="Logo SubastyMED" width="130" />

  # 🔨 SubastyMED
  **Plataforma Móvil de Subastas en Tiempo Real (Android Nativo + Firebase + FastAPI)**

  ![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
  ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
  ![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20Storage-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
  ![FastAPI](https://img.shields.io/badge/Backend-FastAPI%20%26%20SQLite-009688?style=for-the-badge&logo=fastapi&logoColor=white)
  ![Android](https://img.shields.io/badge/Android-MinSDK%2024%20%7C%20Target%2037-3DDC84?style=for-the-badge&logo=android&logoColor=white)
</div>

---

## 📋 Descripción del Proyecto

**SubastyMED** es una aplicación móvil nativa para Android desarrollada en **Kotlin** con **Jetpack Compose** bajo el patrón de arquitectura **MVVM (Model-View-ViewModel)**. Permite a los usuarios publicar artículos en subasta, programar fechas y horas de inicio, participar en **Salas de Subasta en Vivo con duración de 1 hora** y seguir en tiempo real las pujas de todos los participantes conectados desde distintos dispositivos.

La aplicación cuenta con sincronización en la nube en tiempo real mediante **Firebase (Authentication, Cloud Firestore y Cloud Storage)**, respaldo de API REST en **Python (FastAPI)**, aislamiento de sesiones por usuario y una interfaz 100% adaptable tanto para **Celulares** como para **Tablets** (en formato vertical y horizontal).

---

## ✨ Características Principales

- 🔐 **Autenticación Segura y Sesiones Independientes**:
  - Registro e inicio de sesión mediante correo electrónico o nombre de usuario con validación real de credenciales en **Firebase Authentication**.
  - Aislamiento completo de datos y almacenamiento por sesión de usuario (`UID`).
- 🔴 **Sala de Subasta en Vivo (Ventana Activa de 1 Hora)**:
  - Cuando llega la fecha y hora programada de una subasta, se habilita automáticamente una sala interactiva en vivo durante **1 hora** con barra de progreso y cuenta regresiva segundo a segundo.
  - **Tablero de Pujas en Tiempo Real**: Todos los usuarios ven al instante quién lidera la subasta (`🏆 GANANDO`), quiénes han sido superados (`SUPERADO`) y el historial de ofertas registradas.
  - **Ingreso Flexible de Ofertas en Pesos Colombianos (COP)**: Botones de incremento rápido (`+$10.000`, `+$20.000`, `+$30.000`, `+$50.000`) y campo numérico libre compatible con formato de miles (`50000`, `50.000`, `1.500.000`).
  - **Regla de Negocio para Creadores**: El usuario que publica una subasta puede monitorear las ofertas en vivo, pero el sistema bloquea que puje sobre su propio artículo.
- 📊 **Gestión Inteligente de "Mis Pujas"**:
  - Clasificación en tiempo real por usuario en tres pestañas:
    - **Activas**: Pujas en curso con indicador en vivo (*Ganando* / *Superado*).
    - **Ganadas**: Subastas finalizadas adjudicadas al usuario con la oferta más alta.
    - **Perdidas**: Subastas cerradas donde otro postor obtuvo el primer lugar.
- 📸 **Publicación y Gestión de Subastas Propias**:
  - Carga de fotografías reales desde la galería del dispositivo con compresión optimizada y caché en memoria (`LruCache`) para visualización fluida sin parpadeos.
  - Edición de título, descripción, categoría y precio o eliminación desde la sección *"Mis Subastas"*.
- 🛡️ **Panel de Administración (Control Total)**:
  - Rol exclusivo de Administrador con acceso global para editar o eliminar cualquier publicación, iniciar/cerrar subastas en vivo y verificar o administrar usuarios registrados.
- 📱 **Diseño Responsivo (Celular y Tablet)**:
  - Adaptación automática de cuadrículas (1, 2, 3 o 4 columnas) y vista dividida de 2 columnas en la Sala de Subasta para Tablets y pantallas horizontales.

---

## 🗂️ Estructura del Repositorio

```text
App_SubastyMED/
├── app/                                      # Módulo principal de la aplicación Android (Frontend)
│   ├── google-services.json                  # Configuración oficial del proyecto Firebase
│   └── src/main/
│       ├── AndroidManifest.xml               # Permisos, ícono personalizado y actividad principal
│       ├── java/com/example/subastymed/
│       │   ├── MainActivity.kt               # Navegación raíz (Splash, Login, Registro, BottomBar)
│       │   ├── model/                        # Modelos de dominio (AuctionItem, BidItem, LocalAuctionCache)
│       │   ├── network/                      # FirebaseManager, SessionManager, ApiService, RetrofitClient
│       │   ├── viewmodel/                    # ViewModels reactivos con StateFlow (Home, Bids, Create, Profile)
│       │   └── ui/theme/                     # Pantallas en Jetpack Compose (Home, Bids, Create, Profile, Auth)
│       └── res/                              # Recursos gráficos, logo SubastyMED e íconos adaptativos
├── backend/                                  # Servidor API REST opcional en Python (FastAPI + SQLAlchemy)
│   ├── app/                                  # Modelos, esquemas Pydantic, routers, autenticación JWT
│   ├── requirements.txt                      # Dependencias del servidor Python
│   └── run.py                                # Script de arranque del backend local
├── DOCUMENTACION_FRONTEND.md                 # Documentación técnica detallada del Frontend Android
├── DOCUMENTACION_BACKEND.md                  # Documentación técnica detallada del Backend y Firebase
├── SubastyMED_v2.apk                         # Instalador APK universal compilado (Celular y Tablet)
└── README.md                                 # Documentación general y evidencias de compilación
```

---

## 🚀 Instrucciones de Compilación y Prueba

### Opción 1: Instalación directa del APK compilado
En la raíz del repositorio se encuentra el instalador listo para dispositivos Android (Celular o Tablet, Android 7.0 / API 24 en adelante):
- 📦 **[`SubastyMED_v2.apk`](SubastyMED_v2.apk)**

### Opción 2: Compilación desde Android Studio / Terminal
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/PulidoMiguelacademy/App_SubastyMED.git
   ```
2. Abrir la carpeta del proyecto en **Android Studio** (Ladybug / Meerkat o superior con JDK 11+).
3. Sincronizar dependencias de Gradle (*Sync Project with Gradle Files*) y ejecutar en emulador o dispositivo físico, o compilar por consola con:
   ```bash
   ./gradlew assembleDebug
   ```

### 🔑 Credenciales de Prueba (Rol Administrador)
Para revisar las funciones avanzadas de administración, moderación de subastas y gestión de usuarios:
- **Correo / Usuario**: `admin@subastymed.com` *(o simplemente `admin`)*
- **Contraseña**: `admin123`

*(También se puede registrar cualquier cuenta nueva desde la pantalla **"Crear Cuenta"** para probar la interacción en tiempo real entre múltiples usuarios).*

---

## 📚 Documentación Técnica Adicional

- 📄 **[Documentación del Frontend (`DOCUMENTACION_FRONTEND.md`)](DOCUMENTACION_FRONTEND.md)**: Arquitectura MVVM, pantallas en Jetpack Compose, estado reactivo y diseño responsivo.
- 📄 **[Documentación del Backend (`DOCUMENTACION_BACKEND.md`)](DOCUMENTACION_BACKEND.md)**: Estructura de colecciones en Firebase Cloud Firestore (`users`, `auctions`, `bids`, `live_bids`), reglas de sincronización y endpoints FastAPI.

---

## SubastyMed
# Primera entrega 
<img width="958" height="539" alt="image" src="https://github.com/user-attachments/assets/60197bfa-906a-4849-9dd2-76a64ffe0b31" />

<img width="959" height="539" alt="image" src="https://github.com/user-attachments/assets/f541227a-f3cd-423a-8109-a384bac73f49" />

<img width="959" height="539" alt="image" src="https://github.com/user-attachments/assets/198012c6-b358-4e01-ae9b-2855eaf04e94" />

<img width="959" height="539" alt="image" src="https://github.com/user-attachments/assets/98a03ba6-1314-4235-a782-8c8273f43ba0" />

<img width="959" height="539" alt="image" src="https://github.com/user-attachments/assets/9c2809c0-8221-4fef-8b02-52fbd9fcc330" />

<img width="959" height="539" alt="image" src="https://github.com/user-attachments/assets/7c9c4630-1868-4750-9da9-105b4e972ca6" />

# Segunda entrega

<img width="1600" height="1000" alt="image" src="https://github.com/user-attachments/assets/30bc7631-5ffd-44b3-9a63-be56ba46b1c6" />

<img width="1000" height="1600" alt="image" src="https://github.com/user-attachments/assets/4aeafcfa-a6dc-4338-beb7-9159fd5a26f9" />

<img width="1000" height="1600" alt="image" src="https://github.com/user-attachments/assets/a41516eb-83a4-4fd2-93e9-caa870c22461" />

<img width="1000" height="1600" alt="image" src="https://github.com/user-attachments/assets/e80a177e-08cd-4d3e-a6c6-9be9701b2c5b" />

<img width="1000" height="1600" alt="image" src="https://github.com/user-attachments/assets/11e09fbc-0631-47fb-9d53-6d72d0e1dfff" />
