use crate::manifest::{Manifest, PackageType};
use std::path::{Path, PathBuf};

#[derive(Debug, Clone)]
pub struct Package {
    pub manifest: Manifest,
    pub root_dir: PathBuf,
}

impl Package {
    /// Load a package from a directory
    pub fn load<P: AsRef<Path>>(path: P) -> Result<Self, Box<dyn std::error::Error>> {
        let root_dir = path.as_ref().to_path_buf();
        let manifest_path = root_dir.join("River.toml");
        
        if !manifest_path.exists() {
            return Err("River.toml not found".into());
        }
        
        let manifest = Manifest::load(manifest_path)?;
        
        Ok(Package { manifest, root_dir })
    }
    
    /// Get the source directory
    pub fn src_dir(&self) -> PathBuf {
        self.root_dir.join("src")
    }
    
    /// Get the build directory
    pub fn build_dir(&self) -> PathBuf {
        self.root_dir.join("target")
    }
    
    /// Get the entry point file
    pub fn entry_point(&self) -> PathBuf {
        if let Some(ref entry) = self.manifest.package.entry {
            self.root_dir.join(entry)
        } else {
            match self.manifest.package.package_type {
                PackageType::Bin => self.root_dir.join("src/main.flow"),
                PackageType::Lib => self.root_dir.join("src/lib.flow"),
                PackageType::Native => self.root_dir.join("src/lib.c"),
            }
        }
    }
    
    /// Check if this is an executable package
    pub fn is_binary(&self) -> bool {
        self.manifest.package.package_type == PackageType::Bin
    }
    
    /// Check if this is a library package
    pub fn is_library(&self) -> bool {
        self.manifest.package.package_type == PackageType::Lib
    }
    
    /// Check if this is a native package
    pub fn is_native(&self) -> bool {
        self.manifest.package.package_type == PackageType::Native
    }
    
    /// Get the output binary name
    pub fn binary_name(&self) -> String {
        if self.is_binary() {
            self.manifest.package.name.clone()
        } else if self.is_native() {
            // Native libraries output as .o object files
            format!("lib{}.o", self.manifest.package.name)
        } else {
            format!("lib{}", self.manifest.package.name)
        }
    }
    
    /// Get the output path
    pub fn output_path(&self) -> PathBuf {
        self.build_dir().join(&self.binary_name())
    }
}

