from nicegui import ui, app
from config import Config
import asyncio

ui.add_head_html('''
<script src="https://cdn.tailwindcss.com"></script>
<style>
    .dark { background: #0f172a; color: #e2e8f0; }
</style>
''')

async def init_tailwind():
    await ui.run_javascript('''
        document.documentElement.setAttribute('data-theme', 'dark');
        return tailwind.config = { content: ["./**/*.html"], theme: { extend: {} } }
    ''')

@ui.page('/')
def home():
    ui.label('Bitna l1j-kr 2.0 Web Control Panel').classes('text-4xl font-bold text-center mt-8')
    ui.label('Modern Administration Interface').classes('text-xl text-center text-gray-400')
    
    with ui.grid(columns=3).classes('w-full max-w-6xl mx-auto gap-6 p-8'):
        ui.card().classes('p-6').tight().on('click', lambda: ui.notify('Dashboard')).add(
            ui.label('Server Status').classes('text-2xl'),
            ui.label('Online: 0 / 500').classes('text-green-400')
        )
        ui.card().classes('p-6').tight().on('click', lambda: ui.notify('Players')).add(
            ui.label('Players').classes('text-2xl')
        )
        ui.card().classes('p-6').tight().on('click', lambda: ui.notify('Console')).add(
            ui.label('Console').classes('text-2xl')
        )

    ui.run_javascript('init_tailwind()')

if __name__ in {"__main__", "__mp_main__"}:
    ui.run(title='Bitna Web GUI', port=Config.SERVER_PORT, reload=Config.DEBUG, dark=True)
