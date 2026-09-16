from sqlalchemy.orm import Session
from .database import engine, SessionLocal, Base
from .models import User, Auction, Bid
from .security import hash_password


def seed_database():
    Base.metadata.create_all(bind=engine)
    db: Session = SessionLocal()

    try:
        # Check if users already exist
        if db.query(User).count() > 0:
            print("Base de datos ya contiene datos. Omitiendo seed.")
            return

        print("Poblando base de datos inicial con datos de prueba...")

        # 1. Crear usuarios
        user_alejandro = User(
            username="alejandro",
            email="alejandro@subastymed.com",
            full_name="Dr. Alejandro Silva",
            hashed_password=hash_password("password123"),
            avatar_url="/uploads/img_perfil.png",
            rating=4.9,
            member_since="2023",
        )
        user_carlos = User(
            username="carlos",
            email="carlos@gmail.com",
            full_name="Carlos Méndez",
            hashed_password=hash_password("password123"),
            avatar_url="/uploads/img_perfil.png",
            rating=4.7,
            member_since="2024",
        )
        db.add_all([user_alejandro, user_carlos])
        db.commit()
        db.refresh(user_alejandro)
        db.refresh(user_carlos)

        # 2. Crear subastas iniciales (exactamente las de la app)
        auction_macbook = Auction(
            title="MacBook Pro 16\" M4",
            description="Excelente estado, 32GB RAM, 1TB SSD, cargador original y caja.",
            category="Electrónica",
            starting_price=1000.00,
            current_bid=1250.00,
            bid_count=14,
            time_remaining="02h 15m",
            image_url="/uploads/img_macbook.png",
            status="Activa",
            creator_id=user_carlos.id,
            winner_id=user_alejandro.id,  # Alejandro va ganando
        )

        auction_bmw = Auction(
            title="BMW Serie 3 2021",
            description="Impecable, 35,000 km, mantenimientos al día en concesionario oficial.",
            category="Vehículos",
            starting_price=20000.00,
            current_bid=24800.00,
            bid_count=45,
            time_remaining="1d 04h",
            image_url="/uploads/img_bmw.png",
            status="Activa",
            creator_id=user_alejandro.id,
            winner_id=user_carlos.id,  # Carlos va ganando (Alejandro superado)
        )

        auction_chaqueta = Auction(
            title="Chaqueta Cuero Retro",
            description="Vintage, Talla L, Unisex, 100% cuero genuino, acabado clásico.",
            category="Moda",
            starting_price=120.00,
            current_bid=180.00,
            bid_count=8,
            time_remaining="Finalizada",
            image_url="/uploads/img_chaqueta.png",
            status="Finalizada",
            creator_id=user_carlos.id,
            winner_id=user_alejandro.id,  # Alejandro ganó esta subasta
        )

        auction_oleo = Auction(
            title="Oleo Abstracto",
            description="Autor firmado, 120x80cm, técnica mixta sobre lienzo con detalles dorados.",
            category="Arte",
            starting_price=500.00,
            current_bid=620.00,
            bid_count=10,
            time_remaining="05h 22m",
            image_url="/uploads/img_oleo.png",
            status="Activa",
            creator_id=user_carlos.id,
            winner_id=user_carlos.id,
        )

        db.add_all([auction_macbook, auction_bmw, auction_chaqueta, auction_oleo])
        db.commit()
        db.refresh(auction_macbook)
        db.refresh(auction_bmw)
        db.refresh(auction_chaqueta)
        db.refresh(auction_oleo)

        # 3. Registrar pujas correspondientes
        # MacBook: Alejandro ofertó $1250 (máxima puja actual)
        bids = [
            Bid(auction_id=auction_macbook.id, bidder_id=user_carlos.id, amount=1100.00),
            Bid(auction_id=auction_macbook.id, bidder_id=user_alejandro.id, amount=1250.00),
            
            # BMW: Alejandro ofertó $24,200, pero Carlos pujó $24,800
            Bid(auction_id=auction_bmw.id, bidder_id=user_alejandro.id, amount=24200.00),
            Bid(auction_id=auction_bmw.id, bidder_id=user_carlos.id, amount=24800.00),

            # Chaqueta: Alejandro ofertó $180 y ganó
            Bid(auction_id=auction_chaqueta.id, bidder_id=user_carlos.id, amount=150.00),
            Bid(auction_id=auction_chaqueta.id, bidder_id=user_alejandro.id, amount=180.00),

            # Oleo:
            Bid(auction_id=auction_oleo.id, bidder_id=user_carlos.id, amount=620.00),
        ]
        db.add_all(bids)
        db.commit()

        print("Base de datos inicializada con éxito con usuarios, subastas y pujas de ejemplo.")
    finally:
        db.close()


if __name__ == "__main__":
    seed_database()
