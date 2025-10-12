<div class="install-guide">
  <h2>💾 Installing Flow</h2>
  
  <div class="step-box">
    <h3>Step 1: Clone the Repository</h3>
    <div class="code-box">
      <code>git clone https://github.com/theDevJade/flow.git</code>
      <code>cd flow</code>
    </div>
  </div>
  
  <div class="step-box">
    <h3>Step 2: Install Dependencies</h3>
    <p><strong>macOS:</strong></p>
    <div class="code-box">
      <code>brew install llvm cmake rust</code>
    </div>
    <p><strong>Linux:</strong></p>
    <div class="code-box">
      <code>sudo apt-get install build-essential cmake llvm-18-dev clang-18</code>
    </div>
  </div>
  
  <div class="step-box">
    <h3>Step 3: Build Flow Compiler</h3>
    <div class="code-box">
      <code>cd flowbase</code>
      <code>./build.sh</code>
    </div>
  </div>
  
  <div class="step-box">
    <h3>Step 4: Build River Package Manager</h3>
    <div class="code-box">
      <code>cd ../river</code>
      <code>cargo build --release</code>
    </div>
  </div>
  
  <div class="note">
    <strong>📝 Note:</strong> River package manager allows you to install Flow packages:
    <div class="code-box">
      <code>river install http</code>
    </div>
  </div>
</div>

<style>
  .install-guide {
    padding: 20px;
    max-height: 500px;
    overflow-y: auto;
  }
  
  h2 {
    margin: 0 0 20px 0;
    color: var(--text-primary);
  }
  
  .step-box {
    background: var(--bg-secondary);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 15px;
  }
  
  .step-box h3 {
    margin: 0 0 10px 0;
    color: var(--ocean-cyan);
  }
  
  .step-box p {
    margin: 10px 0 5px 0;
    color: var(--text-secondary);
    font-weight: bold;
  }
  
  .code-box {
    background: #000;
    border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: 10px;
    margin: 10px 0;
  }
  
  .code-box code {
    display: block;
    color: #0f0;
    font-family: 'Courier New', monospace;
    margin: 5px 0;
  }
  
  .note {
    background: rgba(6, 182, 212, 0.1);
    border: 1px solid var(--accent-primary);
    border-radius: 8px;
    padding: 15px;
    margin-top: 20px;
  }
  
  .note strong {
    color: var(--accent-primary);
  }
</style>

