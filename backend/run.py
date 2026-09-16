import uvicorn

if __name__ == "__main__":
    # Host 0.0.0.0 allows both localhost and Android Emulator (10.0.2.2) or physical phone (WiFi IP) to connect
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
