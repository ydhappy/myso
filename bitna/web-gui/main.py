from nicegui import ui, app
from dotenv import load_dotenv
import os
import pymysql
from pymysql.cursors import DictCursor
import asyncio

load_dotenv()

# DB 설정
DB_CONFIG = {
    'host': os.getenv('DB_HOST', '127.0.0.1'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'db': os.getenv('DB_NAME', 'goldbitna_l200'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'cursorclass': DictCursor
}

def get_db_connection():
    return pymysql.connect(**DB_CONFIG)

# 메인 페이지
@ui.page('/')
def index():
    ui.html('<h1 style="text-align:center; color:#00ff88;">bitna l1j-kr 2.0 - Web GUI</h1>')
    ui.label('SWT GUI 완전 대체 버전').classes('text-center text-lg text-gray-400')

    with ui.tabs().classes('w-full'):
        dashboard = ui.tab('Dashboard')
        players = ui.tab('Players')
        console = ui.tab('Live Console')
        npc_spawn = ui.tab('NPC Spawn')
        action_flow = ui.tab('Action Flow')
        chat = ui.tab('Chat')

    with ui.tab_panels().classes('w-full min-h-screen'):
        with ui.tab_panel(dashboard):
            ui.label('Dashboard').classes('text-3xl')
            ui.button('서버 상태 새로고침', on_click=lambda: ui.notify('서버 상태 업데이트 (실제 구현 필요)'))

        with ui.tab_panel(players):
            ui.label('플레이어 관리').classes('text-3xl')
            # characters 테이블 조회 예시
            ui.button('온라인 플레이어 조회', on_click=lambda: ui.notify('DB에서 characters 조회'))

        with ui.tab_panel(console):
            ui.label('Live Console').classes('text-3xl')
            log_area = ui.log().classes('w-full h-96')
            ui.input('명령어 입력', placeholder='GM 명령어').on('keydown.enter', lambda e: log_area.push(f'> {e.sender.value}'))

        with ui.tab_panel(npc_spawn):
            ui.label('NPC / Monster Spawn').classes('text-3xl')
            ui.input('NPC ID')
            ui.input('X 좌표')
            ui.input('Y 좌표')
            ui.button('Spawn', color='green')

        with ui.tab_panel(action_flow):
            ui.label('Action Flow').classes('text-3xl')
            ui.button('Teleport → Item Give → Announce', color='orange', on_click=lambda: ui.notify('Action Flow 실행'))

        with ui.tab_panel(chat):
            ui.label('채팅 모니터').classes('text-3xl')
            ui.label('실시간 채팅 (추후 WebSocket 연동)').classes('text-gray-500')

ui.run(title='bitna Web GUI', port=8080, dark=True, reload=True)
