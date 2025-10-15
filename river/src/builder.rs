use crate::package::Package;
use crate::resolver::Resolver;
use colored::*;
use std::fs;
use std::path::PathBuf;
use std::process::Command;

pub struct Builder {
    package: Package,
}

struct BuildResult {
    output_path: PathBuf,
    is_library: bool,
}

impl Builder {
    pub fn new(package: Package) -> Self {
        Builder { package }
    }
    
    pub fn build(&self) -> Result<PathBuf, Box<dyn std::error::Error>> {
        self.build_internal(false)
    }
    
    fn build_internal(&self, is_dependency: bool) -> Result<PathBuf, Box<dyn std::error::Error>> {
        // If this is a native package, use C compiler
        if self.package.is_native() {
            return self.build_native(is_dependency);
        }
        
        if !is_dependency {
            println!("  {} Resolving dependencies...", "→".cyan());
        }
        let resolver = Resolver::new(&self.package);
        let deps = resolver.resolve()?;
        
        let mut dep_objects = Vec::new();
        
        if deps.is_empty() {
            if !is_dependency {
                println!("    {} No dependencies", "✓".green());
            }
        } else {
            if !is_dependency {
                println!("    {} {} dependencies", "✓".green(), deps.len());
                for dep in &deps {
                    println!("      {} {}", "·".cyan(), dep.name.yellow());
                }
                println!();
            }
            
            for dep in &deps {
                let dep_output = self.build_dependency(&dep)?;
                dep_objects.push(dep_output);
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
        let is_library = self.package.is_library();
        
        let mut cmd = Command::new(&flow_compiler);
        cmd.arg(&entry_point);
        
        // If library, add -c flag for object-only compilation
        if is_library {
            cmd.arg("-c");
        }
        
        // Add output flag if supported
        cmd.arg("-o");
        cmd.arg(&output_path);
        
        // Add dependency object files for linking (only for executables)
        if !is_library {
            for dep_obj in &dep_objects {
                cmd.arg(dep_obj);
            }
        }
        
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
        
        
        let current = PathBuf::from("flowbase/build/flowbase");
        if current.exists() {
            return Ok(current);
        }
        

        if let Ok(path) = which::which("flowbase") {
            return Ok(path);
        }
        
        
        if let Ok(path) = which::which("flow") {
            return Ok(path);
        }
        
        
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
            // Also try build/flow
            let flow_bin = PathBuf::from(&flow_home).join("build/flow");
            if flow_bin.exists() {
                return Ok(flow_bin);
            }
            // Also try bin/flow
            let flow_bin = PathBuf::from(&flow_home).join("bin/flow");
            if flow_bin.exists() {
                return Ok(flow_bin);
            }
        }
        
        Err("Flow compiler (flowbase or flow) not found. Please set FLOW_HOME or ensure flowbase/flow is in PATH".into())
    }
    
    fn build_dependency(&self, dep: &crate::resolver::ResolvedDependency) -> Result<PathBuf, Box<dyn std::error::Error>> {
        println!("  {} Building dependency: {}", "→".cyan(), dep.name.yellow());
        
        let dep_package = Package::load(&dep.path)?;
        let dep_builder = Builder::new(dep_package);
        
        let output_path = dep_builder.build_internal(true)?;
        
        // For libraries, the object file will have .o extension
        let object_path = if output_path.extension().is_none() {
            PathBuf::from(format!("{}.o", output_path.display()))
        } else {
            output_path.clone()
        };
        
        println!("    {} Built: {}", "✓".green(), object_path.display().to_string().cyan());
        
        Ok(object_path)
    }
    
    fn build_native(&self, is_dependency: bool) -> Result<PathBuf, Box<dyn std::error::Error>> {
        if !is_dependency {
            println!("  {} Building native C library...", "→".cyan());
        }
        
        // Create build directory
        let build_dir = self.package.build_dir();
        fs::create_dir_all(&build_dir)?;
        
        // Get entry point (C source file)
        let entry_point = self.package.entry_point();
        if !entry_point.exists() {
            return Err(format!("Native source not found: {}", entry_point.display()).into());
        }
        
        println!("  {} Compiling {}...", "→".cyan(), entry_point.display().to_string().yellow());
        
        // Output will be an object file
        let output_path = self.package.output_path();
        
        // Find C compiler (try cc, gcc, clang)
        let cc = self.find_c_compiler()?;
        println!("  {} Using C compiler: {}", "→".cyan(), cc);
        
        // Compile with -c to create object file, -fPIC for position independent code
        let mut cmd = Command::new(&cc);
        cmd.arg("-c")
           .arg("-fPIC")
           .arg("-O2")
           .arg(&entry_point)
           .arg("-o")
           .arg(&output_path);
        
        println!("  {} Running: {} -c -fPIC -O2 {} -o {}", 
            "→".cyan(),
            cc.cyan(),
            entry_point.display().to_string().yellow(),
            output_path.display().to_string().green());
        
        let status = cmd.status()?;
        
        if !status.success() {
            return Err("Native compilation failed".into());
        }
        
        if !is_dependency {
            println!("    {} Native library compiled successfully", "✓".green());
        }
        
        Ok(output_path)
    }
    
    fn find_c_compiler(&self) -> Result<String, Box<dyn std::error::Error>> {
        // Try to find a C compiler
        
        // 1. Check CC environment variable
        if let Ok(cc) = std::env::var("CC") {
            return Ok(cc);
        }
        
        // 2. Try common compilers
        let compilers = vec!["cc", "gcc", "clang"];
        for compiler in compilers {
            if which::which(compiler).is_ok() {
                return Ok(compiler.to_string());
            }
        }
        
        Err("C compiler not found. Please install gcc, clang, or set CC environment variable".into())
    }
}

