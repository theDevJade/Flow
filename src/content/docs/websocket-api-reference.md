---
title: WebSocket API Reference
description: Complete reference for Flow WebSocket message types and protocols
---

# WebSocket API Reference

This document provides a comprehensive reference for all WebSocket message types, protocols, and data structures used in the Flow backend system.

## Connection Protocol

### WebSocket URL
```
ws://localhost:9090/ws?token=<auth_token>
```

### Authentication
All WebSocket connections require a valid authentication token provided either as:
- Query parameter: `?token=<auth_token>`
- Authorization header: `Authorization: Bearer <auth_token>`

### Connection Lifecycle
1. Client establishes WebSocket connection with token
2. Server validates token and creates session
3. Server sends `connection_established` message
4. Client can begin sending application messages
5. Connection closes with cleanup on either side

## Message Structure

### Base Message Format
```json
{
  "type": "string",
  "id": "string?",
  "data": "object",
  "timestamp": "string",
  "userId": "string?",
  "sessionId": "string?"
}
```

### Field Descriptions
- `type`: Message type identifier (required)
- `id`: Optional message ID for request/response correlation
- `data`: Message payload object (required)
- `timestamp`: ISO 8601 timestamp (auto-generated)
- `userId`: User identifier (auto-populated by server)
- `sessionId`: Session identifier (auto-populated by server)

## Authentication Messages

### Auth Message
Authenticate user session with token validation.

**Type**: `auth`

**Request**:
```json
{
  "type": "auth",
  "data": {
    "userId": "string",
    "username": "string",
    "token": "string?"
  }
}
```

**Response**:
```json
{
  "type": "auth_response",
  "data": {
    "success": true,
    "userId": "string",
    "username": "string",
    "sessionId": "string"
  }
}
```

## Connection Management

### Ping Message
Test connection health and latency.

**Type**: `ping`

**Request**:
```json
{
  "type": "ping",
  "data": {}
}
```

**Response**:
```json
{
  "type": "pong",
  "data": {
    "timestamp": 1234567890,
    "sessionId": "string"
  }
}
```

### Heartbeat Message
Keep connection alive and update activity status.

**Type**: `heartbeat`

**Request**:
```json
{
  "type": "heartbeat",
  "data": {}
}
```

**Response**:
```json
{
  "type": "heartbeat_ack",
  "data": {
    "timestamp": 1234567890,
    "status": "alive",
    "sessionId": "string"
  }
}
```

### Connection Established
Sent by server when connection is successfully established.

**Type**: `connection_established`

**Data**:
```json
{
  "sessionId": "string",
  "userId": "string",
  "username": "string",
  "server_version": "1.0.0",
  "timestamp": 1234567890,
  "active_sessions": 5,
  "authenticated": true
}
```

## Graph Operations

### Graph Update
Broadcast real-time graph modifications to all connected clients.

**Type**: `graph_update`

**Request**:
```json
{
  "type": "graph_update",
  "data": {
    "graphId": "string",
    "updateType": "string?",
    "graphData": "object?"
  }
}
```

**Broadcast**:
```json
{
  "type": "graph_updated",
  "data": {
    "graphId": "string",
    "updateType": "string",
    "updatedBy": "string"
  }
}
```

### Graph Save
Persist graph data to storage.

**Type**: `graph_save`

**Request**:
```json
{
  "type": "graph_save",
  "data": {
    "graphId": "string",
    "graphData": {
      "nodes": [...],
      "connections": [...],
      "version": "1.0.0",
      "metadata": {}
    }
  }
}
```

**Response**:
```json
{
  "type": "success",
  "data": {
    "success": true,
    "message": "Graph saved successfully",
    "graphId": "string"
  }
}
```

**Broadcast**:
```json
{
  "type": "graph_saved",
  "data": {
    "graphId": "string",
    "savedBy": "string"
  }
}
```

### Graph Load
Retrieve graph data from storage.

**Type**: `graph_load`

**Request**:
```json
{
  "type": "graph_load",
  "data": {
    "graphId": "string"
  }
}
```

**Response**:
```json
{
  "type": "graph_load_response",
  "data": {
    "success": true,
    "graphId": "string",
    "graphData": {
      "nodes": [...],
      "connections": [...],
      "version": "1.0.0",
      "metadata": {}
    }
  }
}
```

### Graph List
Get list of available graphs.

**Type**: `graph_list`

**Request**:
```json
{
  "type": "graph_list",
  "data": {}
}
```

