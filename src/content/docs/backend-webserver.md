---
title: Backend WebServer
description: Comprehensive documentation for the Flow backend webserver and WebSocket architecture
---

# Backend WebServer

The Flow backend webserver is a Kotlin-based server built on the Ktor framework, providing real-time WebSocket communication, authentication, and data persistence for the Flow development platform.

## Architecture Overview

The backend consists of several key components:

- **WebSocket Server**: Real-time bidirectional communication
- **HTTP API**: RESTful endpoints for authentication and data access
- **Database Layer**: SQLite-based persistence with Exposed ORM
- **Authentication System**: Token-based user authentication
- **File System Integration**: Project file management and access
- **Graph Data Management**: Visual graph storage and synchronization

## Server Configuration

### Application Entry Point

The server is initialized through `Application.kt` and `WebSocketMain.kt`:

```kotlin
// Main server configuration
object FlowWebserver {
    fun run() {
        DatabaseManager.initialize()
        embeddedServer(Netty, configure = {
            connectors.add(EngineConnectorBuilder().apply {
                host = FlowConfiguration.webserverConfig.hostName!!
                port = FlowConfiguration.webserverConfig.webserverPort!!
            })
        }) {
            module()
        }.start(wait = true)
    }
}
```

### Configuration Parameters

- **Host**: Configurable hostname (default: localhost)
- **WebSocket Port**: 9090 (configurable)
- **HTTP Port**: 8080 (configurable)
- **Database**: SQLite with WAL mode enabled
- **Connection Limits**: 2 connection groups, 5 worker groups, 10 call groups

## WebSocket Communication

### Connection Protocol

WebSocket connections are established at `/ws` with the following requirements:

1. **Authentication Token**: Required in query parameter or Authorization header
2. **Token Validation**: Server validates token before accepting connection
3. **Session Management**: Each connection gets a unique session ID

### Message Format

All WebSocket messages follow a standardized JSON structure:

```json
{
  "type": "message_type",
  "id": "optional_message_id",
  "data": {
    "key": "value"
  },
  "timestamp": "2024-01-01T00:00:00Z",
  "userId": "user_id",
  "sessionId": "session_id"
}
```

### Supported Message Types

#### Authentication Messages
- `auth`: User authentication with token validation
- `auth_response`: Authentication result confirmation

#### Connection Management
- `ping`/`pong`: Connection health monitoring
- `heartbeat`/`heartbeat_ack`: Keep-alive mechanism
- `connection_established`: Initial connection confirmation

#### Graph Operations
- `graph_update`: Real-time graph modifications
- `graph_save`: Persist graph data to storage
- `graph_load`: Retrieve graph from storage
- `graph_list`: Get available graphs
- `graph_updated`: Broadcast graph changes
- `graph_saved`: Confirm graph persistence

#### Node Management
- `node_update`: Modify node properties or position
- `node_updated`: Broadcast node changes
- `node_templates`: Request available node templates
- `node_templates_response`: Return node template definitions

#### File System Operations
- `get_file_tree`: Request project file structure
- `file_tree`: Return file system tree
- `read_file`: Read file contents
- `file_content`: Return file data
- `write_file`: Save file changes
- `file_saved`: Confirm file save
- `create_file`/`create_directory`: Create new files/folders
- `delete_file`/`delete_directory`: Remove files/folders

#### Workspace Management
- `workspace_list`: Get user workspaces
- `create_workspace`: Create new workspace
- `update_workspace`: Modify workspace settings
- `delete_workspace`: Remove workspace

#### Real-time Collaboration
- `user_cursor`: Broadcast cursor position
- `user_cursor_update`: Real-time cursor tracking
- `viewport_update`: Share viewport state
- `viewport_updated`: Broadcast viewport changes

### Message Validation

All incoming messages are validated using a comprehensive validation system:

