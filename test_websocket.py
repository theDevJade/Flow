#!/usr/bin/env python3
"""
Simple WebSocket test client to verify the Flow WebSocket server is working
"""

import asyncio
import websockets
import json
from datetime import datetime

async def test_websocket():
    uri = "ws://localhost:9090/ws"
    
    try:
        print(f"🔌 Connecting to {uri}...")
        async with websockets.connect(uri) as websocket:
            print("✅ Connected successfully!")
            
            # Send authentication message
            auth_message = {
                "type": "auth",
                "id": "test-auth-1",
                "data": {"token": "test-token"},
                "timestamp": datetime.now().isoformat()
            }
            
            print(f"📤 Sending auth message: {auth_message}")
            await websocket.send(json.dumps(auth_message))
            
            # Listen for response
            print("👂 Listening for server response...")
            response = await websocket.recv()
            print(f"📥 Received: {response}")
            
            # Send heartbeat
            heartbeat_message = {
                "type": "heartbeat",
                "id": "test-heartbeat-1", 
                "data": {},
                "timestamp": datetime.now().isoformat()
            }
            
            print(f"📤 Sending heartbeat: {heartbeat_message}")
            await websocket.send(json.dumps(heartbeat_message))
            
            # Listen for heartbeat response
            response = await websocket.recv()
            print(f"📥 Received: {response}")
            
            print("✅ WebSocket test completed successfully!")
            
    except Exception as e:
        print(f"❌ WebSocket test failed: {e}")

if __name__ == "__main__":
    print("🧪 Flow WebSocket Server Test")
    print("=" * 40)
    asyncio.run(test_websocket())