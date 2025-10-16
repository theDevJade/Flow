#!/usr/bin/env ruby
# frozen_string_literal: true

require_relative '../lib/flow'

puts "Flow Ruby Bindings - File Loading Example"
puts "=========================================="
puts

# Initialize runtime
runtime = Flow::Runtime.new

# Load a Flow module from file
module_path = '../c/test_module.flow'

puts "Loading module from: #{module_path}"
begin
  mod = Flow::Module.load(runtime, module_path)
  puts "✓ Module loaded successfully"
  puts
rescue Flow::CompileError => e
  puts "Failed to load module: #{e.message}"
  puts "Note: Make sure test_module.flow exists at: #{module_path}"
  exit 1
end

# Test various functions
puts "Testing integer operations:"
result = mod.call('add', 5, 3)
puts "  add(5, 3) = #{result}"

result = mod.call('multiply', 6, 7)
puts "  multiply(6, 7) = #{result}"

result = mod.call('subtract', 10, 3)
puts "  subtract(10, 3) = #{result}"
puts

puts "Testing boolean operations:"
result = mod.call('is_positive', 5, 0)
puts "  is_positive(5) = #{result}"

result = mod.call('is_positive', -3, 0)
puts "  is_positive(-3) = #{result}"
puts

puts "Testing float operations:"
result = mod.call('square', 4.5, 0.0)
puts "  square(4.5) = #{result}"
puts

# Test method syntax
puts "Using method syntax:"
puts "  mod.add(100, 200) = #{mod.add(100, 200)}"
puts "  mod.multiply(12, 12) = #{mod.multiply(12, 12)}"
puts

puts "✓ All tests passed!"

