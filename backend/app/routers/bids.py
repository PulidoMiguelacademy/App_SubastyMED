from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import func
from ..database import get_db
from ..models import Auction, Bid, User
from ..schemas import BidCreate, BidResponse, UserBidDisplay
from ..security import get_current_user

router = APIRouter(prefix="/api", tags=["Pujas y Ofertas"])


@router.post("/auctions/{auction_id}/bid", response_model=BidResponse, status_code=status.HTTP_201_CREATED)
def place_bid(
    auction_id: int,
    bid_in: BidCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    auction = db.query(Auction).filter(Auction.id == auction_id).first()
    if not auction:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subasta no encontrada")

    if auction.status != "Activa":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Esta subasta no está activa")

    if auction.creator_id == current_user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No puedes pujar en tu propia subasta"
        )

    if bid_in.amount <= auction.current_bid:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Tu puja debe ser mayor a la oferta actual (${auction.current_bid:,.2f})"
        )

    # Create new bid
    new_bid = Bid(
        auction_id=auction.id,
        bidder_id=current_user.id,
        amount=bid_in.amount,
    )
    db.add(new_bid)

    # Update auction state
    auction.current_bid = bid_in.amount
    auction.bid_count += 1
    auction.winner_id = current_user.id

    db.commit()
    db.refresh(new_bid)
    return new_bid


@router.get("/bids/my-bids", response_model=List[UserBidDisplay])
def get_my_bids(
    filter_group: Optional[str] = Query(None, description="'Activas', 'Ganadas', o 'Perdidas'"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Find all auctions where the user has placed at least one bid
    user_auction_ids = db.query(Bid.auction_id).filter(Bid.bidder_id == current_user.id).distinct().all()
    auction_ids = [row[0] for row in user_auction_ids]

    if not auction_ids:
        return []

    auctions = db.query(Auction).filter(Auction.id.in_(auction_ids)).all()
    result = []

    for auction in auctions:
        # Find user's highest bid on this auction
        highest_user_bid = db.query(func.max(Bid.amount)).filter(
            Bid.auction_id == auction.id,
            Bid.bidder_id == current_user.id
        ).scalar() or 0.0

        if auction.status == "Activa":
            group = "Activas"
            # If user's highest bid equals auction's current bid, they are winning!
            if highest_user_bid >= auction.current_bid:
                bid_status = "Ganando"
            else:
                bid_status = "Superado"
        elif auction.status == "Finalizada":
            if auction.winner_id == current_user.id:
                group = "Ganadas"
                bid_status = "Ganada"
            else:
                group = "Perdidas"
                bid_status = "Perdida"
        else:
            group = "Perdidas"
            bid_status = "Cancelada"

        if filter_group and filter_group != group:
            continue

        result.append(
            UserBidDisplay(
                id=str(auction.id),
                auction_id=auction.id,
                title=auction.title,
                your_bid=f"${highest_user_bid:,.2f}",
                max_bid=f"${auction.current_bid:,.2f}",
                status=bid_status,
                filter_group=group,
                image_url=auction.image_url
            )
        )

    return result
