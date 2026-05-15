from nicegui import ui
import pymysql
import os
from dotenv import load_dotenv

load_dotenv()

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', 'goldbitna_l200'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'cursorclass': pymysql.cursors.DictCursor
}

def get_db():
    try:
        return pymysql.connect(**DB_CONFIG)
    except Exception as e:
        ui.notify(f'DB 연결 실패: {e}', type='negative')
        return None

ui.label('bitna l1j-kr 2.0 Web GUI').classes('text-4xl font-bold text-center mt-8')
ui.label('SWT GUI 완전 대체').classes('text-center text-gray-500')

with ui.tabs().classes('w-full justify-center'):
    t1 = ui.tab('Dashboard')
    t2 = ui.tab('Console')
    t3 = ui.tab('Players')
    t4 = ui.tab('NPC Spawn')
    t5 = ui.tab('Action Flow')

with ui.tab_panels():
    with ui.tab_panel(t1):
        ui.label('Dashboard - 서버 상태').classes('text-3xl')
    with ui.tab_panel(t2):
        ui.label('Live Console').classes('text-3xl')
        ui.log(max_lines=50).classes('h-96 w-full')
    with ui.tab_panel(t3):
        ui.label('Players').classes('text-3xl')
        ui.button('플레이어 불러오기')
    with ui.tab_panel(t4):
        ui.label('NPC Spawn').classes('text-3xl')
    with ui.tab_panel(t5):
        ui.label('Action Flow').classes('text-3xl')

ui.run(port=8080, dark=True, title='bitna Web GUI')