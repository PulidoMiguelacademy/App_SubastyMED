# 🛠️ Documentación Técnica del Backend - SubastyMED

## 📌 1. Visión General
El backend de **SubastyMED** proporciona una API RESTful moderna, rápida y segura diseñada para gestionar subastas en tiempo real, pujas, usuarios y archivos multimedia.

La arquitectura combina un servidor local de alto rendimiento basado en **FastAPI (Python)** con la nube en tiempo real de **Google Firebase (Firestore & Firebase Auth)**, permitiendo operar tanto con persistencia local/híbrida como con sincronización en la nube multi-dispositivo.

---

## 🏗️ 2. Arquitectura y Stack Tecnológico

| Componente | Tecnología | Descripción |
| :--- | :--- | :--- |
| **Lenguaje** | Python 3.10+ | Lenguaje base del servidor. |
| **Framework Web** | FastAPI | Framework asíncrono ASGI de alto rendimiento. |
| **Servidor ASGI** | Uvicorn | Servidor web asíncrono para producción y desarrollo. |
| **ORM / Base de Datos** | SQLAlchemy / SQLite | Mapeo objeto-relacional y base de datos relacional local (`subastymed.db`). |
| **Validación de Datos** | Pydantic v2 | Esquemas tipados y validación estricta de payloads JSON. |
| **Seguridad y Criptografía** | Passlib (Bcrypt) + PyJWT | Hasheo de contraseñas y emisión de tokens JWT Bearer. |
| **Capa Cloud / Tiempo Real** | Firebase Firestore & Auth | Sincronización en la nube multi-usuario y reglas de acceso. |
| **Servicio de Archivos** | FastAPI StaticFiles | Servidor de archivos estáticos para fotos en `/uploads`. |

---

## 📂 3. Estructura del Proyecto

```text
backend/
├── app/
│   ├── __init__.py
│   ├── main.py              # Punto de entrada de la aplicación FastAPI y ciclo de vida (Lifespan).
│   ├── database.py          # Configuración de SQLAlchemy Engine y sesión SessionLocal.
│   ├── models.py            # Modelos ORM relacionales (User, Auction, Bid).
│   ├── schemas.py           # Modelos de datos y validación con Pydantic.
│   ├── security.py          # Hasheo de claves con Bcrypt y generación de tokens JWT.
│   ├── seed.py              # Poblado automático de datos iniciales de prueba (Mock Data).
│   └── routers/
│       ├── __init__.py
│       ├── auth.py          # Endpoints de Registro, Login y Perfil de Usuario.
│       ├── auctions.py      # Endpoints CRUD de Subastas públicas y personales.
│       ├── bids.py          # Endpoints de Pujas, historial y estadísticas.
│       └── uploads.py       # Endpoint para subir imágenes de artículos y avatares.
├── uploads/                 # Directorio de almacenamiento de imágenes locales.
├── subastymed.db            # Base de datos SQLite generada automáticamente.
└── iniciar_backend.bat      # Script para arrancar el servidor en Windows con un solo clic.
```

---

## 💾 4. Modelo de Datos Relacional (Entidad - Relación)

### 👤 Tabla: `users`
Almacena las cuentas de usuario y credenciales.
* `id` *(Integer, PK)*: Identificador único.
* `username` *(String, Unique)*: Nombre de usuario único (ej. `@carlos_m`).
* `email` *(String, Unique)*: Correo electrónico del usuario.
* `full_name` *(String)*: Nombre y apellido.
* `hashed_password` *(String)*: Contraseña cifrada mediante Bcrypt.
* `avatar_url` *(String)*: Ruta o URL a la imagen de perfil.
* `rating` *(Float)*: Calificación del usuario (por defecto 5.0).
* `member_since` *(String)*: Año de ingreso.
* `created_at` *(DateTime)*: Fecha y hora UTC de registro.

