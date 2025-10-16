<?php

namespace Flow;

use FFI;
use Exception;

/**
 * Flow Language - PHP Bindings
 * 
 * Allows calling Flow functions from PHP code using FFI.
 */
class Flow
{
    const VERSION = '0.1.0';
    
    private static ?FFI $ffi = null;
    private static bool $initialized = false;

    /**
     * Initialize Flow FFI bindings
     */
    private static function init(): void
    {
        if (self::$initialized) {
            return;
        }

        // Find the library
        $libPath = self::findLibrary();
        
        // Load FFI definitions
        $cdef = <<<'CDEF'
        // Flow value types
        typedef enum {
            FLOW_VAL_INT = 0,
            FLOW_VAL_FLOAT = 1,
            FLOW_VAL_STRING = 2,
            FLOW_VAL_BOOL = 3,
            FLOW_VAL_VOID = 4
        } flow_type_t;

        // Flow value structure
        typedef struct {
            int type;
            union {
                int64_t int_val;
                double float_val;
                const char* string_val;
                bool bool_val;
            } data;
        } flow_value_t;

        // Opaque types
        typedef struct flow_module flow_module_t;

        // Runtime management
        int flow_init(void);
        void flow_cleanup(void);

        // Module management
        flow_module_t* flow_load_module(const char* path);
        flow_module_t* flow_compile_string(const char* source);
        void flow_unload_module(flow_module_t* module);

        // Function calling
        flow_value_t flowc_call_v(flow_module_t* module, const char* function, int argc, flow_value_t* argv);

        // Value constructors
        flow_value_t flow_int(int64_t value);
        flow_value_t flow_float(double value);
        flow_value_t flow_string(const char* value);
        flow_value_t flow_bool(bool value);
        flow_value_t flow_void(void);

        // Value extractors
        int64_t flow_as_int(flow_value_t value);
        double flow_as_float(flow_value_t value);
        const char* flow_as_string(flow_value_t value);
        bool flow_as_bool(flow_value_t value);

        // Error handling
        const char* flow_get_error(void);
        void flow_clear_error(void);

        // Reflection API
        typedef struct {
            const char* name;
            const char* type;
        } flow_param_info_t;

        typedef struct {
            const char* name;
            const char* return_type;
            int param_count;
            flow_param_info_t* params;
        } flow_function_info_t;

        int flow_reflect_function_count(flow_module_t* module);
        int flow_reflect_list_functions(flow_module_t* module, char*** names_out);
        void flow_reflect_free_names(char** names, int count);
        const char* flow_reflect_function_name_at(flow_module_t* module, int index);
        flow_function_info_t* flow_reflect_get_function_info(flow_module_t* module, const char* function_name);
        void flow_reflect_free_function_info(flow_function_info_t* info);
        
        // Bidirectional reflection
        int flow_reflect_register_foreign_module(const char* adapter, const char* module_name, const char** functions, int count);
        int flow_reflect_has_foreign_module(const char* adapter, const char* module_name);
        int flow_reflect_foreign_function_count(const char* adapter, const char* module_name);
        int flow_reflect_foreign_functions(const char* adapter, const char* module_name, char*** names_out);
        CDEF;

        try {
            self::$ffi = FFI::cdef($cdef, $libPath);
        } catch (\FFI\Exception $e) {
            throw new FlowException("Failed to load Flow library: " . $e->getMessage());
        }

        // Initialize runtime
        $ret = self::$ffi->flow_init();
        if ($ret !== 0) {
            $error = self::$ffi->flow_get_error();
            $errorMsg = $error ? FFI::string($error) : "Unknown error";
            self::$ffi->flow_clear_error();
            throw new FlowInitializationException("Failed to initialize Flow runtime: $errorMsg");
        }

        self::$initialized = true;
    }

    /**
     * Find the Flow C library
     */
    private static function findLibrary(): string
    {
        $libName = PHP_OS_FAMILY === 'Darwin' ? 'libflow.dylib' : 'libflow.so';
        
        // Calculate path relative to this file
        // This file is at interop/php/src/Flow.php
        // C library is at interop/c/libflow.dylib
        $thisFile = realpath(__FILE__);
        $interopDir = dirname(dirname(dirname($thisFile)));
        $cLibPath = $interopDir . '/c/' . $libName;
        
        $searchPaths = [
            $cLibPath,                      // Development: interop/c/
            '/usr/local/lib/' . $libName,   // System install
            '/usr/lib/' . $libName,         // System install
        ];

        foreach ($searchPaths as $path) {
            if (file_exists($path)) {
                return $path;
            }
        }

        throw new FlowException(
            "Could not find $libName. Tried:\n" .
            implode("\n", array_map(fn($p) => "  - $p", $searchPaths)) .
            "\n\nPlease build the C library first (see interop/c/README.md)"
        );
    }