**Response**:
```json
{
  "type": "graph_list_response",
  "data": {
    "success": true,
    "graphs": [
      {
        "id": "string",
        "name": "string",
        "description": "string",
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

## Node Operations

### Node Update
Modify node properties or position.

**Type**: `node_update`

**Request**:
```json
{
  "type": "node_update",
  "data": {
    "graphId": "string",
    "nodeId": "string",
    "nodeData": {
      "id": "string",
      "name": "string",
      "position": {"x": 0, "y": 0},
      "properties": {}
    }
  }
}
```

**Broadcast**:
```json
{
  "type": "node_updated",
  "data": {
    "graphId": "string",
    "nodeId": "string",
    "nodeData": {},
    "updatedBy": "string"
  }
}
```

### Node Templates
Request available node templates.

**Type**: `node_templates`

**Request**:
```json
{
  "type": "node_templates",
  "data": {}
}
```

**Response**:
```json
{
  "type": "node_templates_response",
  "data": {
    "success": true,
    "templates": {
      "version": "1.0",
      "node_templates": [
        {
          "id": "input_material",
          "name": "Material Input",
          "description": "Material input node for shaders",
          "category": "Input",
          "color": {"r": 0.2, "g": 0.6, "b": 0.2, "a": 1.0},
          "size": {"width": 150, "height": 80},
          "inputs": [],
          "outputs": [...],
          "properties": [...]
        }
      ]
    }
  }
}
```

## Connection Operations

### Connection Update
Modify graph connections between nodes.

**Type**: `connection_update`

**Request**:
```json
{
  "type": "connection_update",
  "data": {
    "graphId": "string",
    "connectionId": "string",
    "connectionData": {
      "id": "string",
      "fromNodeId": "string",
      "fromPortId": "string",
      "toNodeId": "string",
      "toPortId": "string",
      "color": 0xFF0000
    }
  }
}
```

**Broadcast**:
```json
{
  "type": "connection_updated",
  "data": {
    "graphId": "string",
    "connectionId": "string",
    "connectionData": {},
    "updatedBy": "string"
  }
}
```

## File System Operations

### Get File Tree
Request project file structure.

**Type**: `get_file_tree`

**Request**:
```json
{
  "type": "get_file_tree",
  "data": {
    "rootPath": "string?"
  }
}
```

**Response**:
```json
{
  "type": "file_tree",
  "data": {
    "success": true,
    "name": "root",
    "fullPath": "/",
    "isDirectory": true,
    "lastModified": "2024-01-01T00:00:00Z",
    "size": 0,
    "children": [
      {
        "name": "file.txt",
        "fullPath": "/file.txt",
        "isDirectory": false,
        "lastModified": "2024-01-01T00:00:00Z",
        "size": 1024,
        "children": []
      }
    ]
  }
}
```

### Read File
Read file contents.

**Type**: `read_file`

**Request**:
```json
{
  "type": "read_file",
  "data": {
    "path": "string",
    "requestId": "string?"
  }
}
```

**Response**:
```json
{
  "type": "file_content",
  "data": {
    "success": true,
    "path": "string",
    "content": "string",
    "requestId": "string?"
  }
}
```

### Write File
Save file contents.

**Type**: `write_file`

**Request**:
```json
{
  "type": "write_file",
  "data": {
    "path": "string",
    "content": "string"
  }
}
```

**Response**:
```json
{
  "type": "file_saved",
  "data": {
    "success": true,
    "path": "string"
  }
}
```

### Create File
Create new file.

**Type**: `create_file`

**Request**:
```json
{
  "type": "create_file",
  "data": {
    "dirPath": "string",
    "fileName": "string"
  }
}
```

**Response**:
```json
{
  "type": "file_created",
  "data": {
    "success": true,
    "path": "string"
  }
}
```

### Create Directory
Create new directory.

**Type**: `create_directory`

**Request**:
```json
{
  "type": "create_directory",
  "data": {
    "parentPath": "string",
    "dirName": "string"
  }
}
```

**Response**:
```json
{
  "type": "directory_created",
  "data": {
    "success": true,
    "path": "string"
  }
}
```

### Delete File
Delete file or directory.

**Type**: `delete_file` / `delete_directory`

**Request**:
```json
{
  "type": "delete_file",
  "data": {
    "path": "string"
  }
}
```

**Response**:
```json
{
  "type": "file_deleted",
  "data": {
    "success": true,
    "path": "string"
  }
}
```

## Workspace Management

### Workspace List
Get user workspaces.

**Type**: `workspace_list`

**Request**:
```json
{
  "type": "workspace_list",
  "data": {}
}
```

**Response**:
```json
{
  "type": "workspace_list_response",
  "data": {
    "success": true,
    "workspaces": [
      {
        "workspaceId": "string",
        "name": "string",
        "data": {},
        "currentPage": "string?",
        "settings": {},
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

### Create Workspace
Create new workspace.

**Type**: `create_workspace`

**Request**:
```json
{
  "type": "create_workspace",
  "data": {
    "name": "string",
    "data": {},
    "settings": {}
  }
}
```

**Response**:
```json
{
  "type": "workspace_created",
  "data": {
    "success": true,
    "workspace": {
      "workspaceId": "string",
      "name": "string",
      "data": {},
      "currentPage": null,
      "settings": {},
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    }
  }
}
```

### Update Workspace
Modify workspace settings.

**Type**: `update_workspace`

**Request**:
```json
{
  "type": "update_workspace",
  "data": {
    "workspaceId": "string",
    "name": "string?",
    "data": {},
    "settings": {}
  }
}
```

**Response**:
```json
{
  "type": "workspace_updated",
  "data": {
    "success": true,
    "workspace": {
      "workspaceId": "string",
      "name": "string",
      "data": {},
      "currentPage": "string?",
      "settings": {},
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z"
    }
  }
}
```

### Delete Workspace
Remove workspace.

**Type**: `delete_workspace`

**Request**:
```json
{
  "type": "delete_workspace",
  "data": {
    "workspaceId": "string"
  }
}
```

**Response**:
```json
{
  "type": "workspace_deleted",
  "data": {
    "success": true,
    "workspaceId": "string"
  }
}
```

## Real-time Collaboration

### User Cursor
Broadcast cursor position for real-time collaboration.

**Type**: `user_cursor`

**Request**:
```json
{
  "type": "user_cursor",
  "data": {
    "graphId": "string",
    "position": {
      "x": 0,
      "y": 0
    }
  }
}
```

**Broadcast**:
```json
{
  "type": "user_cursor_update",
  "data": {
    "graphId": "string",
    "userId": "string",
    "position": {
      "x": 0,
      "y": 0
    }
  }
}
```

### Viewport Update
Share viewport state for synchronized navigation.

**Type**: `viewport_update`

**Request**:
```json
{
  "type": "viewport_update",
  "data": {
    "graphId": "string",
    "scale": 1.0,
    "panOffset": {
      "dx": 0,
      "dy": 0
    }
  }
}
```

**Broadcast**:
```json
{
  "type": "viewport_updated",
  "data": {
    "graphId": "string",
    "scale": 1.0,
    "panOffset": {
      "dx": 0,
      "dy": 0
    },
    "updatedBy": "string"
  }
}
```

## Terminal Operations

### Terminal Command
Execute terminal command.

**Type**: `terminal_command`

**Request**:
```json
{
  "type": "terminal_command",
  "data": {
    "command": "string",
    "cwd": "string?",
    "pageId": "string?"
  }
}
```

**Response**:
```json
{
  "type": "terminal_response",
  "data": {
    "success": true,
    "output": ["line1", "line2"],
    "cwd": "string",
    "exitCode": 0,
    "pageId": "string?"
  }
}
```

## Error Messages

### Error Response
Standard error message format.

**Type**: `error`

**Data**:
```json
{
  "error": "Error description",
  "correlationId": "string",
  "timestamp": 1234567890
}
```

### Parse Error
JSON parsing error.

**Type**: `parse_error`

**Data**:
```json
{
  "error": "Invalid JSON format",
  "details": "Detailed error message",
  "timestamp": 1234567890
}
```

### Success Response
Standard success message format.

**Type**: `success`

**Data**:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "correlationId": "string",
  "timestamp": 1234567890
}
```

## Data Structures

### Graph Data
```json
{
  "nodes": [
    {
      "id": "string",
      "name": "string",
      "inputs": [
        {
          "id": "string",
          "name": "string",
          "isInput": true,
          "color": 0xFF0000
        }
      ],
      "outputs": [...],
      "color": 0xFF0000,
      "position": {"x": 0, "y": 0},
      "size": {"width": 150, "height": 80},
      "templateId": "string?",
      "properties": {}
    }
  ],
  "connections": [
    {
      "id": "string",
      "fromNodeId": "string",
      "fromPortId": "string",
      "toNodeId": "string",
      "toPortId": "string",
      "color": 0xFF0000
    }
  ],
  "version": "1.0.0",
  "metadata": {}
}
```

### File Tree Node
```json
{
  "name": "string",
  "fullPath": "string",
  "isDirectory": true,
  "lastModified": "2024-01-01T00:00:00Z",
  "size": 1024,
  "children": [...]
}
```

### Workspace Data
```json
{
  "id": "string",
  "name": "string",
  "currentPage": "string?",
  "pages": [
    {
      "id": "string",
      "name": "string",
      "type": "graph|code|terminal",
      "data": {},
      "lastModified": "2024-01-01T00:00:00Z"
    }
  ],
  "settings": {},
  "lastModified": "2024-01-01T00:00:00Z",
  "version": "1.0.0"
}
```

## Rate Limiting

### Limits
- **Messages per window**: 1000 messages per 60 seconds per session
- **Message size**: Maximum 1MB per message
- **Processing timeout**: 30 seconds per message

### Rate Limit Exceeded
When rate limits are exceeded, the server responds with:

```json
{
  "type": "error",
  "data": {
    "error": "Rate limit exceeded",
    "correlationId": "string",
    "timestamp": 1234567890
  }
}
```

## Connection Management

### User Left
Broadcast when user disconnects.

**Type**: `user_left`

**Data**:
```json
{
  "userId": "string",
  "username": "string",
  "sessionId": "string",
  "timestamp": 1234567890
}
```

### Health Check
Monitor server health.

**Endpoint**: `GET /ws/health`

**Response**:
```json
{
  "status": "healthy",
  "active_sessions": 5,
  "users_online": 3,
  "timestamp": 1234567890
}
```

### Active Users
Get list of active users.

**Endpoint**: `GET /ws/users`

**Response**:
```json
{
  "users": [
    {
      "userId": "string",
      "username": "string",
      "sessionId": "string"
    }
  ],
  "count": 3,
  "timestamp": 1234567890
}
```

This comprehensive API reference provides all the information needed to integrate with the Flow WebSocket system effectively.
