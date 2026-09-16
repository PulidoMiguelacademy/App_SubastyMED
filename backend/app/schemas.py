from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, EmailStr


# --- User Schemas ---
class UserBase(BaseModel):
    username: str
    email: EmailStr
    full_name: str


class UserCreate(UserBase):
    password: str


class UserLogin(BaseModel):
    username_or_email: str
    password: str


class UserResponse(UserBase):
    id: int
    avatar_url: Optional[str] = None
    rating: float = 5.0
    member_since: str = "2024"

    class Config:
        from_attributes = True


class UserStatsResponse(BaseModel):
    won_count: int = 0
    active_bids_count: int = 0
    favorites_count: int = 0


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


# --- Bid Schemas ---
class BidCreate(BaseModel):
    amount: float


class BidResponse(BaseModel):
    id: int
    auction_id: int
    bidder_id: int
    amount: float
    created_at: datetime

    class Config:
        from_attributes = True


class UserBidDisplay(BaseModel):
    id: str
    auction_id: int
    title: str
    your_bid: str
    max_bid: str
    status: str  # "Ganando", "Superado", "Ganada", "Perdida"
    filter_group: str  # "Activas", "Ganadas", "Perdidas"
    image_url: Optional[str] = None


# --- Auction Schemas ---
class AuctionBase(BaseModel):
    title: str
    description: str
    category: str
    starting_price: float
    time_remaining: Optional[str] = "24h 00m"
    image_url: Optional[str] = None


class AuctionCreate(AuctionBase):
    pass


class AuctionUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    category: Optional[str] = None
    starting_price: Optional[float] = None
    current_bid: Optional[float] = None
    time_remaining: Optional[str] = None
    image_url: Optional[str] = None
    status: Optional[str] = None


class AuctionResponse(AuctionBase):
    id: int
    current_bid: float
    bid_count: int
    status: str
    creator_id: int
    created_at: datetime

    class Config:
        from_attributes = True


class AuctionDetailResponse(AuctionResponse):
    creator: UserResponse
    bids: List[BidResponse] = []
