use crate::config::Config;
use crate::manifest::Dependency;
use crate::package::Package;
use std::path::PathBuf;

#[derive(Debug, Clone)]
pub struct ResolvedDependency {
    pub name: String,
    pub version: String,
    pub path: PathBuf,
    pub lib_path: Option<PathBuf>,
}

pub struct Resolver<'a> {
    package: &'a Package,
}

impl<'a> Resolver<'a> {
    pub fn new(package: &'a Package) -> Self {
        Resolver { package }
    }
    
    pub fn resolve(&self) -> Result<Vec<ResolvedDependency>, Box<dyn std::error::Error>> {
        let mut resolved = Vec::new();
        let config = Config::load()?;
        config.ensure_directories()?;
        
        for (name, dep) in &self.package.manifest.dependencies {
            let resolved_dep = self.resolve_dependency(name, dep, &config)?;
            resolved.push(resolved_dep);
        }
        
        Ok(resolved)
    }
    
    fn resolve_dependency(
        &self,
        name: &str,
        dep: &Dependency,
        config: &Config,
    ) -> Result<ResolvedDependency, Box<dyn std::error::Error>> {
        match dep {
            Dependency::Version(version) => {
                // Resolve from registry or local cache
                let pkg_dir = config.paths.packages.join(format!("{}-{}", name, version));
                
                if pkg_dir.exists() {
                    Ok(ResolvedDependency {
                        name: name.to_string(),
                        version: version.clone(),
                        path: pkg_dir.clone(),
                        lib_path: Some(pkg_dir.join("target")),
                    })
                } else {
                    // Package not installed, would need to download from registry
                    Err(format!("Package '{}' version '{}' not found. Run 'river install {}' first", 
                        name, version, name).into())
                }
            }
            Dependency::Path { path } => {
                let dep_path = self.package.root_dir.join(path);
                if !dep_path.exists() {
                    return Err(format!("Path dependency not found: {}", dep_path.display()).into());
                }
                
                Ok(ResolvedDependency {
                    name: name.to_string(),
                    version: "local".to_string(),
                    path: dep_path.clone(),
                    lib_path: Some(dep_path.join("target")),
                })
            }
            Dependency::Git { git, branch, tag, rev } => {
                // Git dependencies would be cloned to cache
                let cache_name = format!("{}-git", name);
                let cache_path = config.paths.cache.join(cache_name);
                
                if cache_path.exists() {
                    Ok(ResolvedDependency {
                        name: name.to_string(),
                        version: format!("git+{}", git),
                        path: cache_path.clone(),
                        lib_path: Some(cache_path.join("target")),
                    })
                } else {
                    Err(format!("Git dependency '{}' not cloned yet", name).into())
                }
            }
            Dependency::Detailed { version, .. } => {
                self.resolve_dependency(name, &Dependency::Version(version.clone()), config)
            }
        }
    }
}

