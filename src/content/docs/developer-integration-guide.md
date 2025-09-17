---
title: Developer Integration Guide
description: Step-by-step guide for integrating with the Flow backend system
---

# Developer Integration Guide

This guide provides practical examples and best practices for integrating with the Flow backend WebSocket system and HTTP API.

## Quick Start

### Prerequisites
- WebSocket-capable client (JavaScript, Python, Java, etc.)
- Valid authentication credentials
- Understanding of JSON message formats

### Basic Connection

#### JavaScript/TypeScript
```javascript
class FlowWebSocketClient {
    constructor(serverUrl, authToken) {
        this.serverUrl = serverUrl;
        this.authToken = authToken;
        this.ws = null;
        this.messageHandlers = new Map();
        this.correlationIds = new Map();
    }

    connect() {
        return new Promise((resolve, reject) => {
            const wsUrl = `${this.serverUrl}/ws?token=${this.authToken}`;
            this.ws = new WebSocket(wsUrl);

            this.ws.onopen = () => {
                console.log('Connected to Flow WebSocket server');
                resolve();
            };

            this.ws.onmessage = (event) => {
                this.handleMessage(JSON.parse(event.data));
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket error:', error);
                reject(error);
            };

            this.ws.onclose = () => {
                console.log('WebSocket connection closed');
            };
        });
    }

    handleMessage(message) {
        const handler = this.messageHandlers.get(message.type);
        if (handler) {
            handler(message);
        }
    }

    sendMessage(type, data, id = null) {
        const message = {
            type: type,
            id: id || this.generateId(),
            data: data,
            timestamp: new Date().toISOString()
        };
        this.ws.send(JSON.stringify(message));
        return message.id;
    }

    onMessageType(type, handler) {
        this.messageHandlers.set(type, handler);
    }

    generateId() {
        return Math.random().toString(36).substr(2, 9);
    }
}
```

#### Python
```python
import asyncio
import websockets
import json
import uuid
from typing import Dict, Callable, Optional

class FlowWebSocketClient:
    def __init__(self, server_url: str, auth_token: str):
        self.server_url = server_url
        self.auth_token = auth_token
        self.websocket = None
        self.message_handlers = {}
        
    async def connect(self):
        ws_url = f"{self.server_url}/ws?token={self.auth_token}"
        self.websocket = await websockets.connect(ws_url)
        print("Connected to Flow WebSocket server")
        
    async def send_message(self, message_type: str, data: Dict, message_id: str = None):
        message = {
            "type": message_type,
            "id": message_id or str(uuid.uuid4()),
            "data": data,
            "timestamp": asyncio.get_event_loop().time()
        }
        await self.websocket.send(json.dumps(message))
        return message["id"]
        
    async def handle_messages(self):
        async for message in self.websocket:
            data = json.loads(message)
            handler = self.message_handlers.get(data["type"])
            if handler:
                await handler(data)
                
    def on_message_type(self, message_type: str, handler: Callable):
        self.message_handlers[message_type] = handler
```

## Authentication

### HTTP Authentication
```javascript
async function authenticate(username, password) {
    const response = await fetch('/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    });
    
    const result = await response.json();
    if (result.success) {
        return result.token;
    } else {
        throw new Error(result.message);
    }
}

// Usage
const token = await authenticate('admin', 'admin123');
const client = new FlowWebSocketClient('ws://localhost:9090', token);
```

### Token Validation
```javascript
async function validateToken(token) {
    const response = await fetch('/auth/validate', {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
    
    const result = await response.json();
    return result.valid;
}
```

## Graph Operations

### Loading and Saving Graphs
```javascript
class GraphManager {
    constructor(wsClient) {
        this.client = wsClient;
        this.setupHandlers();
    }

    setupHandlers() {
        this.client.onMessageType('graph_load_response', (message) => {
            if (message.data.success) {
                this.onGraphLoaded(message.data.graphData);
            }
        });

        this.client.onMessageType('graph_saved', (message) => {
            console.log(`Graph ${message.data.graphId} saved successfully`);
        });
    }

    loadGraph(graphId) {
        return this.client.sendMessage('graph_load', { graphId });
    }

    saveGraph(graphId, graphData) {
        return this.client.sendMessage('graph_save', {
            graphId,
            graphData
        });
    }

    updateGraph(graphId, updateType, graphData) {
        return this.client.sendMessage('graph_update', {
            graphId,
            updateType,
            graphData
        });
    }

    onGraphLoaded(graphData) {
        // Handle loaded graph data
        console.log('Graph loaded:', graphData);
    }
}
```