- **Rate Limiting**: 1000 messages per 60-second window per session
- **Message Size**: Maximum 1MB per message
- **Authentication**: Required for sensitive operations
- **Schema Validation**: Type-specific validation for each message type

## Database Architecture

### SQLite Configuration

The server uses SQLite with the following optimizations:

- **WAL Mode**: Write-Ahead Logging for better concurrency
- **Normal Synchronization**: Balanced performance and safety
- **Memory Temp Store**: Improved temporary operation performance

### Database Tables

#### Users Table
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

#### Authentication Tokens
```sql
CREATE TABLE auth_tokens (
    token TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### Graph Data
```sql
CREATE TABLE graph_data (
    id TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    graph_data TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### Workspace Data
```sql
CREATE TABLE workspace_data (
    id TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    data TEXT NOT NULL,
    current_page TEXT,
    settings TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Authentication System

### Token-Based Authentication

The authentication system uses secure tokens with the following characteristics:

- **Token Generation**: UUID-based with 24-hour expiry
- **Password Hashing**: SHA-256 with salt
- **Token Validation**: Database-backed validation with expiry checking
- **Session Management**: Automatic cleanup of expired tokens

### Default Users

The system includes pre-configured test users:

- `admin` / `admin123`
- `user` / `password`
- `demo` / `demo`
- `test` / `test123`

### Authentication Endpoints

#### POST /auth/login
```json
{
  "username": "string",
  "password": "string"
}
```

Response:
```json
{
  "success": true,
  "token": "auth_token",
  "username": "username",
  "userId": "user_id",
  "expiresAt": 1234567890
}
```

#### POST /auth/logout
Headers: `Authorization: Bearer <token>`

#### GET /auth/validate
Headers: `Authorization: Bearer <token>`

#### GET /auth/stats
Returns active token and user counts.

## File System Integration

### Project File Access

The server provides comprehensive file system access through the Flow API:

- **Root Directory**: Configurable project root
- **File Operations**: Read, write, create, delete files and directories
- **Tree Navigation**: Hierarchical file structure browsing
- **Path Validation**: Secure path resolution and validation

### File System API

The file system integration supports:

- **File Tree Generation**: Recursive directory scanning
- **Content Management**: Text file reading and writing
- **Path Operations**: Safe path manipulation and validation
- **Permission Handling**: User-based access control

## Graph Data Management

### Graph Storage

Graphs are stored as JSON documents with the following structure:

```json
{
  "nodes": [
    {
      "id": "node_id",
      "name": "Node Name",
      "inputs": [...],
      "outputs": [...],
      "color": 0xFF0000,
      "position": {"x": 0, "y": 0},
      "templateId": "template_id",
      "properties": {...}
    }
  ],
  "connections": [
    {
      "id": "connection_id",
      "fromNodeId": "node1",
      "fromPortId": "port1",
      "toNodeId": "node2",
      "toPortId": "port2",
      "color": 0x00FF00
    }
  ],
  "version": "1.0.0",
  "metadata": {...}
}
```

### Graph Operations

- **Real-time Updates**: Live graph modification broadcasting
- **Persistence**: Automatic saving to database and file system
- **Versioning**: Graph version tracking and management
- **Collaboration**: Multi-user real-time editing support

## Session Management

### WebSocket Session Lifecycle

1. **Connection**: Client connects with authentication token
2. **Validation**: Server validates token and creates session
3. **Activity Tracking**: Session activity and heartbeat monitoring
4. **Cleanup**: Automatic session cleanup on disconnect

### Session Data

Each session maintains:

- **Session ID**: Unique identifier
- **User Information**: User ID and username
- **Connection State**: WebSocket session reference
- **Activity Tracking**: Last activity timestamp
- **Viewport State**: Current graph viewport information
- **Metadata**: Custom session-specific data

## Error Handling

### Error Response Format

All errors follow a consistent format:

```json
{
  "type": "error",
  "id": "message_id",
  "data": {
    "error": "Error description",
    "correlationId": "request_id",
    "timestamp": 1234567890
  }
}
```

### Error Types

- **Authentication Errors**: Invalid or expired tokens
- **Validation Errors**: Malformed or invalid messages
- **Rate Limiting**: Too many requests per time window
- **Resource Errors**: File or graph not found
- **System Errors**: Internal server errors

## Performance Considerations

### Connection Management

- **Connection Pooling**: Efficient connection reuse
- **Timeout Handling**: 30-second message processing timeout
- **Memory Management**: Automatic cleanup of disconnected sessions

### Database Optimization

- **WAL Mode**: Improved concurrent access
- **Connection Pooling**: Efficient database connection management
- **Query Optimization**: Indexed queries for common operations

### Message Processing

- **Asynchronous Processing**: Non-blocking message handling
- **Rate Limiting**: Prevents system overload
- **Message Validation**: Early rejection of invalid messages

## Security Features

### Authentication Security

- **Token Expiry**: Automatic token expiration
- **Password Hashing**: Secure password storage
- **Session Isolation**: User-specific session management

### Input Validation

- **Message Size Limits**: Prevents large message attacks
- **Path Validation**: Secure file system access
- **Type Validation**: Strict message format validation

### Rate Limiting

- **Per-Session Limits**: Individual session rate limiting
- **Global Monitoring**: System-wide usage tracking
- **Automatic Cleanup**: Removal of expired rate limit data

## Monitoring and Logging

### Logging System

The server includes comprehensive logging:

- **Debug Logging**: Detailed operation tracking
- **Error Logging**: Exception and error recording
- **Performance Logging**: Timing and performance metrics
- **Security Logging**: Authentication and authorization events

### Health Monitoring

- **Health Endpoint**: `/ws/health` for system status
- **User Statistics**: `/ws/users` for active user information
- **Database Health**: Connection and query monitoring

## Development Integration

### WebSocket Client Integration

To connect to the WebSocket server:

```javascript
const ws = new WebSocket('ws://localhost:9090/ws?token=your_auth_token');

ws.onopen = function() {
    console.log('Connected to Flow WebSocket server');
};

ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    handleWebSocketMessage(message);
};

