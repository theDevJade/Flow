#!/usr/bin/env ruby
# frozen_string_literal: true

require_relative '../lib/flow'

puts "Flow Ruby Bindings - Error Handling Example"
puts "============================================"
puts

# Test 1: Runtime initialization
puts "Test 1: Runtime initialization"
begin
  runtime = Flow::Runtime.new
  puts "✓ Runtime initialized successfully"
rescue Flow::InitializationError => e
  puts "✗ Failed to initialize: #{e.message}"
end
puts

runtime = Flow::Runtime.new

# Test 2: Loading non-existent file
puts "Test 2: Loading non-existent file"
begin
  Flow::Module.load(runtime, 'nonexistent.flow')
  puts "✗ Should have failed"
rescue ArgumentError, Flow::CompileError => e
  puts "✓ Expected error: #{e.message}"
end
puts

# Test 3: Empty module
puts "Test 3: Empty module compilation"
begin
  # Note: Parser has error recovery, syntax errors might not raise immediately
  mod = Flow::Module.compile(runtime, '')
  # Try calling non-existent function to trigger error
  begin
    mod.call('test', 0, 0)
    puts "✗ Should have raised error"
  rescue Flow::RuntimeError => e
    puts "✓ Expected error (empty module): Function not found"
  end
rescue Flow::CompileError => e
  puts "✓ Expected compile error: #{e.message}"
end
puts

# Test 4: Calling non-existent function
puts "Test 4: Calling non-existent function"
begin
  source = <<~FLOW
    func add(a: int, b: int) -> int {
      return a + b;
    }
  FLOW
  mod = Flow::Module.compile(runtime, source)
  mod.call('nonexistent', 1)
  puts "✗ Should have failed"
rescue Flow::RuntimeError => e
  puts "✓ Expected error: #{e.message}"
end
puts

# Test 5: Successful call
puts "Test 5: Successful function call"
begin
  source = <<~FLOW
    func add(a: int, b: int) -> int {
      return a + b;
    }
  FLOW
  mod = Flow::Module.compile(runtime, source)
  result = mod.call('add', 10, 20)
  puts "✓ Success: #{result}"
rescue Flow::FlowError => e
  puts "✗ Unexpected error: #{e.message}"
end
puts

# Test 6: Method missing
puts "Test 6: Method missing with NoMethodError"
begin
  source = <<~FLOW
    func test() -> int {
      return 42;
    }
  FLOW
  mod = Flow::Module.compile(runtime, source)
  mod.this_function_does_not_exist
  puts "✗ Should have raised NoMethodError"
rescue NoMethodError => e
  puts "✓ NoMethodError raised as expected"
rescue Flow::RuntimeError => e
  puts "✓ RuntimeError raised: #{e.message}"
end
puts

puts "✓ Error handling tests completed!"

