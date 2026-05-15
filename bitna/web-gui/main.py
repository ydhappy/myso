from nicegui import ui
import pymysql
import os
from dotenv import load_dotenv

load_dotenv()

def get_db():
    try:
        conn = pymysql.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            password=os.getenv('DB_PASSWORD', ''),
            database=os.getenv('DB_NAME', 'goldbitna_l200'),
            port=int(os.getenv('DB_PORT', 3306)),
            cursorclass=pymysql.cursors.DictCursor
        )
        return conn
    except Exception as e:
        print(f'DB Error: {e}')
        return None

ui.label('bitna l1j-kr 2.0 Web GUI').classes('text-4xl font-bold text-center mt-10')
ui.label('SWT GUI 완전 대체').classes('text-center text-xl text-gray-500')

with ui.tabs().classes('w-full'):
    ui.tab('Dashboard')
    ui.tab('Players')
    ui.tab('Console')
    ui.tab('NPC Spawn')

ui.run(title='bitna Web GUI', port=8080, dark=True)