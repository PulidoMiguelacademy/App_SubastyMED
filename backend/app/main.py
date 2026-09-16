import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from .database import engine, Base
from .seed import seed_database
from .routers import auth, auctions, bids, uploads

# Ensure database tables exist and seed mock data on startup
@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    seed_database()
    yield


app = FastAPI(
    title="SubastyMED API",
    description="Backend oficial de la plataforma de subastas SubastyMED.",
    version="1.0.0",
    lifespan=lifespan,
)

# Enable CORS for mobile app access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Static files for uploaded images
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")

# Include Routers
app.include_router(auth.router)
app.include_router(auctions.router)
app.include_router(bids.router)
app.include_router(uploads.router)


@app.get("/", tags=["General"])
def read_root():
    return {
        "message": "Bienvenido a la API de SubastyMED",
        "docs": "/docs",
        "version": "1.0.0",
        "status": "online"
    }
