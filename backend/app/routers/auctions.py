from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import or_
from ..database import get_db
from ..models import Auction, User
from ..schemas import AuctionCreate, AuctionUpdate, AuctionResponse, AuctionDetailResponse
from ..security import get_current_user

router = APIRouter(prefix="/api/auctions", tags=["Subastas"])


@router.get("", response_model=List[AuctionResponse])
def get_auctions(
    category: Optional[str] = Query(None, description="Filtrar por categoría (ej. 'Electrónica', 'Vehículos', 'Todos')"),
    search: Optional[str] = Query(None, description="Búsqueda por título o descripción"),
    db: Session = Depends(get_db)
):
    query = db.query(Auction).filter(Auction.status == "Activa")

    if category and category != "Todos":
        query = query.filter(Auction.category == category)

    if search:
        search_fmt = f"%{search}%"
        query = query.filter(
            or_(
                Auction.title.ilike(search_fmt),
                Auction.description.ilike(search_fmt),
            )
        )

    return query.order_by(Auction.created_at.desc()).all()


@router.get("/my-auctions", response_model=List[AuctionResponse])
def get_my_auctions(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Devuelve todas las subastas creadas por el usuario autenticado"""
    return db.query(Auction).filter(
        Auction.creator_id == current_user.id
    ).order_by(Auction.created_at.desc()).all()


@router.get("/{auction_id}", response_model=AuctionDetailResponse)
def get_auction(auction_id: int, db: Session = Depends(get_db)):
    auction = db.query(Auction).filter(Auction.id == auction_id).first()
    if not auction:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subasta no encontrada")
    return auction


@router.post("", response_model=AuctionResponse, status_code=status.HTTP_201_CREATED)
def create_auction(
    auction_in: AuctionCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    new_auction = Auction(
        title=auction_in.title,
        description=auction_in.description,
        category=auction_in.category,
        starting_price=auction_in.starting_price,
        current_bid=auction_in.starting_price,
        bid_count=0,
        time_remaining=auction_in.time_remaining or "24h 00m",
        image_url=auction_in.image_url or "/uploads/img_macbook.png",
        status="Activa",
        creator_id=current_user.id,
    )
    db.add(new_auction)
    db.commit()
    db.refresh(new_auction)
    return new_auction


@router.put("/{auction_id}", response_model=AuctionResponse)
def update_auction(
    auction_id: int,
    auction_in: AuctionUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Modifica una subasta (título, descripción, oferta/precio, categoría, etc.)"""
    auction = db.query(Auction).filter(Auction.id == auction_id).first()
    if not auction:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subasta no encontrada")

    if auction.creator_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No tienes permiso para modificar esta subasta"
        )

    if auction_in.title is not None:
        auction.title = auction_in.title
    if auction_in.description is not None:
        auction.description = auction_in.description
    if auction_in.category is not None:
        auction.category = auction_in.category
    if auction_in.starting_price is not None:
        auction.starting_price = auction_in.starting_price
    if auction_in.current_bid is not None:
        auction.current_bid = auction_in.current_bid
    if auction_in.time_remaining is not None:
        auction.time_remaining = auction_in.time_remaining
    if auction_in.image_url is not None:
        auction.image_url = auction_in.image_url
    if auction_in.status is not None:
        auction.status = auction_in.status

    db.commit()
    db.refresh(auction)
    return auction


@router.delete("/{auction_id}", status_code=status.HTTP_200_OK)
def delete_auction(
    auction_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Elimina o cancela una subasta del usuario"""
    auction = db.query(Auction).filter(Auction.id == auction_id).first()
    if not auction:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subasta no encontrada")

    if auction.creator_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No tienes permiso para eliminar esta subasta"
        )

    db.delete(auction)
    db.commit()
    return {"message": "Subasta eliminada correctamente"}
