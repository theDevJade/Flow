use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs;
use std::path::Path;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Manifest {
    pub package: Package,
    #[serde(default)]
    pub dependencies: HashMap<String, Dependency>,
    #[serde(default)]
    pub dev_dependencies: HashMap<String, Dependency>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Package {
    pub name: String,
    pub version: String,
    pub authors: Vec<String>,
    #[serde(default)]
    pub description: Option<String>,
    #[serde(default)]
    pub license: Option<String>,
    #[serde(default)]
    pub repository: Option<String>,
    #[serde(rename = "type")]
    pub package_type: PackageType,
    #[serde(default)]
    pub entry: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum PackageType {

    Bin,

    Lib,
}

impl Default for PackageType {
    fn default() -> Self {
        PackageType::Bin
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum Dependency {
    /// Simple version string
    Version(String),
    /// Detailed dependency specification
    Detailed {
        version: String,
        #[serde(default)]
        optional: bool,
        #[serde(default)]
        features: Vec<String>,
    },
    /// Git repository
    Git {
        git: String,
        #[serde(default)]
        branch: Option<String>,
        #[serde(default)]
        tag: Option<String>,
        #[serde(default)]
        rev: Option<String>,
    },
    /// Local path
    Path {
        path: String,
    },
}

impl Manifest {
    /// Load manifest from River.toml file
    pub fn load<P: AsRef<Path>>(path: P) -> Result<Self, Box<dyn std::error::Error>> {
        let content = fs::read_to_string(path)?;
        let manifest: Manifest = toml::from_str(&content)?;
        Ok(manifest)
    }
    
    /// Save manifest to River.toml file
    pub fn save<P: AsRef<Path>>(&self, path: P) -> Result<(), Box<dyn std::error::Error>> {
        let content = toml::to_string_pretty(self)?;
        fs::write(path, content)?;
        Ok(())
    }
    
    /// Create a new manifest
    pub fn new(name: String, package_type: PackageType) -> Self {
        let entry = match package_type {
            PackageType::Bin => Some("src/main.flow".to_string()),
            PackageType::Lib => Some("src/lib.flow".to_string()),
        };
        
        Manifest {
            package: Package {
                name,
                version: "0.1.0".to_string(),
                authors: vec!["Your Name <you@example.com>".to_string()],
                description: None,
                license: Some("MIT".to_string()),
                repository: None,
                package_type,
                entry,
            },
            dependencies: HashMap::new(),
            dev_dependencies: HashMap::new(),
        }
    }
    
    /// Add a dependency
    pub fn add_dependency(&mut self, name: String, version: String) {
        self.dependencies.insert(name, Dependency::Version(version));
    }
    
    /// Remove a dependency
    pub fn remove_dependency(&mut self, name: &str) -> bool {
        self.dependencies.remove(name).is_some()
    }
    
    /// Get all dependencies
    pub fn all_dependencies(&self) -> Vec<(String, &Dependency)> {
        self.dependencies
            .iter()
            .map(|(k, v)| (k.clone(), v))
            .collect()
    }
}

