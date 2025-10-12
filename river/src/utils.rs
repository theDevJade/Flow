use std::path::Path;

/// Check if a directory is a valid Flow package
pub fn is_package_root<P: AsRef<Path>>(path: P) -> bool {
    path.as_ref().join("River.toml").exists()
}

/// Get the package root directory by searching upward
pub fn find_package_root() -> Option<std::path::PathBuf> {
    let mut current = std::env::current_dir().ok()?;
    
    loop {
        if is_package_root(&current) {
            return Some(current);
        }
        
        if !current.pop() {
            return None;
        }
    }
}

