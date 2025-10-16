# frozen_string_literal: true

# Flow Language - Ruby Bindings
#
# Ruby bindings for calling Flow functions from Ruby code.
#
# @example
#   require 'flow'
#
#   runtime = Flow::Runtime.new
#   mod = Flow::Module.compile(runtime, <<~FLOW)
#     func add(a: int, b: int) -> int {
#       return a + b;
#     }
#   FLOW
#
#   result = mod.call('add', 10, 20)
#   puts "Result: #{result}"  # => 30

require 'ffi'
require 'tempfile'

module Flow
  VERSION = '0.1.0'

  # FFI bindings to the C library
  module LibFlow
    extend FFI::Library
    
    # Find the library - try multiple locations
    lib_paths = [
      File.expand_path('../../c/libflow.dylib', __dir__),  # macOS dev
      File.expand_path('../../c/libflow.so', __dir__),      # Linux dev
      'libflow.dylib',                                       # macOS installed
      'libflow.so',                                          # Linux installed
      '/usr/local/lib/libflow.dylib',
      '/usr/local/lib/libflow.so'
    ]
    
    lib_path = lib_paths.find { |path| File.exist?(path) }
    raise "Could not find libflow library. Tried: #{lib_paths.join(', ')}" unless lib_path
    
    ffi_lib lib_path
    
    # Value types enum
    enum :flow_type, [
      :int, 0,
      :float, 1,
      :string, 2,
      :bool, 3,
      :void, 4
    ]
    
    # Value union
    class FlowValueData < FFI::Union
      layout :int_val, :int64,
             :float_val, :double,
             :string_val, :pointer,
             :bool_val, :bool
    end
    
    # Value struct
    class FlowValueT < FFI::Struct
      layout :type, :flow_type,
             :data, FlowValueData
    end
    
    # Runtime functions
    attach_function :flow_init, [], :int
    attach_function :flow_cleanup, [], :void
    
    # Module functions
    attach_function :flow_load_module, [:string], :pointer
    attach_function :flow_compile_string, [:string], :pointer
    attach_function :flow_unload_module, [:pointer], :void
    
    # Function calls
    attach_function :flowc_call_v, [:pointer, :string, :int, :pointer], FlowValueT.by_value
    
    # Value constructors
    attach_function :flow_int, [:int64], FlowValueT.by_value
    attach_function :flow_float, [:double], FlowValueT.by_value
    attach_function :flow_string, [:string], FlowValueT.by_value
    attach_function :flow_bool, [:bool], FlowValueT.by_value
    attach_function :flow_void, [], FlowValueT.by_value
    
    # Value extractors
    attach_function :flow_as_int, [FlowValueT.by_value], :int64
    attach_function :flow_as_float, [FlowValueT.by_value], :double
    attach_function :flow_as_string, [FlowValueT.by_value], :string
    attach_function :flow_as_bool, [FlowValueT.by_value], :bool
    
    # Error handling
    attach_function :flow_get_error, [], :string
    attach_function :flow_clear_error, [], :void
    
    # ============================================================
    # REFLECTION API
    # ============================================================
    
    # Parameter info structure
    class FlowParamInfo < FFI::Struct
      layout :name, :pointer,
             :type, :pointer
    end
    
    # Function info structure
    class FlowFunctionInfo < FFI::Struct
      layout :name, :pointer,
             :return_type, :pointer,
             :param_count, :int,
             :params, :pointer  # FlowParamInfo*
    end
    
    # Module reflection
    attach_function :flow_reflect_function_count, [:pointer], :int
    attach_function :flow_reflect_list_functions, [:pointer, :pointer], :int
    attach_function :flow_reflect_free_names, [:pointer, :int], :void
    attach_function :flow_reflect_function_name_at, [:pointer, :int], :string
    
    # Function reflection
    attach_function :flow_reflect_get_function_info, [:pointer, :string], :pointer
    attach_function :flow_reflect_free_function_info, [:pointer], :void
    
    # Bidirectional reflection
    attach_function :flow_reflect_register_foreign_module, [:string, :string, :pointer, :int], :int
    attach_function :flow_reflect_has_foreign_module, [:string, :string], :int
    attach_function :flow_reflect_foreign_function_count, [:string, :string], :int
    attach_function :flow_reflect_foreign_functions, [:string, :string, :pointer], :int
  end

  # Flow runtime error
  class FlowError < StandardError; end
  class CompileError < FlowError; end
  class RuntimeError < FlowError; end
  class InitializationError < FlowError; end

  # Flow runtime manager
  class Runtime
    def initialize
      ret = LibFlow.flow_init
      if ret != 0
        error = LibFlow.flow_get_error
        raise InitializationError, "Failed to initialize Flow runtime: #{error}"
      end
      
      @initialized = true
      ObjectSpace.define_finalizer(self, self.class.finalize)
    end

    def self.finalize
      proc { LibFlow.flow_cleanup }
    end

    def initialized?
      @initialized
    end
  end

  # Represents a compiled Flow module
  class Module
    attr_reader :path

    def initialize(handle, path)
      @handle = handle
      @path = path
      @functions = {}
      
      raise CompileError, "Failed to create module" if @handle.null?
      
      ObjectSpace.define_finalizer(self, self.class.finalize(@handle))
    end

    def self.finalize(handle)
      proc { LibFlow.flow_unload_module(handle) unless handle.null? }
    end

    # Load a Flow module from a file
    #
    # @param runtime [Runtime] Flow runtime instance
    # @param path [String] Path to the .flow file
    # @return [Module] Compiled module
    #
    # @example
    #   runtime = Flow::Runtime.new
    #   mod = Flow::Module.load(runtime, 'math.flow')
    def self.load(runtime, path)
      raise ArgumentError, "File not found: #{path}" unless File.exist?(path)
      
      handle = LibFlow.flow_load_module(path)
      if handle.null?
        error = LibFlow.flow_get_error
        LibFlow.flow_clear_error # Clear error after retrieving
        raise CompileError, "Failed to load module: #{error}"
      end
      
      new(handle, path)
    end

    # Compile Flow source code from a string
    #
    # @param runtime [Runtime] Flow runtime instance
    # @param source [String] Flow source code
    # @return [Module] Compiled module
    #
    # @example
    #   runtime = Flow::Runtime.new
    #   mod = Flow::Module.compile(runtime, <<~FLOW)
    #     func add(a: int, b: int) -> int {
    #       return a + b;
    #     }
    #   FLOW
    def self.compile(runtime, source)
      handle = LibFlow.flow_compile_string(source)
      if handle.null?
        error = LibFlow.flow_get_error
        LibFlow.flow_clear_error # Clear error after retrieving
        raise CompileError, "Failed to compile: #{error}"
      end
      
      new(handle, '<compiled>')
    end

    # Call a Flow function
    #
    # @param function_name [String] Name of the function
    # @param args [Array] Arguments to pass
    # @return [Object] Return value (Integer, Float, String, Boolean, or nil)
    #
    # @example
    #   result = mod.call('add', 10, 20)  # => 30
    def call(function_name, *args)
      # Convert Ruby values to C values
      c_values = args.map { |arg| ruby_to_flow(arg) }
      
      # Create array for FFI
      c_array = FFI::MemoryPointer.new(LibFlow::FlowValueT, c_values.size)
      c_values.each_with_index do |val, i|
        LibFlow::FlowValueT.new(c_array + i * LibFlow::FlowValueT.size).tap do |struct|
          struct[:type] = val[:type]
          struct[:data] = val[:data]
        end
      end
      
      # Call the function
      result = LibFlow.flowc_call_v(@handle, function_name, c_values.size, c_array)
      
      # Check for errors
      error = LibFlow.flow_get_error
      if error && !error.empty?
        LibFlow.flow_clear_error # Clear error after retrieving
        raise RuntimeError, error
      end
      
      # Convert result back to Ruby
      flow_to_ruby(result)
    end

    # Allow calling functions as methods
    #
    # @example
    #   mod.add(10, 20)  # Same as mod.call('add', 10, 20)
    def method_missing(method, *args)
      call(method.to_s, *args)
    rescue RuntimeError => e
      if e.message.include?('Function not found')
        super
      else
        raise
      end
    end

    def respond_to_missing?(method, include_private = false)
      true
    end
    
    # ============================================================
    # REFLECTION API
    # ============================================================
    
    # Get the number of functions in this module
    #
    # @return [Integer] Number of functions
    #
    # @example
    #   count = mod.function_count
    #   puts "Module has #{count} functions"
    def function_count
      raise FlowError, "Module is not loaded" if @handle.null?
      LibFlow.flow_reflect_function_count(@handle)
    end
    
    # List all function names in this module
    #
    # @return [Array<String>] Array of function names
    #
    # @example
    #   functions = mod.list_functions
    #   puts "Functions: #{functions.join(', ')}"
    def list_functions
      raise FlowError, "Module is not loaded" if @handle.null?
      
      count = LibFlow.flow_reflect_function_count(@handle)
      return [] if count == 0
      
      # Allocate pointer to pointer array
      names_ptr = FFI::MemoryPointer.new(:pointer)
      actual_count = LibFlow.flow_reflect_list_functions(@handle, names_ptr)
      
      return [] if actual_count <= 0
      
      # Read the array of string pointers
      names_array = names_ptr.read_pointer
      function_names = []
      
      actual_count.times do |i|
        str_ptr = names_array.get_pointer(i * FFI::Pointer.size)
        function_names << str_ptr.read_string unless str_ptr.null?
      end
      
      # Free the C array
      LibFlow.flow_reflect_free_names(names_array, actual_count)
      
      function_names
    end
    
    # Get detailed information about a function
    #
    # @param function_name [String] Name of the function
    # @return [Hash] Function information with keys: :name, :return_type, :parameters
    #
    # @example
    #   info = mod.function_info('add')
    #   puts "Function: #{info[:name]}"
    #   puts "Returns: #{info[:return_type]}"
    #   info[:parameters].each do |param|
    #     puts "  #{param[:name]}: #{param[:type]}"
    #   end
    def function_info(function_name)
      raise FlowError, "Module is not loaded" if @handle.null?
      
      info_ptr = LibFlow.flow_reflect_get_function_info(@handle, function_name)
      raise FlowError, "Function '#{function_name}' not found in module" if info_ptr.null?
      
      begin
        info_struct = LibFlow::FlowFunctionInfo.new(info_ptr)
        
        result = {
          name: info_struct[:name].read_string,
          return_type: info_struct[:return_type].read_string,
          parameters: []
        }
        
        # Extract parameters
        param_count = info_struct[:param_count]
        if param_count > 0
          params_ptr = info_struct[:params]
          param_count.times do |i|
            param_struct = LibFlow::FlowParamInfo.new(params_ptr + i * LibFlow::FlowParamInfo.size)
            result[:parameters] << {
              name: param_struct[:name].read_string,
              type: param_struct[:type].read_string
            }
          end
        end
        
        result
      ensure
        LibFlow.flow_reflect_free_function_info(info_ptr)
      end
    end
    
    # Get a human-readable string representation of all functions in the module
    #
    # @return [String] Formatted string with all function signatures
    #
    # @example
    #   puts mod.inspect_functions
    #   # Output:
    #   # Module contains 3 function(s):
    #   #
    #   #   add(a: int, b: int) -> int
    #   #   subtract(x: int, y: int) -> int
    #   #   multiply(m: int, n: int) -> int
    def inspect_functions
      functions = list_functions
      return "Module contains no functions" if functions.empty?
      
      lines = ["Module contains #{functions.size} function(s):\n"]
      
      functions.each do |func_name|
        begin
          info = function_info(func_name)
          params_str = info[:parameters].map { |p| "#{p[:name]}: #{p[:type]}" }.join(", ")
          lines << "  #{info[:name]}(#{params_str}) -> #{info[:return_type]}"
        rescue FlowError => e
          lines << "  #{func_name} (error getting info: #{e.message})"
        end
      end
      
      lines.join("\n")
    end

    private

    # Convert Ruby value to Flow C value
    def ruby_to_flow(value)
      case value
      when Integer
        LibFlow.flow_int(value)
      when Float
        LibFlow.flow_float(value)
      when String
        LibFlow.flow_string(value)
      when TrueClass, FalseClass
        LibFlow.flow_bool(value)
      when NilClass
        LibFlow.flow_void
      else
        raise ArgumentError, "Unsupported value type: #{value.class}"
      end
    end

    # Convert Flow C value to Ruby value
    def flow_to_ruby(c_value)
      case c_value[:type]
      when :int
        LibFlow.flow_as_int(c_value)
      when :float
        LibFlow.flow_as_float(c_value)
      when :string
        LibFlow.flow_as_string(c_value)
      when :bool
        LibFlow.flow_as_bool(c_value)
      when :void
        nil
      else
        raise "Unknown Flow type: #{c_value[:type]}"
      end
    end
  end

  # Convenience methods

  # Load a Flow module from a file
  #
  # @param path [String] Path to the .flow file
  # @return [Module] Compiled module
  #
  # @example
  #   mod = Flow.load_module('math.flow')
  def self.load_module(path)
    runtime = Runtime.new
    Module.load(runtime, path)
  end

  # Compile Flow source code from a string
  #
  # @param source [String] Flow source code
  # @return [Module] Compiled module
  #
  # @example
  #   mod = Flow.compile('func add(a: int, b: int) -> int { return a + b; }')
  def self.compile(source)
    runtime = Runtime.new
    Module.compile(runtime, source)
  end

  # Get Flow language version
  #
  # @return [String] Version string
  def self.version
    VERSION
  end
end
