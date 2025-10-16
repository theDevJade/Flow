Gem::Specification.new do |spec|
  spec.name          = "flow-lang"
  spec.version       = "0.1.0"
  spec.authors       = ["Flow Language Team"]
  spec.email         = ["team@flowc.dev"]

  spec.summary       = "Ruby bindings for the Flow programming language"
  spec.description   = "Call Flow functions from Ruby code using FFI"
  spec.homepage      = "https://flowc.dev"
  spec.license       = "MIT"
  spec.required_ruby_version = ">= 2.7.0"

  spec.files         = Dir["lib/**/*", "examples/**/*", "spec/**/*", "README.md"]
  spec.require_paths = ["lib"]

  # Runtime dependency
  spec.add_dependency "ffi", "~> 1.15"

  # Development dependencies
  spec.add_development_dependency "rake", "~> 13.0"
  spec.add_development_dependency "rspec", "~> 3.0"
end