### Real-time Graph Collaboration
```javascript
class CollaborativeGraph {
    constructor(wsClient, graphId) {
        this.client = wsClient;
        this.graphId = graphId;
        this.setupCollaborationHandlers();
    }

    setupCollaborationHandlers() {
        this.client.onMessageType('graph_updated', (message) => {
            if (message.data.graphId === this.graphId) {
                this.handleRemoteUpdate(message.data);
            }
        });

        this.client.onMessageType('node_updated', (message) => {
            if (message.data.graphId === this.graphId) {
                this.handleNodeUpdate(message.data);
            }
        });

        this.client.onMessageType('user_cursor_update', (message) => {
            if (message.data.graphId === this.graphId) {
                this.handleCursorUpdate(message.data);
            }
        });
    }

    updateNode(nodeId, nodeData) {
        this.client.sendMessage('node_update', {
            graphId: this.graphId,
            nodeId,
            nodeData
        });
    }

    updateCursor(x, y) {
        this.client.sendMessage('user_cursor', {
            graphId: this.graphId,
            position: { x, y }
        });
    }

    updateViewport(scale, panOffset) {
        this.client.sendMessage('viewport_update', {
            graphId: this.graphId,
            scale,
            panOffset
        });
    }

    handleRemoteUpdate(data) {
        // Apply remote changes to local graph
        console.log('Remote graph update:', data);
    }

    handleNodeUpdate(data) {
        // Apply remote node changes
        console.log('Remote node update:', data);
    }

    handleCursorUpdate(data) {
        // Update remote user cursor
        console.log('Remote cursor update:', data);
    }
}
```

## File System Operations

### File Management
```javascript
class FileManager {
    constructor(wsClient) {
        this.client = wsClient;
        this.setupHandlers();
    }

    setupHandlers() {
        this.client.onMessageType('file_tree', (message) => {
            this.onFileTreeReceived(message.data);
        });

        this.client.onMessageType('file_content', (message) => {
            this.onFileContentReceived(message.data);
        });

        this.client.onMessageType('file_saved', (message) => {
            console.log(`File saved: ${message.data.path}`);
        });
    }

    getFileTree(rootPath = null) {
        return this.client.sendMessage('get_file_tree', { rootPath });
    }

    readFile(path, requestId = null) {
        return this.client.sendMessage('read_file', { path, requestId });
    }

    writeFile(path, content) {
        return this.client.sendMessage('write_file', { path, content });
    }

    createFile(dirPath, fileName) {
        return this.client.sendMessage('create_file', { dirPath, fileName });
    }

    createDirectory(parentPath, dirName) {
        return this.client.sendMessage('create_directory', { parentPath, dirName });
    }

    deleteFile(path) {
        return this.client.sendMessage('delete_file', { path });
    }

    onFileTreeReceived(data) {
        if (data.success) {
            this.renderFileTree(data);
        }
    }

    onFileContentReceived(data) {
        if (data.success) {
            this.displayFileContent(data.path, data.content);
        }
    }

    renderFileTree(data) {
        // Render file tree UI
        console.log('File tree:', data);
    }

    displayFileContent(path, content) {
        // Display file content in editor
        console.log(`File ${path}:`, content);
    }
}
```

## Workspace Management

### Workspace Operations
```javascript
class WorkspaceManager {
    constructor(wsClient) {
        this.client = wsClient;
        this.setupHandlers();
    }

    setupHandlers() {
        this.client.onMessageType('workspace_list_response', (message) => {
            this.onWorkspacesReceived(message.data.workspaces);
        });

        this.client.onMessageType('workspace_created', (message) => {
            this.onWorkspaceCreated(message.data.workspace);
        });
    }

    getWorkspaces() {
        return this.client.sendMessage('workspace_list', {});
    }

    createWorkspace(name, data = {}, settings = {}) {
        return this.client.sendMessage('create_workspace', {
            name,
            data,
            settings
        });
    }

    updateWorkspace(workspaceId, updates) {
        return this.client.sendMessage('update_workspace', {
            workspaceId,
            ...updates
        });
    }

    deleteWorkspace(workspaceId) {
        return this.client.sendMessage('delete_workspace', { workspaceId });
    }

    onWorkspacesReceived(workspaces) {
        // Update workspace list UI
        console.log('Workspaces:', workspaces);
    }

    onWorkspaceCreated(workspace) {
        // Add new workspace to UI
        console.log('Workspace created:', workspace);
    }
}
```

