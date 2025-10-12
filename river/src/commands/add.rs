use crate::manifest::Manifest;
use colored::*;
use indicatif::{ProgressBar, ProgressStyle};
use std::thread;
use std::time::Duration;

pub fn execute(package: String, version: Option<String>) -> Result<(), Box<dyn std::error::Error>> {
    println!();
    let spinner = ProgressBar::new_spinner();
    spinner.set_style(
        ProgressStyle::default_spinner()
            .template("{spinner:.cyan} {msg}")
            .unwrap()
            .tick_strings(&["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"])
    );
    spinner.set_message(format!("Adding dependency '{}'...", package.yellow()));
    spinner.enable_steady_tick(Duration::from_millis(80));
    thread::sleep(Duration::from_millis(400));
    
    // Load manifest
    let mut manifest = Manifest::load("River.toml")?;
    
    // Determine version
    let dep_version = version.unwrap_or_else(|| "*".to_string());
    
    // Add dependency
    manifest.add_dependency(package.clone(), dep_version.clone());
    
    // Save manifest
    spinner.set_message("Updating River.toml...");
    thread::sleep(Duration::from_millis(300));
    manifest.save("River.toml")?;
    
    spinner.finish_and_clear();
    
    println!("{} Added {} @ {}", 
        "✓".green().bold(),
        package.yellow().bold(),
        dep_version.cyan().bold());
    
    println!();
    println!("{} Run {} to fetch the dependency.", 
        "→".cyan(),
        "river install".cyan().bold());
    println!();
    
    Ok(())
}