    /**
     * Get FFI instance
     */
    public static function getFFI(): FFI
    {
        if (!self::$initialized) {
            self::init();
        }
        return self::$ffi;
    }

    /**
     * Load a Flow module from a file
     * 
     * @param string $path Path to the .flow file
     * @return FlowModule Module with callable functions
     * 
     * @throws FlowCompileException If compilation fails
     */
    public static function loadModule(string $path): FlowModule
    {
        if (!file_exists($path)) {
            throw new \InvalidArgumentException("File not found: $path");
        }

        self::init();
        $runtime = new Runtime();
        
        $handle = self::$ffi->flow_load_module($path);
        if ($handle === null || FFI::isNull($handle)) {
            $error = self::$ffi->flow_get_error();
            $errorMsg = $error ? FFI::string($error) : "Unknown error";
            self::$ffi->flow_clear_error();
            throw new FlowCompileException("Failed to load module: $errorMsg");
        }

        return new FlowModule($handle, $path, $runtime);
    }

    /**
     * Compile Flow source code from a string
     * 
     * @param string $source Flow source code
     * @return FlowModule Module with callable functions
     * 
     * @throws FlowCompileException If compilation fails
     */
    public static function compile(string $source): FlowModule
    {
        self::init();
        $runtime = new Runtime();
        
        $handle = self::$ffi->flow_compile_string($source);
        if ($handle === null || FFI::isNull($handle)) {
            $error = self::$ffi->flow_get_error();
            $errorMsg = $error ? FFI::string($error) : "Unknown error";
            self::$ffi->flow_clear_error();
            throw new FlowCompileException("Failed to compile: $errorMsg");
        }

        return new FlowModule($handle, '<compiled>', $runtime);
    }

    /**
     * Get Flow language version
     * 
     * @return string Version string
     */
    public static function version(): string
    {
        return self::VERSION;
    }

    /**
     * Cleanup on shutdown
     */
    public static function cleanup(): void
    {
        if (self::$initialized && self::$ffi !== null) {
            self::$ffi->flow_cleanup();
            self::$initialized = false;
        }
    }
}

// Register shutdown function
register_shutdown_function([Flow::class, 'cleanup']);

/**
 * Flow runtime manager
 */
class Runtime
{
    private bool $initialized = true;

    public function __construct()
    {
        // Runtime is already initialized by Flow::init()
    }

    public function isInitialized(): bool
    {
        return $this->initialized;
    }
}

/**
 * Represents a compiled Flow module
 */
class FlowModule
{
    private $handle;  // FFI\CData
    private string $path;
    private Runtime $runtime;

    public function __construct($handle, string $path, Runtime $runtime)
    {
        $this->handle = $handle;
        $this->path = $path;
        $this->runtime = $runtime;
    }

    /**
     * Call a Flow function
     * 
     * @param string $name Function name
     * @param array $arguments Function arguments
     * @return mixed Return value
     * 
     * @throws FlowRuntimeException If function call fails
     */
    public function call(string $name, ...$arguments)
    {
        $ffi = Flow::getFFI();
        
        // Convert PHP values to Flow values
        $flowArgs = [];
        foreach ($arguments as $arg) {
            $flowArgs[] = self::phpToFlow($arg);
        }

        // Create C array
        if (!empty($flowArgs)) {
            $cArray = $ffi->new('flow_value_t[' . count($flowArgs) . ']');
            foreach ($flowArgs as $i => $value) {
                $cArray[$i] = $value;
            }
        } else {
            $cArray = null;
        }

        // Call the function
        $result = $ffi->flowc_call_v($this->handle, $name, count($flowArgs), $cArray);

        // Check for errors
        $error = $ffi->flow_get_error();
        if ($error !== null && $error !== '') {
            $ffi->flow_clear_error();
            throw new FlowRuntimeException($error);
        }

        // Convert result back to PHP
        return self::flowToPhp($result);
    }

    /**
     * Magic method to call Flow functions as methods
     */
    public function __call(string $name, array $arguments)
    {
        return $this->call($name, ...$arguments);
    }

    /**
     * Convert PHP value to Flow value
     */
    private static function phpToFlow($value)
    {
        $ffi = Flow::getFFI();

        if (is_bool($value)) {
            return $ffi->flow_bool($value);
        } elseif (is_int($value)) {
            return $ffi->flow_int($value);
        } elseif (is_float($value)) {
            return $ffi->flow_float($value);
        } elseif (is_string($value)) {
            return $ffi->flow_string($value);
        } elseif ($value === null) {
            return $ffi->flow_void();
        } else {
            throw new \InvalidArgumentException("Unsupported type for Flow: " . gettype($value));
        }
    }

