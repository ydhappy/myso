from nicegui import ui, app
from fastapi import FastAPI
import asyncio

# Create FastAPI app
fastapi_app = FastAPI(title="bitna Web Admin")

# NiceGUI integration
@app.get('/')
async def home():
    return {"message": "bitna Web GUI is running"}

ui.add_head_html('''
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@3.4.1/dist/tailwind.min.css" rel="stylesheet">
''')

@ui.page('/')
def index():
    ui.html('''
        <div class="min-h-screen bg-gray-950 text-white">
            <div class="max-w-7xl mx-auto p-8">
                <h1 class="text-5xl font-bold text-center mb-8 text-cyan-400">bitna</h1>
                <h2 class="text-2xl text-center text-gray-400">l1j-kr 2.0 Modern Web Admin Panel</h2>
                
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mt-12">
                    <div class="bg-gray-900 p-8 rounded-3xl">
                        <h3 class="text-xl font-semibold mb-4">서버 상태</h3>
                        <div id="status">연결 중...</div>
                    </div>
                </div>
            </div>
        </div>
    ''')
    ui.run(title='bitna Web GUI', port=8080, reload=True)

if __name__ == "__main__":
    ui.run(title='bitna Admin', port=8080, reload=True, dark=True)
print("Web GUI starting...")