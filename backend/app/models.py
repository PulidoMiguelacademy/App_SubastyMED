from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from .database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True, nullable=False)
    email = Column(String(100), unique=True, index=True, nullable=False)
    full_name = Column(String(100), nullable=False)
    hashed_password = Column(String(255), nullable=False)
    avatar_url = Column(String(255), default="/uploads/img_perfil.png")
    rating = Column(Float, default=5.0)
    member_since = Column(String(20), default="2024")
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    # Relationships
    auctions = relationship("Auction", back_populates="creator", foreign_keys="Auction.creator_id")
    bids = relationship("Bid", back_populates="bidder")


class Auction(Base):
    __tablename__ = "auctions"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(150), nullable=False, index=True)
    description = Column(Text, nullable=False)
    category = Column(String(50), nullable=False, index=True)
    starting_price = Column(Float, nullable=False)
    current_bid = Column(Float, nullable=False)
    bid_count = Column(Integer, default=0)
    time_remaining = Column(String(50), default="24h 00m")
    image_url = Column(String(255), nullable=True)
    status = Column(String(20), default="Activa")  # Activa, Finalizada, Cancelada
    
    creator_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    winner_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    # Relationships
    creator = relationship("User", back_populates="auctions", foreign_keys=[creator_id])
    winner = relationship("User", foreign_keys=[winner_id])
    bids = relationship("Bid", back_populates="auction", cascade="all, delete-orphan", order_by="desc(Bid.amount)")


class Bid(Base):
    __tablename__ = "bids"

    id = Column(Integer, primary_key=True, index=True)
    auction_id = Column(Integer, ForeignKey("auctions.id"), nullable=False)
    bidder_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    amount = Column(Float, nullable=False)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    # Relationships
    auction = relationship("Auction", back_populates="bids")
    bidder = relationship("User", back_populates="bids")
