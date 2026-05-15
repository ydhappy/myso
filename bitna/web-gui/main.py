from nicegui import ui
from fastapi import FastAPI
import pymysql
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI()

# DB Connection

def get_db_connection():
    return pymysql.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        password=os.getenv('DB_PASSWORD', ''),
        db=os.getenv('DB_NAME', 'goldbitna_l200'),
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )

ui.page_title('bitna Server Web GUI - l1j-kr 2.0')

with ui.header().classes('justify-between items-center'):
    ui.label('bitna Web GUI').classes('text-2xl font-bold')
    ui.button('Refresh', on_click=lambda: ui.notify('Refreshed')).props('flat')

with ui.left_drawer(value=True).classes('bg-blue-grey-9 text-white'):
    ui.label('Menu').classes('text-xl m-4')
    ui.link('Dashboard', '/').classes('text-white block p-4 hover:bg-blue-grey-8')
    ui.link('Console', '/console').classes('text-white block p-4 hover:bg-blue-grey-8')
    ui.link('Players', '/players').classes('text-white block p-4 hover:bg-blue-grey-8')
    ui.link('NPC Spawn', '/npc').classes('text-white block p-4 hover:bg-blue-grey-8')
    ui.link('Action Flow', '/actionflow').classes('text-white block p-4 hover:bg-blue-grey-8')

@ui.page('/')
def dashboard():
    ui.label('Dashboard').classes('text-4xl')
    ui.label('bitna l1j-kr 2.0 Web Management Interface').classes('text-xl text-gray-500')
    with ui.grid(columns=3):
        ui.card().classes('p-6').tight().on('click', lambda: ui.notify('Online Players'))
        ui.label('온라인: 0').classes('text-3xl')
        # Add more cards

ui.run(title='bitna Web GUI', port=8080, reload=True)
