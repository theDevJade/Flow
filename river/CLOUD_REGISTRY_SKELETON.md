
## API Structure

### Base URL
### Flowlang.org is just a placeholder
```
https://registry.flowlang.org/api/v1
```

### Endpoints

#### 1. Get Package Info

**Endpoint:** `GET /api/v1/packages/{name}`

**Response:**
```json
{
  "name": "http",
  "version": "2.0.0",
  "description": "HTTP client library for Flow",
  "author": "Flow Team",
  "license": "MIT",
  "download_url": "https://registry.flowlang.org/packages/http-2.0.0.tar.gz",
  "checksum": "abc123...",
  "dependencies": ["json", "tls"],
  "repository": "https://github.com/flowlang/http",
  "homepage": "https://flowlang.org/packages/http",
  "keywords": ["http", "client", "rest"],
  "published_at": "2025-10-11T12:00:00Z"
}
```

#### 2. Download Package

**Endpoint:** `GET /api/v1/packages/{name}/{version}/download`

**Response:** Binary `.tar.gz` file

**Headers:**
- `Content-Type: application/gzip`
- `Content-Length: {size}`
- `X-Checksum: {sha256}`

#### 3. Publish Package

**Endpoint:** `POST /api/v1/publish`

**Headers:**
- `Authorization: Bearer {token}`
- `Content-Type: multipart/form-data`

**Body:**
```
package: <binary .tar.gz file>
name: "mypackage"
version: "1.0.0"
checksum: "abc123..."
```

**Response:**
```json
{
  "success": true,
  "message": "Package published successfully",
  "url": "https://registry.flowlang.org/packages/mypackage"
}
```

#### 4. Search Packages

**Endpoint:** `GET /api/v1/search?q={query}&limit={limit}`

**Response:**
```json
{
  "results": [
    {
      "name": "http",
      "version": "2.0.0",
      "description": "HTTP client library",
      "downloads": 15420
    }
  ],
  "total": 1
}
```

#### 5. List Versions

**Endpoint:** `GET /api/v1/packages/{name}/versions`

**Response:**
```json
{
  "versions": [
    "2.0.0",
    "1.9.5",
    "1.9.0",
    "1.8.3"
  ]
}
```

---

## Implementation Guide

### Phase 1: Basic HTTP Client

**File:** `river/src/registry.rs`

**Add to Registry struct:**
```rust
use reqwest::blocking::Client;

pub struct Registry {
    url: String,
    client: Client,
    auth_token: Option<String>,
}

impl Registry {
    pub fn new(url: String) -> Self {
        Registry {
            url,
            client: Client::builder()
                .user_agent("river-pm/0.1.0")
                .timeout(Duration::from_secs(30))
                .build()
                .unwrap(),
            auth_token: None,
        }
    }
}
```

### Phase 2: Get Package Info

**Implement:**
```rust
pub fn get_package_info(&self, name: &str) -> Result<PackageInfo, Box<dyn std::error::Error>> {
    let url = format!("{}/api/v1/packages/{}", self.url, name);
    
    let response = self.client
        .get(&url)
        .send()?;
    
    if response.status().is_success() {
        let pkg_info: PackageInfo = response.json()?;
        Ok(pkg_info)
    } else if response.status() == 404 {
        Err(format!("Package '{}' not found in registry", name).into())
    } else {
        Err(format!("Registry error: {}", response.status()).into())
    }
}
```

### Phase 3: Download Package

**Implement with progress tracking:**
```rust
use std::fs::File;
use std::io::{Read, Write};

pub fn download_package(
    &self,
    name: &str,
    version: &str,
    config: &Config,
    progress: &ProgressBar,
) -> Result<PathBuf, Box<dyn std::error::Error>> {
    let url = format!("{}/api/v1/packages/{}/{}/download", self.url, name, version);
    
    // Start download
    let mut response = self.client.get(&url).send()?;
    let total_size = response.content_length().unwrap_or(0);
    
    // Create temporary file
    let tarball_path = config.paths.cache.join(format!("{}-{}.tar.gz", name, version));
    let mut file = File::create(&tarball_path)?;
    
    // Download with progress
    progress.set_length(total_size);
    let mut downloaded = 0u64;
    let mut buffer = [0; 8192];
    
    while let Ok(n) = response.read(&mut buffer) {
        if n == 0 { break; }
        file.write_all(&buffer[..n])?;
        downloaded += n as u64;
        progress.set_position(downloaded);
    }
    
    // Extract to packages directory
    let pkg_dir = config.paths.packages.join(format!("{}-{}", name, version));
    extract_tarball(&tarball_path, &pkg_dir)?;
    
    // Clean up tarball
    std::fs::remove_file(tarball_path)?;
    
    Ok(pkg_dir)
}
```

### Phase 4: Checksum Verification

**Implement:**
```rust
use sha2::{Sha256, Digest};

fn verify_checksum(&self, path: &PathBuf, expected: &str) -> Result<bool, Box<dyn std::error::Error>> {
    let mut file = File::open(path)?;
    let mut hasher = Sha256::new();
    
    std::io::copy(&mut file, &mut hasher)?;
    let computed = format!("{:x}", hasher.finalize());
    
    Ok(computed == expected)
}
```

### Phase 5: Tarball Extraction

**Implement:**
```rust
use flate2::read::GzDecoder;
use tar::Archive;

fn extract_tarball(tarball: &PathBuf, dest: &PathBuf) -> Result<(), Box<dyn std::error::Error>> {
    let file = File::open(tarball)?;
    let gz = GzDecoder::new(file);
    let mut archive = Archive::new(gz);
    
    std::fs::create_dir_all(dest)?;
    archive.unpack(dest)?;
    
    Ok(())
}
```

