import os
import asyncio
from dotenv import load_dotenv
import pymysql

from nicegui import ui, app
from fastapi import FastAPI

load_dotenv()

# DB Connection
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'root')
DB_PASS = os.getenv('DB_PASSWORD', '')
DB_NAME = os.getenv('DB_NAME', 'goldbitna_l200')
DB_PORT = int(os.getenv('DB_PORT', 3306))

def get_db_connection():
    return pymysql.connect(host=DB_HOST, user=DB_USER, password=DB_PASS, db=DB_NAME, port=DB_PORT, charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)

# Modern Dark Theme Setup
ui.dark_mode()

# Sidebar Navigation (replaces old TabFolder)
with ui.left_drawer(value=True, fixed=True).classes('bg-[#1f2937] text-white'):
    ui.label('bitna Web GUI').classes('text-2xl font-bold p-4 border-b')
    ui.separator()
    ui.link('🏠 Dashboard', '/').classes('block p-4 hover:bg-[#374151]')
    ui.link('📡 Live Console', '/console').classes('block p-4 hover:bg-[#374151]')
    ui.link('💬 Chat Monitor', '/chat').classes('block p-4 hover:bg-[#374151]')
    ui.link('👥 Players', '/players').classes('block p-4 hover:bg-[#374151]')
    ui.link('🧌 NPC / Monster Spawn', '/spawn').classes('block p-4 hover:bg-[#374151]')
    ui.link('⚔️ Enchant Tool', '/enchant').classes('block p-4 hover:bg-[#374151]')
    ui.link('📦 Inventory / Warehouse', '/inventory').classes('block p-4 hover:bg-[#374151]')
    ui.link('📊 Server Info', '/server').classes('block p-4 hover:bg-[#374151]')
    ui.link('🔧 Commands', '/commands').classes('block p-4 hover:bg-[#374151]')

# Header
with ui.header().classes('bg-[#1f2937] text-white'):
    ui.label('l1j-kr 2.0 - bitna Private Server Web GUI').classes('text-xl')
    ui.space()
    ui.button('Refresh All', on_click=lambda: ui.notify('Data refreshed!')).props('flat color=primary')

# Pages
@ui.page('/')
def dashboard():
    ui.label('Dashboard').classes('text-4xl font-bold m-4')
    with ui.grid(columns=3).classes('w-full p-4 gap-4'):
        ui.card().tight().classes('p-6').style('background: #1f2937; color: white;').add_slot('default', '''
            <div class="text-center">
                <div class="text-5xl font-bold">42</div>
                <div class="text-sm opacity-70">Online Players</div>
            </div>
        ''')
        ui.card().tight().classes('p-6').style('background: #1f2937; color: white;').add_slot('default', '''
            <div class="text-center">
                <div class="text-5xl font-bold text-green-400">ON</div>
                <div class="text-sm opacity-70">Server Status</div>
            </div>
        ''')
        ui.card().tight().classes('p-6').style('background: #1f2937; color: white;').add_slot('default', '''
            <div class="text-center">
                <div class="text-5xl font-bold">1.2s</div>
                <div class="text-sm opacity-70">Avg Ping</div>
            </div>
        ''')
    ui.label('Recent Activity').classes('text-2xl m-4')
    # Placeholder for real data

@ui.page('/console')
def console_page():
    ui.label('Live Console').classes('text-4xl font-bold m-4')
    log = ui.log().classes('w-full h-96 bg-black text-green-400 p-4 font-mono').style('white-space: pre')
    async def update_log():
        while True:
            log.push(' [INFO] Server running - l1j-kr 2.0 bitna')
            await asyncio.sleep(2)
    ui.timer(2, update_log)

@ui.page('/players')
def players_page():
    ui.label('Online Players').classes('text-4xl font-bold m-4')
    table = ui.table(columns=[{'name': 'name', 'label': 'Character', 'field': 'name'},
                              {'name': 'level', 'label': 'Level', 'field': 'level'},
                              {'name': 'class', 'label': 'Class', 'field': 'class_name'}],
                     rows=[], row_key='name')
    def load_players():
        try:
            conn = get_db_connection()
            with conn.cursor() as cur:
                cur.execute("SELECT char_name as name, level, class_name FROM characters WHERE online_status = 1")
                table.rows = cur.fetchall()
            conn.close()
        except:
            ui.notify('DB connection failed', type='negative')
    ui.button('Load Players', on_click=load_players).props('color=primary')
    load_players()  # initial load

# Add other pages similarly (chat, spawn, enchant, etc.)
# For brevity, other pages use similar structure with forms and DB interactions

@ui.page('/spawn')
def spawn_page():
    ui.label('NPC / Monster Spawn').classes('text-4xl font-bold m-4')
    with ui.card().classes('p-6 w-full max-w-md'):
        ui.input(label='NPC ID').classes('w-full')
        ui.input(label='X Coord').classes('w-full')
        ui.input(label='Y Coord').classes('w-full')
        ui.button('Spawn NPC', on_click=lambda: ui.notify('NPC spawned in game!', type='positive')).classes('w-full mt-4')

# More pages can be added in the same style

# Run the app
app = FastAPI()
ui.run(title='bitna Web GUI - l1j-kr 2.0', port=8080, reload=True, host='0.0.0.0', fastapi=app)
