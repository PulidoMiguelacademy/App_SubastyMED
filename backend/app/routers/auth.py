from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from ..database import get_db
from ..models import User, Auction, Bid
from ..schemas import UserCreate, UserLogin, UserResponse, TokenResponse, UserStatsResponse
from ..security import (
    hash_password,
    verify_password,
    create_access_token,
    get_current_user,
)

router = APIRouter(prefix="/api/auth", tags=["Autenticación y Usuarios"])


@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
def register(user_data: UserCreate, db: Session = Depends(get_db)):
    # Check if username or email already exists
    existing_user = db.query(User).filter(
        (User.username == user_data.username) | (User.email == user_data.email)
    ).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="El nombre de usuario o correo ya está registrado",
        )

    hashed_pw = hash_password(user_data.password)
    new_user = User(
        username=user_data.username,
        email=user_data.email,
        full_name=user_data.full_name,
        hashed_password=hashed_pw,
        avatar_url="/uploads/img_perfil.png",
        rating=5.0,
        member_since="2026",
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    token = create_access_token(data={"sub": str(new_user.id)})
    return TokenResponse(access_token=token, token_type="bearer", user=new_user)


@router.post("/login", response_model=TokenResponse)
def login(login_data: UserLogin, db: Session = Depends(get_db)):
    user = db.query(User).filter(
        (User.username == login_data.username_or_email) | (User.email == login_data.username_or_email)
    ).first()

    if not user or not verify_password(login_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credenciales incorrectas",
        )

    token = create_access_token(data={"sub": str(user.id)})
    return TokenResponse(access_token=token, token_type="bearer", user=user)


@router.get("/me", response_model=UserResponse)
def get_me(current_user: User = Depends(get_current_user)):
    return current_user


@router.get("/stats", response_model=UserStatsResponse)
def get_user_stats(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    # 1. Ganadas: auctions where status == 'Finalizada' and winner_id == current_user.id
    won_count = db.query(Auction).filter(
        Auction.status == "Finalizada",
        Auction.winner_id == current_user.id
    ).count()

    # 2. Activas: distinct active auctions where current_user has placed a bid
    active_bids_count = db.query(Bid.auction_id).join(Auction).filter(
        Bid.bidder_id == current_user.id,
        Auction.status == "Activa"
    ).distinct().count()

    # 3. Favoritos: mock count or created auctions count
    favorites_count = db.query(Auction).filter(Auction.creator_id == current_user.id).count()

    return UserStatsResponse(
        won_count=won_count,
        active_bids_count=active_bids_count,
        favorites_count=favorites_count
    )
