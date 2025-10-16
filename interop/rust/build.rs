fn main() {
    // Tell Cargo where to find libflow
    let lib_path = std::path::Path::new("../c");
    println!("cargo:rustc-link-search=native={}", lib_path.display());
    println!("cargo:rustc-link-lib=dylib=flow");
    
    // Set rpath for macOS
    #[cfg(target_os = "macos")]
    {
        println!("cargo:rustc-link-arg=-Wl,-rpath,@loader_path/../../../c");
    }
    
    // Tell Cargo to re-run if the C library changes
    println!("cargo:rerun-if-changed=../c/libflow.dylib");
    println!("cargo:rerun-if-changed=../c/libflow.so");
}
