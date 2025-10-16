# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Flow do
  describe '.version' do
    it 'returns the version string' do
      expect(Flow.version).to eq('0.1.0')
    end
  end

  describe Flow::Runtime do
    describe '#initialize' do
      it 'initializes successfully' do
        expect { Flow::Runtime.new }.not_to raise_error
      end

      it 'sets initialized flag' do
        runtime = Flow::Runtime.new
        expect(runtime).to be_initialized
      end
    end
  end

  describe Flow::Module do
    let(:runtime) { Flow::Runtime.new }

    describe '.compile' do
      it 'compiles valid Flow code' do
        source = <<~FLOW
          func add(a: int, b: int) -> int {
            return a + b;
          }
        FLOW

        expect { Flow::Module.compile(runtime, source) }.not_to raise_error
      end

      it 'raises error for invalid syntax' do
        expect {
          Flow::Module.compile(runtime, 'invalid syntax')
        }.to raise_error(Flow::CompileError)
      end

      it 'returns a module with path "<compiled>"' do
        source = 'func test() -> int { return 42; }'
        mod = Flow::Module.compile(runtime, source)
        expect(mod.path).to eq('<compiled>')
      end
    end

    describe '.load' do
      it 'raises error for non-existent file' do
        expect {
          Flow::Module.load(runtime, 'nonexistent.flow')
        }.to raise_error(ArgumentError, /File not found/)
      end

      context 'with valid file' do
        let(:test_file) { '../c/test_module.flow' }

        it 'loads the module if file exists' do
          skip 'test_module.flow not available' unless File.exist?(test_file)
          
          expect { Flow::Module.load(runtime, test_file) }.not_to raise_error
        end
      end
    end

    describe '#call' do
      let(:source) do
        <<~FLOW
          func add(a: int, b: int) -> int {
            return a + b;
          }

          func multiply(a: int, b: int) -> int {
            return a * b;
          }

          func is_positive(n: int) -> bool {
            return n > 0;
          }

          func get_pi() -> float {
            return 3.14159;
          }
        FLOW
      end

      let(:mod) { Flow::Module.compile(runtime, source) }

      it 'calls integer function and returns result' do
        result = mod.call('add', 10, 20)
        expect(result).to eq(30)
      end

      it 'calls multiplication' do
        result = mod.call('multiply', 6, 7)
        expect(result).to eq(42)
      end

      it 'calls boolean function' do
        result = mod.call('is_positive', 5, 0)
        expect(result).to be true
      end

      it 'returns false for negative number' do
        result = mod.call('is_positive', -3, 0)
        expect(result).to be false
      end

      it 'raises error for non-existent function' do
        expect {
          mod.call('nonexistent')
        }.to raise_error(Flow::RuntimeError)
      end
    end

    describe '#method_missing' do
      let(:source) do
        <<~FLOW
          func double(x: int) -> int {
            return x * 2;
          }
        FLOW
      end

      let(:mod) { Flow::Module.compile(runtime, source) }

      it 'allows calling functions as methods' do
        result = mod.double(21)
        expect(result).to eq(42)
      end

      it 'raises NoMethodError for truly missing methods' do
        expect {
          mod.this_does_not_exist(1)
        }.to raise_error(Flow::RuntimeError).or raise_error(NoMethodError)
      end
    end
  end

  describe 'convenience methods' do
    describe '.compile' do
      it 'creates runtime and compiles code' do
        source = 'func test() -> int { return 1; }'
        expect { Flow.compile(source) }.not_to raise_error
      end
    end

    describe '.load_module' do
      it 'raises error for non-existent file' do
        expect {
          Flow.load_module('nonexistent.flow')
        }.to raise_error(ArgumentError)
      end
    end
  end

  describe 'value conversion' do
    let(:runtime) { Flow::Runtime.new }

    it 'handles integer values' do
      source = 'func echo(x: int) -> int { return x; }'
      mod = Flow::Module.compile(runtime, source)
      expect(mod.call('echo', 42, 0)).to eq(42)
    end

    it 'handles float values' do
      source = 'func echo(x: float) -> float { return x; }'
      mod = Flow::Module.compile(runtime, source)
      result = mod.call('echo', 3.14, 0.0)
      expect(result).to be_within(0.01).of(3.14)
    end

    it 'handles boolean values' do
      source = 'func echo(x: bool) -> bool { return x; }'
      mod = Flow::Module.compile(runtime, source)
      expect(mod.call('echo', true, false)).to be true
    end
  end

  describe 'error scenarios' do
    let(:runtime) { Flow::Runtime.new }

    it 'handles compilation errors gracefully' do
      expect {
        Flow::Module.compile(runtime, 'func broken {')
      }.to raise_error(Flow::CompileError)
    end

    it 'handles runtime errors gracefully' do
      source = 'func test() -> int { return 1; }'
      mod = Flow::Module.compile(runtime, source)
      
      expect {
        mod.call('nonexistent')
      }.to raise_error(Flow::RuntimeError)
    end
  end
end

