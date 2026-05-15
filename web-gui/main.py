from nicegui import ui, app
from dotenv import load_dotenv
import os
import pymysql
from pathlib import Path

load_dotenv()

# DB 설정
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', 'goldbitna_l200'),
    'port': int(os.getenv('DB_PORT', 3306))
}

@ui.page('/')
def index():
    ui.label('bitna Web GUI').classes('text-4xl font-bold text-center mt-8')
    ui.label('l1j-kr 2.0 서버 관리자').classes('text-xl text-center')
    with ui.row().classes('justify-center gap-4 mt-8'):
        ui.button('Dashboard', on_click=lambda: ui.notify('Dashboard')).classes('px-8 py-4')
        ui.button('Players', on_click=lambda: ui.notify('Players')).classes('px-8 py-4')
        ui.button('Console', on_click=lambda: ui.notify('Console')).classes('px-8 py-4')
        ui.button('Spawn', on_click=lambda: ui.notify('Spawn')).classes('px-8 py-4')

ui.run(title='bitna Web GUI', port=int(os.getenv('APP_PORT', 8080)), reload=True, dark=True)