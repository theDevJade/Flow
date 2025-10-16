"""
Flow Python Package

Python bindings for the Flow programming language.
Allows calling Flow functions from Python code.
"""

__version__ = '0.1.0'

import ctypes
import os
import sys
from typing import Any, Union, List
from pathlib import Path

# ============================================================================
# Find and load the Flow C library
# ============================================================================

def _find_library():
    """Locate the Flow C library (libflow.dylib or libflow.so)."""
    # Platform-specific library names
    if sys.platform == 'darwin':
        lib_name = 'libflow.dylib'
    else:
        lib_name = 'libflow.so'
    
    # Try development path first (relative to this file)
    # This file is at interop/python/flow/__init__.py
    # C library is at interop/c/libflow.dylib
    this_file = Path(__file__).resolve()
    interop_dir = this_file.parent.parent.parent  # Go up to interop/
    c_lib_path = interop_dir / 'c' / lib_name
    
    # Search paths
    search_paths = [
        c_lib_path,                             # Development: interop/c/
        Path('/usr/local/lib') / lib_name,      # System install
        Path('/usr/lib') / lib_name,            # System install
    ]
    
    for path in search_paths:
        if path.exists():
            return str(path)
    
    raise RuntimeError(
        f"Could not find {lib_name}. Tried:\n" +
        "\n".join(f"  - {p}" for p in search_paths) +
        "\n\nPlease build the C library first (see interop/c/README.md)"
    )

# Load the library
_lib_path = _find_library()
_lib = ctypes.CDLL(_lib_path)

# ============================================================================
# C Type Definitions
# ============================================================================

# flow_type_t enum
FLOW_VAL_INT = 0
FLOW_VAL_FLOAT = 1
FLOW_VAL_STRING = 2
FLOW_VAL_BOOL = 3
FLOW_VAL_VOID = 4

# Union for flow_value_t data
class FlowValueData(ctypes.Union):
    _fields_ = [
        ('int_val', ctypes.c_int64),
        ('float_val', ctypes.c_double),
        ('string_val', ctypes.c_char_p),
        ('bool_val', ctypes.c_bool),
    ]

# flow_value_t structure
class FlowValue(ctypes.Structure):
    _fields_ = [
        ('type', ctypes.c_int),
        ('data', FlowValueData),
    ]

# Opaque pointer types
class FlowModuleT(ctypes.Structure):
    pass

# ============================================================================
# C Function Declarations
# ============================================================================

# Runtime management
_lib.flow_init.argtypes = []
_lib.flow_init.restype = ctypes.c_int

_lib.flow_cleanup.argtypes = []
_lib.flow_cleanup.restype = None

# Module management
_lib.flow_load_module.argtypes = [ctypes.c_char_p]
_lib.flow_load_module.restype = ctypes.POINTER(FlowModuleT)

_lib.flow_compile_string.argtypes = [ctypes.c_char_p]
_lib.flow_compile_string.restype = ctypes.POINTER(FlowModuleT)

_lib.flow_unload_module.argtypes = [ctypes.POINTER(FlowModuleT)]
_lib.flow_unload_module.restype = None

# Function calling
_lib.flowc_call_v.argtypes = [
    ctypes.POINTER(FlowModuleT),
    ctypes.c_char_p,
    ctypes.c_int,
    ctypes.POINTER(FlowValue)
]
_lib.flowc_call_v.restype = FlowValue

# Value constructors
_lib.flow_int.argtypes = [ctypes.c_int64]
_lib.flow_int.restype = FlowValue

_lib.flow_float.argtypes = [ctypes.c_double]
_lib.flow_float.restype = FlowValue

_lib.flow_string.argtypes = [ctypes.c_char_p]
_lib.flow_string.restype = FlowValue

_lib.flow_bool.argtypes = [ctypes.c_bool]
_lib.flow_bool.restype = FlowValue

_lib.flow_void.argtypes = []
_lib.flow_void.restype = FlowValue

# Value extractors
_lib.flow_as_int.argtypes = [FlowValue]
_lib.flow_as_int.restype = ctypes.c_int64

_lib.flow_as_float.argtypes = [FlowValue]
_lib.flow_as_float.restype = ctypes.c_double

_lib.flow_as_string.argtypes = [FlowValue]
_lib.flow_as_string.restype = ctypes.c_char_p

