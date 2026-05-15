# Bitna l1j-kr 2.0 Web GUI

Modern Web Administration Panel for l1j-kr 2.0 Private Server

## Tech Stack
- NiceGUI (Python + FastAPI + Vue-like)
- Tailwind CSS (modern dark design)
- MySQL (direct connection to goldbitna_l200)
- WebSocket for real-time (console, chat, online players)

## How to run
1. cd web-gui
2. python -m venv venv
3. source venv/bin/activate (or venv\Scripts\activate on Windows)
4. pip install -r requirements.txt
5. cp .env.example .env
6. Edit .env with your DB settings
7. python main.py

Access: http://localhost:8080