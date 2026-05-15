import os
from dotenv import load_dotenv
from nicegui import ui, app
from fastapi import FastAPI

load_dotenv()

# TODO: DB connection and full implementation

ui.label('Bitna Web GUI - Loading...').classes('text-4xl')
ui.run(title='bitna Web GUI', port=8080, reload=True)