## Error Handling

### Comprehensive Error Handling
```javascript
class FlowClient {
    constructor(serverUrl, authToken) {
        this.client = new FlowWebSocketClient(serverUrl, authToken);
        this.setupErrorHandlers();
    }

    setupErrorHandlers() {
        this.client.onMessageType('error', (message) => {
            this.handleError(message.data);
        });

        this.client.onMessageType('parse_error', (message) => {
            this.handleParseError(message.data);
        });
    }

    handleError(errorData) {
        console.error('Server error:', errorData.error);
        
        // Handle specific error types
        switch (errorData.error) {
            case 'Rate limit exceeded':
                this.handleRateLimitError();
                break;
            case 'Authentication required':
                this.handleAuthError();
                break;
            case 'Graph not found':
                this.handleGraphNotFound();
                break;
            default:
                this.handleGenericError(errorData);
        }
    }

    handleParseError(errorData) {
        console.error('JSON parse error:', errorData.details);
        // Show user-friendly error message
    }

    handleRateLimitError() {
        console.warn('Rate limit exceeded, implementing backoff');
        // Implement exponential backoff
    }

    handleAuthError() {
        console.error('Authentication required');
        // Redirect to login or refresh token
    }

    handleGraphNotFound() {
        console.error('Graph not found');
        // Show error in UI
    }

    handleGenericError(errorData) {
        console.error('Generic error:', errorData);
        // Show generic error message
    }
}
```

## Connection Management

### Robust Connection Handling
```javascript
class RobustFlowClient {
    constructor(serverUrl, authToken) {
        this.serverUrl = serverUrl;
        this.authToken = authToken;
        this.client = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
        this.isConnected = false;
    }

    async connect() {
        try {
            this.client = new FlowWebSocketClient(this.serverUrl, this.authToken);
            await this.client.connect();
            this.isConnected = true;
            this.reconnectAttempts = 0;
            this.setupConnectionHandlers();
        } catch (error) {
            console.error('Connection failed:', error);
            this.handleConnectionError();
        }
    }

    setupConnectionHandlers() {
        this.client.ws.onclose = () => {
            this.isConnected = false;
            this.handleDisconnection();
        };

        this.client.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.handleConnectionError();
        };
    }

    handleDisconnection() {
        console.log('Connection lost, attempting to reconnect...');
        this.attemptReconnect();
    }

    handleConnectionError() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.attemptReconnect();
        } else {
            console.error('Max reconnection attempts reached');
            this.onConnectionFailed();
        }
    }

    async attemptReconnect() {
        this.reconnectAttempts++;
        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
        
        console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
        
        setTimeout(async () => {
            try {
                await this.connect();
            } catch (error) {
                this.handleConnectionError();
            }
        }, delay);
    }

    onConnectionFailed() {
        // Notify user of connection failure
        console.error('Failed to establish connection');
    }

    sendMessage(type, data) {
        if (this.isConnected && this.client) {
            return this.client.sendMessage(type, data);
        } else {
            console.warn('Cannot send message: not connected');
            return null;
        }
    }
}
```

## Performance Optimization

### Message Batching
```javascript
class BatchedFlowClient {
    constructor(wsClient, batchSize = 10, batchDelay = 100) {
        this.client = wsClient;
        this.batchSize = batchSize;
        this.batchDelay = batchDelay;
        this.messageQueue = [];
        this.batchTimer = null;
    }

    sendMessage(type, data) {
        this.messageQueue.push({ type, data });
        
        if (this.messageQueue.length >= this.batchSize) {
            this.flushBatch();
        } else if (!this.batchTimer) {
            this.batchTimer = setTimeout(() => {
                this.flushBatch();
            }, this.batchDelay);
        }
    }

    flushBatch() {
        if (this.messageQueue.length === 0) return;

        const batch = this.messageQueue.splice(0, this.batchSize);
        
        // Send batch as single message
        this.client.sendMessage('batch', {
            messages: batch
        });

        if (this.batchTimer) {
            clearTimeout(this.batchTimer);
            this.batchTimer = null;
        }
    }
}
```

