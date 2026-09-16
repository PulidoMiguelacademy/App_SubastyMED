import os
import uuid
import shutil
from fastapi import APIRouter, UploadFile, File, HTTPException

router = APIRouter(prefix="/api/upload", tags=["Subida de Archivos"])

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)


@router.post("")
async def upload_image(file: UploadFile = File(...)):
    # Extension deduction
    filename = file.filename or "image.jpg"
    ext = os.path.splitext(filename)[1].lower()
    if not ext:
        if file.content_type == "image/png":
            ext = ".png"
        elif file.content_type == "image/webp":
            ext = ".webp"
        else:
            ext = ".jpg"

    allowed_extensions = {".jpg", ".jpeg", ".png", ".webp", ".gif"}
    if ext not in allowed_extensions:
        raise HTTPException(status_code=400, detail="Formato de imagen no soportado (solo jpg, jpeg, png, webp)")

    # Unique file name
    clean_name = os.path.basename(filename).replace(" ", "_")
    unique_filename = f"{uuid.uuid4().hex[:10]}_{clean_name}"
    if not unique_filename.endswith(ext):
        unique_filename += ext

    file_path = os.path.join(UPLOAD_DIR, unique_filename)

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    return {"url": f"/uploads/{unique_filename}", "filename": unique_filename}
