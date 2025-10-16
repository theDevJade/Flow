#!/usr/bin/env ruby
# frozen_string_literal: true

# Comprehensive test suite for Flow Ruby bindings reflection API

require_relative 'lib/flow'
require 'minitest/autorun'

class FlowReflectionTest < Minitest::Test
  def setup
    @runtime = Flow::Runtime.new
  end

  def test_function_count
    code = <<~FLOW
      func add(a: int, b: int) -> int {
        return a + b;
      }
      
      func subtract(x: int, y: int) -> int {
        return x - y;
      }
      
      func multiply(m: int, n: int) -> int {
        return m * n;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    assert_equal 3, mod.function_count, "Should have 3 functions"
  end

  def test_list_functions
    code = <<~FLOW
      func greet(name: string) -> string {
        return "Hello, " + name;
      }
      
      func square(x: int) -> int {
        return x * x;
      }
      
      func is_positive(n: int) -> bool {
        return n > 0;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    functions = mod.list_functions
    
    assert_equal 3, functions.size, "Should have 3 function names"
    assert_includes functions, 'greet', "Should include 'greet'"
    assert_includes functions, 'square', "Should include 'square'"
    assert_includes functions, 'is_positive', "Should include 'is_positive'"
  end

  def test_function_info_with_parameters
    code = <<~FLOW
      func add(a: int, b: int) -> int {
        return a + b;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    info = mod.function_info('add')
    
    assert_equal 'add', info[:name], "Function name should be 'add'"
    assert_equal 'int', info[:return_type], "Return type should be 'int'"
    assert_equal 2, info[:parameters].size, "Should have 2 parameters"
    
    assert_equal 'a', info[:parameters][0][:name], "First param name"
    assert_equal 'int', info[:parameters][0][:type], "First param type"
    assert_equal 'b', info[:parameters][1][:name], "Second param name"
    assert_equal 'int', info[:parameters][1][:type], "Second param type"
  end

  def test_function_info_string_parameter
    code = <<~FLOW
      func greet(name: string) -> string {
        return "Hello, " + name;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    info = mod.function_info('greet')
    
    assert_equal 'greet', info[:name]
    assert_equal 'string', info[:return_type]
    assert_equal 1, info[:parameters].size
    assert_equal 'name', info[:parameters][0][:name]
    assert_equal 'string', info[:parameters][0][:type]
  end

  def test_function_info_no_parameters
    code = <<~FLOW
      func get_pi() -> float {
        return 3.14159;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    info = mod.function_info('get_pi')
    
    assert_equal 'get_pi', info[:name]
    assert_equal 'float', info[:return_type]
    assert_equal 0, info[:parameters].size, "Should have no parameters"
  end

  def test_function_info_nonexistent
    code = <<~FLOW
      func test() -> int {
        return 42;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    
    assert_raises Flow::FlowError do
      mod.function_info('nonexistent')
    end
  end

  def test_inspect_functions
    code = <<~FLOW
      func add(a: int, b: int) -> int {
        return a + b;
      }
      
      func greet(name: string) -> string {
        return "Hello";
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    inspection = mod.inspect_functions
    
    assert_match(/add\(a: int, b: int\) -> int/, inspection, "Should contain 'add' signature")
    assert_match(/greet\(name: string\) -> string/, inspection, "Should contain 'greet' signature")
    assert_match(/2 function\(s\)/, inspection, "Should mention 2 functions")
  end

  def test_boolean_return_types
    code = <<~FLOW
      func is_positive(x: int) -> bool {
        return x > 0;
      }
      
      func is_even(n: int) -> bool {
        return n % 2 == 0;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    
    info = mod.function_info('is_positive')
    assert_equal 'bool', info[:return_type]
    
    functions = mod.list_functions
    assert_equal 2, functions.size
  end

  def test_large_module
    # Generate a module with many functions
    code = (0...20).map do |i|
      "func func#{i}(x: int) -> int {\n  return x * #{i};\n}\n"
    end.join("\n")
    
    mod = Flow::Module.compile(@runtime, code)
    
    count = mod.function_count
    assert_equal 20, count, "Should have 20 functions"
    
    functions = mod.list_functions
    assert_equal 20, functions.size, "Should list 20 functions"
    
    # Check specific functions
    info0 = mod.function_info('func0')
    assert_equal 'func0', info0[:name]
    
    info19 = mod.function_info('func19')
    assert_equal 'func19', info19[:name]
  end

  def test_functions_still_callable_after_reflection
    code = <<~FLOW
      func add(a: int, b: int) -> int {
        return a + b;
      }
      
      func multiply(x: int, y: int) -> int {
        return x * y;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    
    # Do reflection
    count = mod.function_count
    assert_equal 2, count
    
    info = mod.function_info('add')
    assert_equal 'add', info[:name]
    
    # Now call the functions to ensure reflection didn't break them
    result = mod.call('add', 10, 20)
    assert_equal 30, result, "add(10, 20) should return 30"
    
    result = mod.call('multiply', 7, 6)
    assert_equal 42, result, "multiply(7, 6) should return 42"
  end

  def test_mixed_parameter_types
    code = <<~FLOW
      func process(a: int, b: float, c: string, d: bool) -> string {
        return "processed";
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    info = mod.function_info('process')
    
    assert_equal 4, info[:parameters].size
    assert_equal 'int', info[:parameters][0][:type]
    assert_equal 'float', info[:parameters][1][:type]
    assert_equal 'string', info[:parameters][2][:type]
    assert_equal 'bool', info[:parameters][3][:type]
  end

  def test_empty_module
    code = "// Just a comment"
    
    mod = Flow::Module.compile(@runtime, code)
    
    count = mod.function_count
    assert_equal 0, count, "Empty module should have 0 functions"
    
    functions = mod.list_functions
    assert_equal 0, functions.size, "Empty module should list 0 functions"
    
    inspection = mod.inspect_functions
    assert_match(/no functions/i, inspection, "Should mention no functions")
  end

  def test_file_loaded_module
    # Create a temporary Flow file
    require 'tempfile'
    
    file = Tempfile.new(['test_reflect', '.flow'])
    begin
      file.write(<<~FLOW)
        func square(x: int) -> int {
          return x * x;
        }
        
        func cube(x: int) -> int {
          return x * x * x;
        }
      FLOW
      file.close
      
      mod = Flow::Module.load(@runtime, file.path)
      functions = mod.list_functions
      
      assert_includes functions, 'square', "'square' should be in loaded module"
      assert_includes functions, 'cube', "'cube' should be in loaded module"
      
      info = mod.function_info('square')
      assert_equal 'square', info[:name]
    ensure
      file.unlink
    end
  end

  def test_float_return_type
    code = <<~FLOW
      func calculate_pi() -> float {
        return 3.14159;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    info = mod.function_info('calculate_pi')
    
    assert_equal 'float', info[:return_type]
    assert_equal 0, info[:parameters].size
  end

  def test_multiple_string_functions
    code = <<~FLOW
      func concat(a: string, b: string) -> string {
        return a + b;
      }
      
      func repeat(s: string, n: int) -> string {
        return s;
      }
    FLOW
    
    mod = Flow::Module.compile(@runtime, code)
    
    info1 = mod.function_info('concat')
    assert_equal 2, info1[:parameters].size
    assert_equal 'string', info1[:parameters][0][:type]
    assert_equal 'string', info1[:parameters][1][:type]
    
    info2 = mod.function_info('repeat')
    assert_equal 2, info2[:parameters].size
    assert_equal 'string', info2[:parameters][0][:type]
    assert_equal 'int', info2[:parameters][1][:type]
  end
end

# Run the tests if this file is executed directly
if __FILE__ == $0
  puts "\n" + "=" * 60
  puts "Flow Ruby Reflection API Tests"
  puts "=" * 60 + "\n"
end