_lib.flow_as_bool.argtypes = [FlowValue]
_lib.flow_as_bool.restype = ctypes.c_bool

# Error handling
_lib.flow_get_error.argtypes = []
_lib.flow_get_error.restype = ctypes.c_char_p

_lib.flow_clear_error.argtypes = []
_lib.flow_clear_error.restype = None

# ============================================================================
# REFLECTION API BINDINGS
# ============================================================================

# Define flow_param_info_t structure
class FlowParamInfo(ctypes.Structure):
    _fields_ = [
        ("name", ctypes.c_char_p),
        ("type", ctypes.c_char_p)
    ]

# Define flow_function_info_t structure
class FlowFunctionInfo(ctypes.Structure):
    _fields_ = [
        ("name", ctypes.c_char_p),
        ("return_type", ctypes.c_char_p),
        ("param_count", ctypes.c_int),
        ("params", ctypes.POINTER(FlowParamInfo))
    ]

# Module reflection
_lib.flow_reflect_function_count.restype = ctypes.c_int
_lib.flow_reflect_function_count.argtypes = [ctypes.POINTER(FlowModuleT)]

_lib.flow_reflect_list_functions.restype = ctypes.c_int
_lib.flow_reflect_list_functions.argtypes = [
    ctypes.POINTER(FlowModuleT),
    ctypes.POINTER(ctypes.POINTER(ctypes.c_char_p))
]

_lib.flow_reflect_free_names.restype = None
_lib.flow_reflect_free_names.argtypes = [
    ctypes.POINTER(ctypes.c_char_p),
    ctypes.c_int
]

_lib.flow_reflect_function_name_at.restype = ctypes.c_char_p
_lib.flow_reflect_function_name_at.argtypes = [ctypes.POINTER(FlowModuleT), ctypes.c_int]

# Function reflection
_lib.flow_reflect_get_function_info.restype = ctypes.POINTER(FlowFunctionInfo)
_lib.flow_reflect_get_function_info.argtypes = [ctypes.POINTER(FlowModuleT), ctypes.c_char_p]

_lib.flow_reflect_free_function_info.restype = None
_lib.flow_reflect_free_function_info.argtypes = [ctypes.POINTER(FlowFunctionInfo)]

# Bidirectional reflection (Flow -> Python)
_lib.flow_reflect_register_foreign_module.restype = ctypes.c_int
_lib.flow_reflect_register_foreign_module.argtypes = [
    ctypes.c_char_p,  # adapter_name
    ctypes.c_char_p,  # module_name
    ctypes.POINTER(ctypes.c_char_p),  # function_names
    ctypes.c_int      # count
]

_lib.flow_reflect_has_foreign_module.restype = ctypes.c_int
_lib.flow_reflect_has_foreign_module.argtypes = [ctypes.c_char_p, ctypes.c_char_p]

_lib.flow_reflect_foreign_function_count.restype = ctypes.c_int
_lib.flow_reflect_foreign_function_count.argtypes = [ctypes.c_char_p, ctypes.c_char_p]

_lib.flow_reflect_foreign_functions.restype = ctypes.c_int
_lib.flow_reflect_foreign_functions.argtypes = [
    ctypes.c_char_p,
    ctypes.c_char_p,
    ctypes.POINTER(ctypes.POINTER(ctypes.c_char_p))
]

# ============================================================================
# Python Exception Classes
# ============================================================================

class FlowError(Exception):
    """Base exception for Flow errors."""
    pass

class FlowInitializationError(FlowError):
    """Raised when Flow runtime initialization fails."""
    pass

class FlowCompileError(FlowError):
    """Raised when Flow code compilation fails."""
    pass

class FlowRuntimeError(FlowError):
    """Raised when Flow function execution fails."""
    pass

# ============================================================================
# Type Conversion Helpers
# ============================================================================

def _python_to_flow(value: Any) -> FlowValue:
    """Convert Python value to Flow C value."""
    if isinstance(value, bool):
        return _lib.flow_bool(value)
    elif isinstance(value, int):
        return _lib.flow_int(value)
    elif isinstance(value, float):
        return _lib.flow_float(value)
    elif isinstance(value, str):
        return _lib.flow_string(value.encode('utf-8'))
    elif value is None:
        return _lib.flow_void()
    else:
        raise TypeError(f"Unsupported type for Flow: {type(value)}")

