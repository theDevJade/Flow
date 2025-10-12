use colored::*;
use indicatif::{ProgressBar, ProgressStyle};
use std::fs;
use std::thread;
use std::time::Duration;

pub fn execute() -> Result<(), Box<dyn std::error::Error>> {
    println!();
    
    let target_dir = "target";
    
    if !std::path::Path::new(target_dir).exists() {
        println!("{} No target directory found", "INFO".yellow().bold());
        println!();
        return Ok(());
    }
    
    // Animated cleaning
    let spinner = ProgressBar::new_spinner();
    spinner.set_style(
        ProgressStyle::default_spinner()
            .template("{spinner:.red} {msg}")
            .unwrap()
            .tick_strings(&["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"])
    );
    spinner.set_message("Cleaning build artifacts...");
    spinner.enable_steady_tick(Duration::from_millis(80));
    thread::sleep(Duration::from_millis(800));
    
    // Remove target directory
    fs::remove_dir_all(target_dir)?;
    
    spinner.finish_and_clear();
    
    println!("{} Cleaned successfully!", "✓".green().bold());
    println!();
    
    Ok(())
}