### 🏷️ Tabla: `auctions`
Almacena las subastas publicadas en la plataforma.
* `id` *(Integer, PK)*: Identificador único de la subasta.
* `title` *(String)*: Título del producto (ej. "MacBook Pro 16\"").
* `description` *(Text)*: Descripción detallada del estado y accesorios.
* `category` *(String)*: Categoría ("Electrónica", "Vehículos", "Arte", "Moda", etc.).
* `starting_price` *(Float)*: Precio base o inicial.
* `current_bid` *(Float)*: Oferta más alta registrada actualmente.
* `bid_count` *(Integer)*: Contador total de pujas.
* `time_remaining` *(String)*: Tiempo restante o fecha límite ("02h 15m", "24/10/2026").
* `image_url` *(String, Nullable)*: Ruta a la foto en `/uploads` o URL pública.
* `status` *(String)*: Estado ("Activa", "Finalizada", "Cancelada").
* `creator_id` *(Integer, FK -> users.id)*: Usuario que publicó la subasta.
* `winner_id` *(Integer, FK -> users.id, Nullable)*: Ganador de la subasta.
* `created_at` *(DateTime)*: Fecha de creación en UTC.

### 💰 Tabla: `bids`
Registro histórico de cada puja realizada.
* `id` *(Integer, PK)*: Identificador único de la puja.
* `auction_id` *(Integer, FK -> auctions.id)*: Subasta donde se realiza la puja.
* `bidder_id` *(Integer, FK -> users.id)*: Usuario que realiza la puja.
* `amount` *(Float)*: Monto ofertado.
* `created_at` *(DateTime)*: Momento exacto de la puja.

---

## 🌐 5. Endpoints de la API REST

### 🔐 Módulo de Autenticación (`/auth`)
* `POST /auth/register`
  * **Descripción:** Registra un nuevo usuario en el sistema.
  * **Payload:** `{ "username": "...", "email": "...", "full_name": "...", "password": "..." }`
  * **Respuesta:** Objeto `UserResponse`.
* `POST /auth/login`
  * **Descripción:** Valida credenciales y genera un Token JWT Bearer.
  * **Payload:** `{ "username_or_email": "...", "password": "..." }`
  * **Respuesta:** `{ "access_token": "...", "token_type": "bearer", "user": { ... } }`
* `GET /auth/me`
  * **Descripción:** Retorna los datos del usuario autenticado (requiere `Authorization: Bearer <token>`).
* `GET /auth/stats`
  * **Descripción:** Retorna métricas del usuario: subastas ganadas, pujas activas y favoritos.

### 📢 Módulo de Subastas (`/auctions`)
* `GET /auctions`
  * **Descripción:** Lista todas las subastas activas. Permite filtrar por `?category=...` y buscar por `?search=...`.
* `GET /auctions/{id}`
  * **Descripción:** Obtiene los detalles completos de una subasta específica y su historial de pujas.
* `POST /auctions`
  * **Descripción:** Crea una nueva subasta asociada al usuario autenticado.
* `PUT /auctions/{id}`
  * **Descripción:** Actualiza el título, descripción, precio o categoría de una subasta.
* `DELETE /auctions/{id}`
  * **Descripción:** Elimina una subasta por ID (propietario o administrador).
* `GET /auctions/my/created`
  * **Descripción:** Lista todas las subastas creadas por el usuario en sesión.

### 💵 Módulo de Pujas (`/bids`)
* `POST /auctions/{id}/bids`
  * **Descripción:** Realiza una nueva puja sobre una subasta activa.
  * **Validación:** El monto ofertado debe ser estrictamente mayor que `current_bid`. Incrementa `bid_count` automáticamente.
* `GET /bids/my`
  * **Descripción:** Retorna las pujas del usuario agrupadas por estado: `"Activas"`, `"Ganadas"` o `"Perdidas"`.

### 🖼️ Módulo de Carga de Archivos (`/uploads`)
* `POST /upload`
  * **Descripción:** Recibe un archivo binario `multipart/form-data`, genera un hash criptográfico único para evitar colisiones y lo guarda en el disco local `/uploads`.
  * **Respuesta:** `{ "url": "/uploads/nombre_archivo.jpg" }`

---

## 🚀 6. Cómo Ejecutar el Backend

### En Windows (Método Rápido):
Haz doble clic en el archivo ubicado en la raíz del proyecto:
```cmd
iniciar_backend.bat
```

### Manualmente con Terminal:
```bash
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install fastapi uvicorn sqlalchemy pydantic passlib[bcrypt] pyjwt python-multipart email-validator
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

* **Documentación Interactiva Swagger UI:** [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
* **Documentación ReDoc:** [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)