function sendMessage(type, data) {
    const message = {
        type: type,
        data: data,
        timestamp: new Date().toISOString()
    };
    ws.send(JSON.stringify(message));
}
```

### Authentication Flow

1. **Login**: POST to `/auth/login` with credentials
2. **Token Storage**: Store received token securely
3. **WebSocket Connection**: Connect with token in query parameter
4. **Session Management**: Handle connection lifecycle

### Message Handling

Implement message handlers for each message type:

```javascript
function handleWebSocketMessage(message) {
    switch(message.type) {
        case 'connection_established':
            handleConnectionEstablished(message.data);
            break;
        case 'graph_updated':
            handleGraphUpdate(message.data);
            break;
        case 'file_content':
            handleFileContent(message.data);
            break;
        // ... other message types
    }
}
```

## Troubleshooting

### Common Issues

1. **Connection Refused**: Check server status and port configuration
2. **Authentication Failed**: Verify token validity and expiry
3. **Message Validation Errors**: Check message format and required fields
4. **Rate Limit Exceeded**: Implement client-side rate limiting

### Debug Information

Enable debug logging by setting appropriate log levels in the server configuration. Debug logs include:

- Session creation and cleanup
- Message processing details
- Authentication events
- Database operations
- Error stack traces

## API Reference

### WebSocket Endpoints

- `ws://localhost:9090/ws` - Main WebSocket connection
- `GET /ws/health` - Health check endpoint
- `GET /ws/users` - Active users endpoint

### HTTP Endpoints

- `POST /auth/login` - User authentication
- `POST /auth/logout` - Token revocation
- `GET /auth/validate` - Token validation
- `GET /auth/stats` - Authentication statistics

This documentation provides a comprehensive overview of the Flow backend webserver architecture, enabling developers to integrate with and extend the system effectively.
