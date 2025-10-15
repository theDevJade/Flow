use crate::builder::Builder;
use crate::package::Package;
use colored::*;
use indicatif::{ProgressBar, ProgressStyle};
use std::thread;
use std::time::Duration;

pub fn execute() -> Result<(), Box<dyn std::error::Error>> {
    // Fancy header
    println!("\n{}", "╔═══════════════════════════════════════╗".cyan().bold());
    println!("{}", "║  Building Flow Package                ║".cyan().bold());
    println!("{}", "╚═══════════════════════════════════════╝".cyan().bold());
    println!();
    
    // Load package
    let spinner = ProgressBar::new_spinner();
    spinner.set_style(
        ProgressStyle::default_spinner()
            .template("{spinner:.cyan} {msg}")
            .unwrap()
            .tick_strings(&["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"])
    );
    spinner.set_message("Loading package manifest...");
    spinner.enable_steady_tick(Duration::from_millis(80));
    thread::sleep(Duration::from_millis(300));
    
    let package = Package::load(".")?;
    spinner.finish_and_clear();
    
    println!("  {} Package: {} v{}", 
        "→".cyan(),
        package.manifest.package.name.yellow().bold(),
        package.manifest.package.version.cyan());
    
    println!("  {} Type: {}", 
        "→".cyan(),
        if package.is_binary() { 
            "executable".green().bold() 
        } else if package.is_native() {
            "native C library".magenta().bold()
        } else { 
            "library".blue().bold() 
        });
    println!();
    
    // Build package with progress bar
    let build_spinner = ProgressBar::new_spinner();
    build_spinner.set_style(
        ProgressStyle::default_spinner()
            .template("{spinner:.green} {msg}")
            .unwrap()
            .tick_strings(&["▹▹▹▹▹", "▸▹▹▹▹", "▹▸▹▹▹", "▹▹▸▹▹", "▹▹▹▸▹", "▹▹▹▹▸"])
    );
    build_spinner.set_message("Building package...");
    build_spinner.enable_steady_tick(Duration::from_millis(120));
    
    let builder = Builder::new(package);
    let output_path = builder.build()?;
    
    build_spinner.finish_and_clear();
    
    println!();
    println!("{}", "═".repeat(60).green());
    println!();
    println!("{} Build completed successfully!", "SUCCESS".green().bold());
    println!();
    println!("  {} {}", "Output:".yellow().bold(), output_path.display().to_string().cyan().bold());
    println!();
    
    Ok(())
}

