use crate::package::Package;
use crate::resolver::Resolver;
use colored::*;
use std::fs;
use std::path::PathBuf;
use std::process::Command;

pub struct Builder {
    package: Package,
}

impl Builder {
    pub fn new(package: Package) -> Self {
        Builder { package }
    }
    
    pub fn build(&self) -> Result<PathBuf, Box<dyn std::error::Error>> {
        // Resolve dependencies first
        println!("  {} Resolving dependencies...", "→".cyan());
        let resolver = Resolver::new(&self.package);
        let deps = resolver.resolve()?;
        
        if deps.is_empty() {
            println!("    {} No dependencies", "✓".green());
        } else {
            println!("    {} {} dependencies", "✓".green(), deps.len());
            for dep in &deps {
                println!("      {} {}", "·".cyan(), dep.name.yellow());
            }
        }
        
        // Create build directory
        let build_dir = self.package.build_dir();
        fs::create_dir_all(&build_dir)?;
        
        // Find Flow compiler
        let flow_compiler = self.find_flow_compiler()?;
        println!("  {} Using compiler: {}", "→".cyan(), flow_compiler.display());
        
        // Get entry point
        let entry_point = self.package.entry_point();
        if !entry_point.exists() {
            return Err(format!("Entry point not found: {}", entry_point.display()).into());
        }
        
        println!("  {} Compiling {}...", "→".cyan(), entry_point.display().to_string().yellow());
        
        // Compile the package
        let output_path = self.package.output_path();
        
        let mut cmd = Command::new(&flow_compiler);
        cmd.arg(&entry_point);
        
        // Add output flag if supported
        cmd.arg("-o");
        cmd.arg(&output_path);
        
        // Add library paths for dependencies
        for dep in &deps {
            if let Some(lib_path) = dep.lib_path.as_ref() {
                cmd.arg("-L");
                cmd.arg(lib_path);
            }
        }
        
        println!("  {} Running: {} {} -o {}", 
            "→".cyan(),
            flow_compiler.display().to_string().cyan(),
            entry_point.display().to_string().yellow(),
            output_path.display().to_string().green());
        
        let status = cmd.status()?;
        
        if !status.success() {
            return Err("Compilation failed".into());
        }
        
        Ok(output_path)
    }
    
    fn find_flow_compiler(&self) -> Result<PathBuf, Box<dyn std::error::Error>> {
        // Try to find flowbase compiler
        
        // 1. Check in ../flowbase/build/flowbase (relative to package)
        let relative = PathBuf::from("../flowbase/build/flowbase");
        if relative.exists() {
            return Ok(relative);
        }
        
        // 2. Check in ../../flowbase/build/flowbase (for nested packages)
        let nested = PathBuf::from("../../flowbase/build/flowbase");
        if nested.exists() {
            return Ok(nested);
        }
        
        // 3. Check in current dir/flowbase/build/flowbase
        let current = PathBuf::from("flowbase/build/flowbase");
        if current.exists() {
            return Ok(current);
        }
        
        // 4. Check in PATH
        if let Ok(path) = which::which("flowbase") {
            return Ok(path);
        }
        
        // 5. Check FLOW_HOME environment variable
        if let Ok(flow_home) = std::env::var("FLOW_HOME") {
            let flow_bin = PathBuf::from(&flow_home).join("build/flowbase");
            if flow_bin.exists() {
                return Ok(flow_bin);
            }
            // Also try bin/flowbase
            let flow_bin = PathBuf::from(&flow_home).join("bin/flowbase");
            if flow_bin.exists() {
                return Ok(flow_bin);
            }
        }
        
        Err("Flow compiler (flowbase) not found. Please set FLOW_HOME or ensure flowbase is in PATH".into())
    }
}