def _flow_to_python(value: FlowValue) -> Any:
    """Convert Flow C value to Python value."""
    if value.type == FLOW_VAL_INT:
        return _lib.flow_as_int(value)
    elif value.type == FLOW_VAL_FLOAT:
        return _lib.flow_as_float(value)
    elif value.type == FLOW_VAL_STRING:
        s = _lib.flow_as_string(value)
        return s.decode('utf-8') if s else None
    elif value.type == FLOW_VAL_BOOL:
        return _lib.flow_as_bool(value)
    elif value.type == FLOW_VAL_VOID:
        return None
    else:
        raise FlowRuntimeError(f"Unknown Flow type: {value.type}")

# ============================================================================
# Runtime Management
# ============================================================================

class Runtime:
    """Flow runtime manager."""
    
    def __init__(self):
        """Initialize the Flow runtime."""
        ret = _lib.flow_init()
        if ret != 0:
            error = _lib.flow_get_error()
            error_msg = error.decode('utf-8') if error else "Unknown error"
            _lib.flow_clear_error()
            raise FlowInitializationError(f"Failed to initialize Flow runtime: {error_msg}")
        
        self._initialized = True
    
    def __del__(self):
        """Cleanup the Flow runtime."""
        if hasattr(self, '_initialized') and self._initialized:
            _lib.flow_cleanup()
    
    @property
    def initialized(self) -> bool:
        """Check if runtime is initialized."""
        return getattr(self, '_initialized', False)

# ============================================================================
# Module Management
# ============================================================================

