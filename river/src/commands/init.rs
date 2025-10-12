use crate::manifest::{Manifest, PackageType};
use colored::*;
use indicatif::{ProgressBar, ProgressStyle};
use std::fs;
use std::path::Path;
use std::thread;
use std::time::Duration;

pub fn execute(name: String, kind: String) -> Result<(), Box<dyn std::error::Error>> {
    // Fancy header
    println!("\n{}", "╔═══════════════════════════════════════╗".cyan().bold());
    println!("{}", "║  River Package Manager                ║".cyan().bold());
    println!("{}", "╚═══════════════════════════════════════╝".cyan().bold());
    println!();
    
    let spinner = ProgressBar::new_spinner();
    spinner.set_style(
        ProgressStyle::default_spinner()
            .template("{spinner:.cyan} {msg}")
            .unwrap()
            .tick_strings(&["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"])
    );
    
    spinner.set_message("Initializing new Flow package...");
    spinner.enable_steady_tick(Duration::from_millis(80));
    thread::sleep(Duration::from_millis(300));
    
    // Determine package type
    let package_type = match kind.as_str() {
        "bin" | "binary" | "executable" => PackageType::Bin,
        "lib" | "library" => PackageType::Lib,
        _ => {
            spinner.finish_and_clear();
            return Err(format!("Invalid package type '{}'. Use 'bin' or 'lib'", kind).into());
        }
    };
    
    // Determine package name and directory
    let (pkg_name, target_dir) = if name == "." {
        let current_dir = std::env::current_dir()?;
        let dir_name = current_dir
            .file_name()
            .and_then(|n| n.to_str())
            .ok_or("Could not determine current directory name")?;
        (dir_name.to_string(), current_dir)
    } else {
        (name.clone(), Path::new(&name).to_path_buf())
    };
    
    // Create directory structure
    spinner.set_message("Creating directory structure...");
    thread::sleep(Duration::from_millis(200));
    
    if name != "." {
        fs::create_dir_all(&target_dir)?;
    }
    
    let src_dir = target_dir.join("src");
    fs::create_dir_all(&src_dir)?;
    
    // Create manifest
    spinner.set_message("Generating River.toml...");
    thread::sleep(Duration::from_millis(200));
    let manifest = Manifest::new(pkg_name.clone(), package_type.clone());
    manifest.save(target_dir.join("River.toml"))?;
    
    spinner.finish_and_clear();
    
    println!("  {} Package type: {}", "✓".green().bold(), 
        match package_type {
            PackageType::Bin => "executable",
            PackageType::Lib => "library",
        }.yellow().bold());
    
    // Create source files with animation
    let file_spinner = ProgressBar::new_spinner();
    file_spinner.set_style(
        ProgressStyle::default_spinner()
            .template("  {spinner:.green} {msg}")
            .unwrap()
            .tick_strings(&["⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"])
    );
    file_spinner.enable_steady_tick(Duration::from_millis(80));
    
    match package_type {
        PackageType::Bin => {
            file_spinner.set_message("Creating src/main.flow...");
            thread::sleep(Duration::from_millis(300));
            let main_flow = src_dir.join("main.flow");
            fs::write(main_flow, create_main_template(&pkg_name))?;
            file_spinner.finish_with_message(format!("{} Created src/main.flow", "✓".green().bold()));
        }
        PackageType::Lib => {
            file_spinner.set_message("Creating src/lib.flow...");
            thread::sleep(Duration::from_millis(300));
            let lib_flow = src_dir.join("lib.flow");
            fs::write(lib_flow, create_lib_template(&pkg_name))?;
            file_spinner.finish_with_message(format!("{} Created src/lib.flow", "✓".green().bold()));
        }
    }
    
    // Create .gitignore
    let git_spinner = ProgressBar::new_spinner();
    git_spinner.set_style(
        ProgressStyle::default_spinner()
            .template("  {spinner:.cyan} {msg}")
            .unwrap()
            .tick_strings(&["◐", "◓", "◑", "◒"])
    );
    git_spinner.enable_steady_tick(Duration::from_millis(100));
    git_spinner.set_message("Creating .gitignore...");
    thread::sleep(Duration::from_millis(200));
    let gitignore = target_dir.join(".gitignore");
    fs::write(gitignore, create_gitignore())?;
    git_spinner.finish_with_message(format!("{} Created .gitignore", "✓".green().bold()));
    
    // Create README
    let readme_spinner = ProgressBar::new_spinner();
    readme_spinner.set_style(
        ProgressStyle::default_spinner()
            .template("  {spinner:.yellow} {msg}")
            .unwrap()
            .tick_strings(&["⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈"])
    );
    readme_spinner.enable_steady_tick(Duration::from_millis(80));
    readme_spinner.set_message("Creating README.md...");
    thread::sleep(Duration::from_millis(200));
    let readme = target_dir.join("README.md");
    fs::write(readme, create_readme(&pkg_name, &package_type))?;
    readme_spinner.finish_with_message(format!("{} Created README.md", "✓".green().bold()));
    
    println!();
    println!("{}", "═".repeat(60).cyan());
    println!();
    println!("{} Package '{}' initialized successfully!", "SUCCESS".green().bold(), pkg_name.cyan().bold());
    println!();
    println!("{}", "Next steps:".yellow().bold());
    if name != "." {
        println!("  {} {}", "→".cyan(), format!("cd {}", name).cyan());
    }
    println!("  {} {}", "→".cyan(), "river build".cyan());
    println!();
    
    Ok(())
}

fn create_main_template(name: &str) -> String {
    format!(r#"// {} - Flow executable
// Generated by River package manager

func main() {{
    print("Hello from {}!");
}}
"#, name, name)
}

fn create_lib_template(name: &str) -> String {
    format!(r#"// {} - Flow library
// Generated by River package manager

/// Example function
func greet(name: string) -> string {{
    return "Hello, " + name + "!";
}}

/// Example function with number
func add(a: int, b: int) -> int {{
    return a + b;
}}
"#, name)
}

fn create_gitignore() -> &'static str {
    r#"/target
/River.lock
.DS_Store
*.o
*.so
*.dylib
*.dll
"#
}

fn create_readme(name: &str, package_type: &PackageType) -> String {
    let kind = match package_type {
        PackageType::Bin => "executable",
        PackageType::Lib => "library",
    };
    
    format!(r#"# {}

A Flow {} package.

## Building

```bash
river build
```

## Installation

```bash
river install
```

## Usage

{}

## License

MIT
"#, name, kind, 
    if *package_type == PackageType::Bin {
        format!("Run the executable:\n```bash\n./{}\n```", name)
    } else {
        "Add as a dependency:\n```toml\n[dependencies]\n".to_string() + name + " = \"0.1.0\"\n```"
    })
}