### Connection Pooling
```javascript
class ConnectionPool {
    constructor(serverUrl, authToken, poolSize = 3) {
        this.serverUrl = serverUrl;
        this.authToken = authToken;
        this.poolSize = poolSize;
        this.connections = [];
        this.availableConnections = [];
        this.initializePool();
    }

    async initializePool() {
        for (let i = 0; i < this.poolSize; i++) {
            const client = new FlowWebSocketClient(this.serverUrl, this.authToken);
            await client.connect();
            this.connections.push(client);
            this.availableConnections.push(client);
        }
    }

    getConnection() {
        if (this.availableConnections.length > 0) {
            return this.availableConnections.pop();
        } else {
            // All connections busy, create new one or wait
            return this.createTemporaryConnection();
        }
    }

    releaseConnection(client) {
        this.availableConnections.push(client);
    }

    async createTemporaryConnection() {
        const client = new FlowWebSocketClient(this.serverUrl, this.authToken);
        await client.connect();
        return client;
    }
}
```

## Testing

### Unit Testing
```javascript
// Jest test example
describe('FlowWebSocketClient', () => {
    let client;
    let mockWebSocket;

    beforeEach(() => {
        mockWebSocket = {
            send: jest.fn(),
            close: jest.fn(),
            onopen: null,
            onmessage: null,
            onerror: null,
            onclose: null
        };
        
        global.WebSocket = jest.fn(() => mockWebSocket);
        client = new FlowWebSocketClient('ws://localhost:9090', 'test-token');
    });

    test('should connect successfully', async () => {
        const connectPromise = client.connect();
        mockWebSocket.onopen();
        await connectPromise;
        expect(mockWebSocket.send).toHaveBeenCalled();
    });

    test('should handle messages correctly', () => {
        const messageHandler = jest.fn();
        client.onMessageType('test_message', messageHandler);
        
        const testMessage = {
            type: 'test_message',
            data: { test: 'data' }
        };
        
        client.handleMessage(testMessage);
        expect(messageHandler).toHaveBeenCalledWith(testMessage);
    });
});
```

### Integration Testing
```javascript
describe('Flow Integration Tests', () => {
    let client;
    let authToken;

    beforeAll(async () => {
        // Authenticate and get token
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'admin', password: 'admin123' })
        });
        const result = await response.json();
        authToken = result.token;
    });

    beforeEach(async () => {
        client = new FlowWebSocketClient('ws://localhost:9090', authToken);
        await client.connect();
    });

    afterEach(() => {
        client.ws.close();
    });

    test('should load graph successfully', async () => {
        return new Promise((resolve) => {
            client.onMessageType('graph_load_response', (message) => {
                expect(message.data.success).toBe(true);
                expect(message.data.graphData).toBeDefined();
                resolve();
            });
            
            client.sendMessage('graph_load', { graphId: 'test-graph' });
        });
    });
});
```

## Best Practices

### 1. Error Handling
- Always implement comprehensive error handling
- Use correlation IDs for request/response tracking
- Implement retry logic with exponential backoff
- Provide user-friendly error messages

### 2. Connection Management
- Implement automatic reconnection
- Handle connection state properly
- Use connection pooling for high-throughput applications
- Monitor connection health with ping/pong

### 3. Message Handling
- Use message type handlers for clean code organization
- Implement message validation on the client side
- Use correlation IDs for async operations
- Batch messages when possible for performance

### 4. Security
- Store authentication tokens securely
- Implement token refresh mechanisms
- Validate all incoming messages
- Use HTTPS/WSS in production

### 5. Performance
- Implement message batching for high-frequency updates
- Use connection pooling for multiple operations
- Monitor and log performance metrics
- Implement rate limiting on the client side

This integration guide provides the foundation for building robust applications that integrate with the Flow backend system effectively.
