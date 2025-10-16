#!/usr/bin/env ruby
# frozen_string_literal: true

require_relative '../lib/flow'

puts "Flow Ruby Bindings - Basic Example"
puts "==================================="
puts

# Initialize runtime
puts "Initializing Flow runtime..."
runtime = Flow::Runtime.new
puts "✓ Runtime initialized"
puts

# Compile inline Flow code
puts "Compiling inline Flow code..."
source = <<~FLOW
  func add(a: int, b: int) -> int {
    return a + b;
  }

  func multiply(x: int, y: int) -> int {
    return x * y;
  }

  func subtract(a: int, b: int) -> int {
    return a - b;
  }

  func is_positive(n: int) -> bool {
    return n > 0;
  }
FLOW

mod = Flow::Module.compile(runtime, source)
puts "✓ Module compiled"
puts

# Call integer functions
puts "Calling add(10, 20)..."
result = mod.call('add', 10, 20)
puts "Result: #{result}"
puts

puts "Calling multiply(6, 7)..."
result = mod.call('multiply', 6, 7)
puts "Result: #{result}"
puts

puts "Calling subtract(100, 58)..."
result = mod.call('subtract', 100, 58)
puts "Result: #{result}"
puts

# Call boolean function
puts "Calling is_positive(42)..."
result = mod.call('is_positive', 42, 0)  # FlowAPI requires 2 args
puts "Result: #{result}"
puts

# Using method_missing syntax
puts "Using method syntax: mod.add(15, 27)..."
result = mod.add(15, 27)
puts "Result: #{result}"
puts

puts "✓ All examples completed successfully!"