### Phase 6: Publishing

**Implement:**
```rust
use reqwest::multipart;

pub fn publish_package(
    &self,
    package: &Package,
    config: &Config,
) -> Result<(), Box<dyn std::error::Error>> {
    // Require authentication
    let token = self.auth_token.as_ref()
        .ok_or("Authentication required. Run 'river login' first")?;
    
    // Create tarball
    let tarball_path = create_tarball(package)?;
    
    // Calculate checksum
    let checksum = compute_sha256(&tarball_path)?;
    
    // Prepare multipart form
    let form = multipart::Form::new()
        .file("package", &tarball_path)?
        .text("name", package.manifest.package.name.clone())
        .text("version", package.manifest.package.version.clone())
        .text("checksum", checksum);
    
    // Upload
    let response = self.client
        .post(&format!("{}/api/v1/publish", self.url))
        .header("Authorization", format!("Bearer {}", token))
        .multipart(form)
        .send()?;
    
    if response.status().is_success() {
        Ok(())
    } else {
        let error: serde_json::Value = response.json()?;
        Err(format!("Publish failed: {}", error["message"]).into())
    }
}
```

---

## Testing the Skeleton

### Current Behavior

```bash
cd test-packages/hello-world
river install http
```

**Output:**
```
→ Installing package 'http'...

⠋ Fetching package info from registry...
✓ Package info fetched
  → Name: http
  → Version: 2.0.0
  → Description: HTTP client library

⠋ Resolving dependencies...
✓ Dependencies resolved

█▓▒░ [████████████████████████] 100% Downloading package...
✓ Download complete

⠋ Verifying package integrity...
✓ Package verified

════════════════════════════════════════════════════════════
SUCCESS Successfully installed http @ 2.0.0

  Location: ~/.river/packages/http-2.0.0
```

**Current Implementation:**
- Creates placeholder directories
- Shows beautiful animated UI
- Demonstrates the expected flow
- Returns error: "Registry not yet implemented"

---

## Server-Side Requirements

stuff need server yayy

### 1. Database Schema

```sql
CREATE TABLE packages (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    author VARCHAR(255) NOT NULL,
    license VARCHAR(50),
    repository TEXT,
    homepage TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE package_versions (
    id SERIAL PRIMARY KEY,
    package_id INTEGER REFERENCES packages(id),
    version VARCHAR(50) NOT NULL,
    download_url TEXT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    published_at TIMESTAMP DEFAULT NOW(),
    downloads INTEGER DEFAULT 0,
    UNIQUE(package_id, version)
);

CREATE TABLE dependencies (
    id SERIAL PRIMARY KEY,
    package_version_id INTEGER REFERENCES package_versions(id),
    dependency_name VARCHAR(255) NOT NULL,
    version_constraint VARCHAR(50)
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    api_token VARCHAR(64) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 2. REST API Server

**Technologies:**
- **Backend:** idc
- **Database:** PostgreSQL (preferably)
- **Storage:** S3 or similar for package files
- **Authentication:** JWT tokens

### 3. CDN for Package Distribution

- **CloudFront**, **Cloudflare**, or **Fastly**
- Cached package downloads
- Global distribution

### 4. Rate Limiting

- Prevent abuse
- Fair usage policies
- API quotas per user/IP

---

## Configuration

### User Config

**File:** `~/.river/config.toml`

```toml
[registry]
url = "https://registry.flowlang.org"
mirrors = [
    "https://mirror1.flowlang.org",
    "https://mirror2.flowlang.org"
]

[auth]
token = "abc123..."

[download]
parallel = 4
timeout = 30

[cache]
enabled = true
ttl = 3600
```

### Package Manifest

**File:** `River.toml`

```toml
[package]
name = "mylib"
version = "1.0.0"
authors = ["Your Name <you@example.com>"]
license = "MIT"
description = "My awesome library"
repository = "https://github.com/user/mylib"
homepage = "https://mylib.dev"
keywords = ["lib", "awesome"]

[dependencies]
http = "2.0.0"
json = "^1.5"

[dev_dependencies]
test = "0.9"
```

---

## Authentication Flow

### 1. Login Command

```bash
river login
```

**Prompts:**
```
Email: user@example.com
Password: ********
```

**Flow:**
1. POST `/api/v1/auth/login` with credentials
2. Receive JWT token
3. Store in `~/.river/config.toml`

### 2. Publish with Auth

```bash
river publish
```

**Flow:**
1. Read token from config
2. Include in `Authorization: Bearer {token}` header
3. Upload package

---

## Dependencies to Add

**Update Cargo.toml:**
```toml
[dependencies]
reqwest = { version = "0.11", features = ["blocking", "json", "multipart"] }
sha2 = "0.10"
tar = "0.4"
flate2 = "1.0"
tokio = { version = "1", features = ["full"] }
```

---

## Security Considerations

### 1. Package Verification

- **Always verify checksums** before extraction
- Use SHA256 (not MD5 or SHA1)
- Reject packages with invalid checksums

### 2. Authentication

- Use HTTPS only
- Store tokens securely
- Support token rotation

### 3. Sandboxing

- Run package builds in containers
- Limit network access during build
- Scan for malware

### 4. Rate Limiting

- Limit download bandwidth per user
- Prevent DOS attacks
- Implement exponential backoff
