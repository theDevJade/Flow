use crate::builder::Builder;
use crate::package::Package;
use colored::*;
use std::process::Command;

pub fn execute(args: Vec<String>) -> Result<(), Box<dyn std::error::Error>> {
    println!("\n{}", "╔═══════════════════════════════════════╗".cyan().bold());
    println!("{}", "║  Building & Running Flow Package     ║".cyan().bold());
    println!("{}", "╚═══════════════════════════════════════╝".cyan().bold());
    println!();
    
    let package = Package::load(".")?;
    
    if !package.is_binary() {
        return Err("Cannot run a library package. Only binary packages can be executed.".into());
    }
    
    println!("  {} Package: {} v{}", 
        "→".cyan(),
        package.manifest.package.name.yellow().bold(),
        package.manifest.package.version.cyan());
    println!();
    
    println!("  {} Building...", "→".cyan());
    let builder = Builder::new(package);
    let output_path = builder.build()?;
    
    println!();
    println!("{}", "═".repeat(60).green());
    println!();
    println!("{} Running: {}", "→".cyan().bold(), output_path.display().to_string().yellow());
    println!();
    println!("{}", "═".repeat(60).cyan());
    println!();
    
    let mut cmd = Command::new(&output_path);
    cmd.args(&args);
    
    let status = cmd.status()?;
    
    println!();
    println!("{}", "═".repeat(60).cyan());
    println!();
    
    if status.success() {
        println!("{} Program exited successfully", "✓".green().bold());
    } else {
        println!("{} Program exited with code: {}", 
            "✗".red().bold(), 
            status.code().unwrap_or(-1));
        std::process::exit(status.code().unwrap_or(1));
    }
    
    Ok(())
}

