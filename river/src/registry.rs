use crate::config::Config;
use crate::package::Package;
use colored::Colorize;
use serde::{Deserialize, Serialize};
use std::fs::{self, File};
use std::path::PathBuf;
use std::io::{Read, Write};
use reqwest::blocking::Client;
use sha2::{Sha256, Digest};
use flate2::read::GzDecoder;
use tar::Archive;

/// Package metadata from the cloud registry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PackageInfo {
    pub name: String,
    pub version: String,
    pub description: Option<String>,
    pub author: String,
    pub license: Option<String>,
    pub download_url: String,
    pub checksum: Option<String>,        // SHA256 hash for verification
    pub dependencies: Vec<String>,       // List of dependencies
    pub repository: Option<String>,      // Git repository URL
    pub homepage: Option<String>,        // Project homepage
    pub keywords: Vec<String>,           // Search keywords
    pub published_at: Option<String>,    // Timestamp
}

/// Search result from registry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchResult {
    pub name: String,
    pub version: String,
    pub description: Option<String>,
    pub downloads: u64,                  // Download count
}


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
                .timeout(std::time::Duration::from_secs(30))
                .build()
                .unwrap(),
            auth_token: None,
        }
    }

    pub fn with_auth(mut self, token: String) -> Self {
        self.auth_token = Some(token);
        self
    }

    /// Fetch package metadata from the cloud registry
    ///
    /// API Endpoint: GET /api/v1/packages/{name}
    ///
    /// Returns: PackageInfo JSON
    ///
    /// TODO: Implement actual HTTP request:
    /// ```rust
    /// let response = self.client
    ///     .get(&format!("{}/api/v1/packages/{}", self.url, name))
    ///     .header("User-Agent", "river-pm/0.1.0")
    ///     .send()?;
    ///
    /// if response.status().is_success() {
    ///     let pkg_info: PackageInfo = response.json()?;
    ///     Ok(pkg_info)
    /// } else {
    ///     Err(format!("Package not found: {}", name).into())
    /// }
    /// ```
    pub fn get_package_info(&self, name: &str) -> Result<PackageInfo, Box<dyn std::error::Error>> {
        let url = format!("{}/api/v1/packages/{}", self.url, name);

        println!("    {} Fetching from: {}", "→".dimmed(), url);

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

    /// Download package from cloud registry
    ///
    /// API Endpoint: GET /api/v1/packages/{name}/{version}/download
    ///
    /// Returns: .tar.gz package archive
    ///
    /// Steps:
    /// 1. Download tarball from registry
    /// 2. Verify SHA256 checksum
    /// 3. Extract to local packages directory
    /// 4. Update local package cache
    ///
    /// TODO: Implement actual download:
    /// ```rust
    /// let url = format!("{}/api/v1/packages/{}/{}/download", self.url, name, version);
    ///
    /// // Stream download with progress
    /// let mut response = self.client.get(&url).send()?;
    /// let total_size = response.content_length().unwrap_or(0);
    ///
    /// let tarball_path = config.paths.cache.join(format!("{}-{}.tar.gz", name, version));
    /// let mut file = File::create(&tarball_path)?;
    ///
    /// let mut downloaded = 0u64;
    /// let mut buffer = [0; 8192];
    ///
    /// while let Ok(n) = response.read(&mut buffer) {
    ///     if n == 0 { break; }
    ///     file.write_all(&buffer[..n])?;
    ///     downloaded += n as u64;
    ///     // Update progress bar here
    /// }
    ///
    /// // Verify checksum
    /// let computed_hash = sha256_file(&tarball_path)?;
    /// if computed_hash != expected_checksum {
    ///     return Err("Checksum mismatch".into());
    /// }
    ///
    /// // Extract tarball
    /// extract_tarball(&tarball_path, &pkg_dir)?;
    /// ```
    pub fn download_package(
        &self,
        name: &str,
        version: &str,
        config: &Config,
    ) -> Result<PathBuf, Box<dyn std::error::Error>> {
        let url = format!("{}/api/v1/packages/{}/{}/download", self.url, name, version);

        println!("    {} Downloading from: {}", "→".dimmed(), url);

        // Download package
        let mut response = self.client.get(&url).send()?;

        if !response.status().is_success() {
            return Err(format!("Download failed: {}", response.status()).into());
        }

        // Get checksum from header
        let expected_checksum = response
            .headers()
            .get("x-checksum")
            .and_then(|v| v.to_str().ok())
            .map(|s| s.to_string());

        // Create temporary file for download
        let tarball_path = config.paths.cache.join(format!("{}-{}.tar.gz", name, version));
        fs::create_dir_all(&config.paths.cache)?;

        let mut file = File::create(&tarball_path)?;
        let mut hasher = Sha256::new();

        // Download and calculate checksum
        let mut buffer = vec![0; 8192];
        loop {
            let n = response.read(&mut buffer)?;
            if n == 0 { break; }
            file.write_all(&buffer[..n])?;
            hasher.update(&buffer[..n]);
        }

        drop(file);

        // Verify checksum if provided
        if let Some(expected) = expected_checksum {
            let computed = format!("{:x}", hasher.finalize());
            if computed != expected {
                fs::remove_file(&tarball_path)?;
                return Err(format!("Checksum mismatch! Expected: {}, got: {}", expected, computed).into());
            }
            println!("    {} Checksum verified", "✓".green());
        }

        // Extract tarball
        let pkg_dir = config.paths.packages.join(format!("{}-{}", name, version));
        fs::create_dir_all(&pkg_dir)?;

        println!("    {} Extracting to: {}", "→".dimmed(), pkg_dir.display());

        let tar_file = File::open(&tarball_path)?;
        let gz = GzDecoder::new(tar_file);
        let mut archive = Archive::new(gz);
        archive.unpack(&pkg_dir)?;

        // Clean up tarball
        fs::remove_file(&tarball_path)?;

        println!("    {} Package extracted successfully", "✓".green());

        Ok(pkg_dir)
    }

    /// Publish package to cloud registry
    ///
    /// API Endpoint: POST /api/v1/publish
    ///
    /// Headers:
    /// - Authorization: Bearer {token}
    /// - Content-Type: multipart/form-data
    ///
    /// Body:
    /// - package: .tar.gz file
    /// - metadata: JSON manifest
    /// - checksum: SHA256 hash
    pub fn publish_package(
        &self,
        package: &Package,
        config: &Config,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let auth_token = self.auth_token.as_ref()
            .ok_or("Authentication token required for publishing")?;

        println!("    {} Publishing to: {}/api/v1/publish", "→".dimmed(), self.url);
        println!("    {} Package: {} v{}", "→".dimmed(), 
            package.manifest.package.name, package.manifest.package.version);

        // Create tarball
        let tarball_path = self.create_tarball(package, config)?;
        
        // Calculate checksum
        let checksum = self.calculate_checksum(&tarball_path)?;

        // Prepare multipart form
        let author = package.manifest.package.authors.first()
            .map(|s| s.as_str())
            .unwrap_or("Unknown");
        let description = package.manifest.package.description.as_deref().unwrap_or("");
        let license = package.manifest.package.license.as_deref().unwrap_or("MIT");
        let repository = package.manifest.package.repository.as_deref().unwrap_or("");
        
        let form = reqwest::blocking::multipart::Form::new()
            .file("package", &tarball_path)?
            .text("name", package.manifest.package.name.clone())
            .text("version", package.manifest.package.version.clone())
            .text("description", description.to_string())
            .text("author", author.to_string())
            .text("license", license.to_string())
            .text("repository", repository.to_string())
            .text("homepage", repository.to_string())
            .text("checksum", checksum);

        // Upload with authentication
        let response = self.client
            .post(&format!("{}/api/v1/publish", self.url))
            .header("Authorization", format!("Bearer {}", auth_token))
            .multipart(form)
            .send()?;

        // Clean up tarball
        fs::remove_file(&tarball_path)?;

        if response.status().is_success() {
            println!("    {} Package published successfully", "✓".green());
            Ok(())
        } else {
            let error_text = response.text().unwrap_or_else(|_| "Unknown error".to_string());
            Err(format!("Publish failed: {}", error_text).into())
        }
    }

    
    pub fn search(&self, query: &str) -> Result<Vec<SearchResult>, Box<dyn std::error::Error>> {
        let url = format!("{}/api/v1/search?q={}&limit=20", self.url, query);

        println!("    {} Searching at: {}", "→".dimmed(), url);

        let response = self.client.get(&url).send()?;

        if !response.status().is_success() {
            return Err(format!("Search failed: {}", response.status()).into());
        }

        #[derive(Deserialize)]
        struct SearchResponse {
            results: Vec<SearchResult>,
        }

        let search_response: SearchResponse = response.json()?;
        Ok(search_response.results)
    }

    /// Get list of package versions from registry
    ///
    /// API Endpoint: GET /api/v1/packages/{name}/versions
    ///
    /// Returns: Array of version strings
    pub fn get_versions(&self, name: &str) -> Result<Vec<String>, Box<dyn std::error::Error>> {
        let url = format!("{}/api/v1/packages/{}/versions", self.url, name);

        println!("    {} Fetching versions from: {}", "→".dimmed(), url);

        let response = self.client.get(&url).send()?;

        if !response.status().is_success() {
            if response.status() == 404 {
                return Err(format!("Package '{}' not found in registry", name).into());
            }
            return Err(format!("Failed to fetch versions: {}", response.status()).into());
        }

        #[derive(Deserialize)]
        struct VersionsResponse {
            versions: Vec<String>,
        }

        let versions_response: VersionsResponse = response.json()?;
        Ok(versions_response.versions)
    }

    /// Verify package integrity using checksum
    ///
    /// Computes SHA256 hash of downloaded package and compares with registry metadata
    fn verify_checksum(&self, path: &PathBuf, expected: &str) -> Result<bool, Box<dyn std::error::Error>> {
        let mut file = File::open(path)?;
        let mut hasher = Sha256::new();
        std::io::copy(&mut file, &mut hasher)?;
        let computed = format!("{:x}", hasher.finalize());
        
        Ok(computed == expected)
    }

    /// Create a tarball from package directory
    fn create_tarball(&self, package: &Package, config: &Config) -> Result<PathBuf, Box<dyn std::error::Error>> {
        let package_dir = config.paths.packages.join(format!("{}-{}", 
            package.manifest.package.name, package.manifest.package.version));
        
        if !package_dir.exists() {
            return Err(format!("Package directory not found: {}", package_dir.display()).into());
        }

        let tarball_path = config.paths.cache.join(format!("{}-{}.tar.gz", 
            package.manifest.package.name, package.manifest.package.version));

        // Create tarball
        let tar_gz = File::create(&tarball_path)?;
        let enc = flate2::write::GzEncoder::new(tar_gz, flate2::Compression::default());
        let mut tar = tar::Builder::new(enc);

        // Add all files from package directory
        tar.append_dir_all(".", &package_dir)?;
        tar.finish()?;

        Ok(tarball_path)
    }

    /// Calculate SHA256 checksum of a file
    fn calculate_checksum(&self, path: &PathBuf) -> Result<String, Box<dyn std::error::Error>> {
        let mut file = File::open(path)?;
        let mut hasher = Sha256::new();
        std::io::copy(&mut file, &mut hasher)?;
        Ok(format!("{:x}", hasher.finalize()))
    }
}