    /**
     * Convert Flow value to PHP value
     */
    private static function flowToPhp($value)
    {
        $ffi = Flow::getFFI();
        
        $type = $value->type;
        
        if ($type === 0) { // FLOW_VAL_INT
            return $ffi->flow_as_int($value);
        } elseif ($type === 1) { // FLOW_VAL_FLOAT
            return $ffi->flow_as_float($value);
        } elseif ($type === 2) { // FLOW_VAL_STRING
            $str = $ffi->flow_as_string($value);
            return $str ? FFI::string($str) : null;
        } elseif ($type === 3) { // FLOW_VAL_BOOL
            return $ffi->flow_as_bool($value);
        } elseif ($type === 4) { // FLOW_VAL_VOID
            return null;
        } else {
            throw new FlowRuntimeException("Unknown Flow type: $type");
        }
    }

    public function getPath(): string
    {
        return $this->path;
    }

    // ========================================================================
    // REFLECTION API
    // ========================================================================

    /**
     * Get the number of functions in this module
     * @return int Number of functions
     */
    public function functionCount(): int
    {
        $ffi = Flow::getFFI();
        return $ffi->flow_reflect_function_count($this->handle);
    }

    /**
     * List all function names in this module
     * @return array Array of function names
     */
    public function listFunctions(): array
    {
        $ffi = Flow::getFFI();
        $count = $ffi->flow_reflect_function_count($this->handle);
        
        if ($count === 0) {
            return [];
        }

        // Use simpler approach: get function names one at a time
        $functionNames = [];
        for ($i = 0; $i < $count; $i++) {
            $name = $ffi->flow_reflect_function_name_at($this->handle, $i);
            if ($name !== null && $name !== '') {
                // PHP FFI automatically converts const char* to string
                $functionNames[] = is_string($name) ? $name : FFI::string($name);
            }
        }

        return $functionNames;
    }

    /**
     * Get detailed information about a function
     * @param string $functionName Name of the function
     * @return array Function info with name, return_type, and parameters
     * @throws FlowException If function not found
     */
    public function functionInfo(string $functionName): array
    {
        $ffi = Flow::getFFI();
        $infoPtr = $ffi->flow_reflect_get_function_info($this->handle, $functionName);
        
        if ($infoPtr === null || FFI::isNull($infoPtr)) {
            throw new FlowException("Function '$functionName' not found in module");
        }

        try {
            $info = $infoPtr;
            
            // PHP FFI automatically converts const char* to string
            $result = [
                'name' => is_string($info->name) ? $info->name : FFI::string($info->name),
                'return_type' => is_string($info->return_type) ? $info->return_type : FFI::string($info->return_type),
                'parameters' => []
            ];

            // Extract parameters
            if ($info->param_count > 0 && $info->params !== null && !FFI::isNull($info->params)) {
                for ($i = 0; $i < $info->param_count; $i++) {
                    $param = $info->params[$i];
                    $result['parameters'][] = [
                        'name' => is_string($param->name) ? $param->name : FFI::string($param->name),
                        'type' => is_string($param->type) ? $param->type : FFI::string($param->type)
                    ];
                }
            }

            return $result;
        } finally {
            // Free the function info
            $ffi->flow_reflect_free_function_info($infoPtr);
        }
    }

    /**
     * Get a human-readable string representation of all functions in the module
     * @return string Formatted string with function signatures
     */
    public function inspect(): string
    {
        $functions = $this->listFunctions();
        
        if (empty($functions)) {
            return 'Module contains no functions';
        }

        $lines = ["Module contains " . count($functions) . " function(s):\n"];
        
        foreach ($functions as $funcName) {
            try {
                $info = $this->functionInfo($funcName);
                $params = array_map(
                    fn($p) => "{$p['name']}: {$p['type']}",
                    $info['parameters']
                );
                $lines[] = "  {$info['name']}(" . implode(', ', $params) . ") -> {$info['return_type']}";
            } catch (FlowException $e) {
                $lines[] = "  $funcName (error getting info: {$e->getMessage()})";
            }
        }

        return implode("\n", $lines);
    }

    public function __destruct()
    {
        if ($this->handle !== null) {
            $ffi = Flow::getFFI();
            $ffi->flow_unload_module($this->handle);
        }
    }
}

// Exception classes

class FlowException extends Exception {}
class FlowInitializationException extends FlowException {}
class FlowCompileException extends FlowException {}
class FlowRuntimeException extends FlowException {}
