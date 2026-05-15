from nicegui import ui
import os
from dotenv import load_dotenv
import pymysql

load_dotenv()

# DB Config
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'db': os.getenv('DB_NAME', 'goldbitna_l200'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'cursorclass': pymysql.cursors.DictCursor
}

def get_db():
    return pymysql.connect(**DB_CONFIG)

ui.label('bitna l1j-kr 2.0 - Web GUI').classes('text-4xl font-bold text-center mt-8')
ui.label('SWT GUI 완전 대체판').classes('text-center text-xl text-gray-400 mb-10')

with ui.tabs().classes('w-full'):
    ui.tab('Dashboard')
    ui.tab('Live Console')
    ui.tab('Players')
    ui.tab('NPC Spawn')
    ui.tab('Action Flow')
    ui.tab('Enchant')

ui.run(title='bitna Web GUI', port=8080, dark=True)