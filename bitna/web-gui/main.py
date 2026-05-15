from nicegui import ui
import pymysql
import os
from dotenv import load_dotenv

load_dotenv()

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

ui.label('bitna l1j-kr 2.0 Web GUI - SWT 완전 대체').classes('text-4xl font-bold text-center mt-8')

with ui.tabs().classes('w-full'):
    ui.tab('Dashboard')
    ui.tab('Live Console')
    ui.tab('Players')
    ui.tab('NPC Spawn')
    ui.tab('Action Flow')

ui.run(port=8080, dark=True, title='bitna Web GUI')