class Module:
    """Represents a compiled Flow module."""
    
    def __init__(self, handle: ctypes.POINTER(FlowModuleT), path: str, runtime: 'Runtime' = None):
        """
        Initialize a Flow module.
        
        Args:
            handle: C library module handle
            path: Module path (file path or '<compiled>')
            runtime: Optional runtime to keep alive
        """
        self._handle = handle
        self._path = path
        self._runtime = runtime  # Keep runtime alive
    
    @classmethod
    def load(cls, runtime: Runtime, path: str) -> 'Module':
        """
        Load a Flow module from a file.
        
        Args:
            runtime: Flow runtime instance
            path: Path to the .flow file
            
        Returns:
            Module instance
            
        Raises:
            FileNotFoundError: If the file doesn't exist
            FlowCompileError: If compilation fails
        """
        if not os.path.exists(path):
            raise FileNotFoundError(f"File not found: {path}")
        
        handle = _lib.flow_load_module(path.encode('utf-8'))
        if not handle:
            error = _lib.flow_get_error()
            error_msg = error.decode('utf-8') if error else "Unknown error"
            _lib.flow_clear_error()
            raise FlowCompileError(f"Failed to load module: {error_msg}")
        
        return cls(handle, path, runtime)
    
    @classmethod
    def compile(cls, runtime: Runtime, source: str) -> 'Module':
        """
        Compile Flow source code from a string.
        
        Args:
            runtime: Flow runtime instance
            source: Flow source code
            
        Returns:
            Module instance
            
        Raises:
            FlowCompileError: If compilation fails
        """
        handle = _lib.flow_compile_string(source.encode('utf-8'))
        if not handle:
            error = _lib.flow_get_error()
            error_msg = error.decode('utf-8') if error else "Unknown error"
            _lib.flow_clear_error()
            raise FlowCompileError(f"Failed to compile: {error_msg}")
        
        return cls(handle, '<compiled>', runtime)
    
    def call(self, function_name: str, *args: Any) -> Any:
        """
        Call a Flow function.
        
        Args:
            function_name: Name of the function to call
            *args: Arguments to pass to the function
            
        Returns:
            Function return value
            
        Raises:
            FlowRuntimeError: If the function call fails
        """
        # Convert arguments
        c_args = [_python_to_flow(arg) for arg in args]
        
        # Create C array
        if c_args:
            c_array = (FlowValue * len(c_args))(*c_args)
        else:
            c_array = None
        
        # Call the function
        result = _lib.flowc_call_v(
            self._handle,
            function_name.encode('utf-8'),
            len(c_args),
            c_array
        )
        
        # Check for errors
        error = _lib.flow_get_error()
        if error:
            error_msg = error.decode('utf-8')
            if error_msg:
                _lib.flow_clear_error()
                raise FlowRuntimeError(error_msg)
        
        # Convert result
        return _flow_to_python(result)
    
    # ============================================================
    # REFLECTION API
    # ============================================================
    
    def get_function_count(self) -> int:
        """Get the number of functions in this module."""
        if not self._handle:
            raise FlowError("Module is not loaded")
        return _lib.flow_reflect_function_count(self._handle)
    
    def list_functions(self) -> List[str]:
        """Get a list of all function names in this module."""
        if not self._handle:
            raise FlowError("Module is not loaded")
        
        count = _lib.flow_reflect_function_count(self._handle)
        if count == 0:
            return []
        
        # Allocate array of char* for function names
        names_ptr = ctypes.POINTER(ctypes.c_char_p)()
        actual_count = _lib.flow_reflect_list_functions(
            self._handle, 
            ctypes.byref(names_ptr)
        )
        
        if actual_count <= 0:
            return []
        
        # Extract names
        function_names = []
        for i in range(actual_count):
            name = names_ptr[i].decode('utf-8') if names_ptr[i] else ""
            function_names.append(name)
        
        # Free the names array
        _lib.flow_reflect_free_names(names_ptr, actual_count)
        
        return function_names
    
    def get_function_info(self, function_name: str) -> dict:
        """
        Get detailed information about a function.
        
        Args:
            function_name: Name of the function
            
        Returns:
            Dictionary with:
            - name: function name
            - return_type: return type as string
            - parameters: list of dicts with 'name' and 'type'
            
        Raises:
            FlowError: If function is not found
        """
        if not self._handle:
            raise FlowError("Module is not loaded")
        
        func_name_bytes = function_name.encode('utf-8')
        info_ptr = _lib.flow_reflect_get_function_info(self._handle, func_name_bytes)
        
        if not info_ptr:
            raise FlowError(f"Function '{function_name}' not found in module")
        
        try:
            # Extract function info
            info = info_ptr.contents
            result = {
                'name': info.name.decode('utf-8'),
                'return_type': info.return_type.decode('utf-8'),
                'parameters': []
            }
            
            # Extract parameters
            for i in range(info.param_count):
                param = info.params[i]
                result['parameters'].append({
                    'name': param.name.decode('utf-8'),
                    'type': param.type.decode('utf-8')
                })
            
            return result
        finally:
            # Free the function info
            _lib.flow_reflect_free_function_info(info_ptr)
    
    def inspect(self) -> str:
        """
        Get a human-readable string representation of all functions in the module.
        Similar to Python's help() function.
        """
        functions = self.list_functions()
        if not functions:
            return "Module contains no functions"
        
        lines = [f"Module contains {len(functions)} function(s):\n"]
        
        for func_name in functions:
            try:
                info = self.get_function_info(func_name)
                params_str = ", ".join(
                    f"{p['name']}: {p['type']}" for p in info['parameters']
                )
                lines.append(
                    f"  {info['name']}({params_str}) -> {info['return_type']}"
                )
            except Exception as e:
                lines.append(f"  {func_name} (error getting info: {e})")
        
        return "\n".join(lines)
    
    def __getattr__(self, name: str) -> Any:
        """Allow calling Flow functions as methods."""
        if name.startswith('_'):
            raise AttributeError(f"'{type(self).__name__}' object has no attribute '{name}'")
        
        def flow_function(*args):
            return self.call(name, *args)
        
        return flow_function

    def __del__(self):
        """Cleanup the module."""
        if hasattr(self, '_handle') and self._handle:
            _lib.flow_unload_module(self._handle)
    
    @property
    def path(self) -> str:
        """Get the module path."""
        return self._path

# ============================================================================
# Convenience Functions
# ============================================================================

def load_module(path: str) -> Module:
    """
    Load a Flow module from a file (convenience function).
    
    Args:
        path: Path to the .flow file
        
    Returns:
        Module instance
    """
    runtime = Runtime()
    return Module.load(runtime, path)

def compile(source: str) -> Module:
    """
    Compile Flow source code (convenience function).
    
    Args:
        source: Flow source code
        
    Returns:
        Module instance
    """
    runtime = Runtime()
    return Module.compile(runtime, source)

def version() -> str:
    """Return the Flow Python bindings version."""
    return __version__

# ============================================================================
# Exports
# ============================================================================

__all__ = [
    'Runtime',
    'Module',
    'load_module',
    'compile',
    'version',
    'FlowError',
    'FlowInitializationError',
    'FlowCompileError',
    'FlowRuntimeError',
]
