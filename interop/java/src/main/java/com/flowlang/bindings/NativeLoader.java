package com.flowlang.bindings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles loading of the native Flow library
 */
class NativeLoader {
    
    private static boolean loaded = false;
    private static final String LIBRARY_NAME = "flowjni";
    
    /**
     * Load the native Flow library
     * Tries multiple strategies:
     * 1. Load from java.library.path
     * 2. Load from bundled resources
     * 3. Load from ../flowbase/build
     */
    static synchronized void loadNativeLibrary() {
        if (loaded) {
            return;
        }
        
        // Try loading from system library path first
        try {
            System.loadLibrary(LIBRARY_NAME);
            loaded = true;
            System.out.println("Loaded Flow native library from system path");
            return;
        } catch (UnsatisfiedLinkError e) {
            // Continue to next strategy
        }
        
        // Try loading from bundled resources
        try {
            String libraryName = getLibraryName();
            String resourcePath = "/native/" + libraryName;
            
            try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
                if (in != null) {
                    // Extract to temp file
                    Path tempFile = Files.createTempFile("flowjni", getSuffix());
                    tempFile.toFile().deleteOnExit();
                    
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    System.load(tempFile.toAbsolutePath().toString());
                    loaded = true;
                    System.out.println("Loaded Flow native library from resources");
                    return;
                }
            }
        } catch (IOException | UnsatisfiedLinkError e) {
            // Continue to next strategy
        }
        
        // Try loading from development build directory
        try {
            String developmentPath = getDevelopmentLibraryPath();
            if (developmentPath != null && new File(developmentPath).exists()) {
                System.load(developmentPath);
                loaded = true;
                System.out.println("Loaded Flow native library from: " + developmentPath);
                return;
            }
        } catch (UnsatisfiedLinkError e) {
            // Continue
        }
        
        throw new UnsatisfiedLinkError(
            "Could not load Flow native library. " +
            "Please ensure libflowjni is in java.library.path or build it first."
        );
    }
    
    private static String getLibraryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return "libflowjni.dylib";
        } else if (os.contains("linux")) {
            return "libflowjni.so";
        } else if (os.contains("windows")) {
            return "flowjni.dll";
        }
        throw new UnsatisfiedLinkError("Unsupported OS: " + os);
    }
    
    private static String getSuffix() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return ".dylib";
        } else if (os.contains("linux")) {
            return ".so";
        } else if (os.contains("windows")) {
            return ".dll";
        }
        return ".so";
    }
    
    private static String getDevelopmentLibraryPath() {
        // Try to find the library in the development build directory
        // This assumes javabindings is next to flowbase
        String currentDir = System.getProperty("user.dir");
        String libraryName = getLibraryName();
        
        // Try several possible locations
        String[] possiblePaths = {
            currentDir + "/../flowbase/build/" + libraryName,
            currentDir + "/../../flowbase/build/" + libraryName,
            currentDir + "/flowbase/build/" + libraryName,
            currentDir + "/build/" + libraryName
        };
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return file.getCanonicalPath();
                } catch (IOException e) {
                    // Try next path
                }
            }
        }
        
        return null;
    }
}

