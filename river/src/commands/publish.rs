use crate::config::Config;
use crate::package::Package;
use crate::registry::Registry;
use colored::*;

pub fn execute() -> Result<(), Box<dyn std::error::Error>> {
    println!("{} Publishing package...", "→".cyan().bold());
    
    // Load package
    let package = Package::load(".")?;
    
    println!("  {} Package: {} v{}", 
        "→".cyan(),
        package.manifest.package.name.yellow(),
        package.manifest.package.version.cyan());
    
    // Load config
    let config = Config::load()?;
    
    // Check for auth token
    if config.registry.token.is_none() {
        return Err("No authentication token found. Run 'river login' first.".into());
    }
    
    // Connect to registry
    let registry = Registry::new(config.registry.url.clone());
    
    // Publish package
    println!("  {} Uploading to registry...", "→".cyan());
    registry.publish_package(&package, &config)?;
    
    println!("{} Published successfully!", "✓".green().bold());
    
    Ok(())
}

