<div class="install-page">
  <div class="container">
    <div class="install-header">
      <h1>💾 Installing Flow</h1>
      <p class="subtitle">Get up and running with Flow in just a few steps</p>
    </div>

    <div class="install-content">
      <div class="step-box">
        <div class="step-number">1</div>
        <div class="step-content">
          <h3>Clone the Repository</h3>
          <p>First, clone the Flow repository from GitHub:</p>
          <div class="code-box">
            <code>git clone https://github.com/theDevJade/flow.git</code>
            <code>cd flow</code>
          </div>
        </div>
      </div>

      <div class="step-box">
        <div class="step-number">2</div>
        <div class="step-content">
          <h3>Install Dependencies</h3>
          <p>Install the required dependencies for your platform:</p>
          
          <div class="platform-tabs">
            <div class="platform-box">
              <h4>🍎 macOS</h4>
              <div class="code-box">
                <code>brew install llvm cmake rust</code>
              </div>
            </div>
            
            <div class="platform-box">
              <h4>🐧 Linux</h4>
              <div class="code-box">
                <code>sudo apt-get install build-essential cmake llvm-18-dev clang-18</code>
              </div>
            </div>
            
            <div class="platform-box">
              <h4>🪟 Windows</h4>
              <div class="code-box">
                <code># Install via WSL2 or use the Linux instructions</code>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="step-box">
        <div class="step-number">3</div>
        <div class="step-content">
          <h3>Build Flow Compiler</h3>
          <p>Build the Flow compiler from source:</p>
          <div class="code-box">
            <code>cd flowbase</code>
            <code>./build.sh</code>
          </div>
        </div>
      </div>

      <div class="step-box">
        <div class="step-number">4</div>
        <div class="step-content">
          <h3>Build River Package Manager</h3>
          <p>Build the River package manager (written in Rust):</p>
          <div class="code-box">
            <code>cd ../river</code>
            <code>cargo build --release</code>
          </div>
        </div>
      </div>

      <div class="info-box">
        <div class="info-icon">📝</div>
        <div class="info-content">
          <h4>Using River Package Manager</h4>
          <p>Once installed, you can use River to manage Flow packages:</p>
          <div class="code-box">
            <code>river install http</code>
            <code>river search networking</code>
            <code>river list</code>
          </div>
        </div>
      </div>

      <div class="help-section">
        <h3>Need Help?</h3>
        <div class="help-links">
          <a href="https://github.com/theDevJade/flow" target="_blank" rel="noopener" class="help-link">
            📚 Read the Documentation
          </a>
          <a href="https://github.com/theDevJade/flow/issues" target="_blank" rel="noopener" class="help-link">
            🐛 Report an Issue
          </a>
          <a href="https://github.com/theDevJade/flow/discussions" target="_blank" rel="noopener" class="help-link">
            💬 Join the Community
          </a>
        </div>
      </div>
    </div>
  </div>
</div>

<style>
  .install-page {
    width: 100%;
    min-height: 100vh;
    background: var(--bg-primary);
    padding: 4rem 0;
  }

  .install-header {
    text-align: center;
    margin-bottom: 4rem;
    animation: fadeIn 0.6s ease;
  }

  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(-20px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .install-header h1 {
    font-size: 3rem;
    margin: 0 0 1rem 0;
    background: linear-gradient(135deg, var(--ocean-cyan) 0%, var(--ocean-foam) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .subtitle {
    font-size: 1.25rem;
    color: var(--text-secondary);
    margin: 0;
  }

  .install-content {
    max-width: 900px;
    margin: 0 auto;
  }

  .step-box {
    display: flex;
    gap: 2rem;
    background: var(--bg-secondary);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 2rem;
    margin-bottom: 2rem;
    transition: all 0.3s ease;
    animation: slideUp 0.6s ease;
  }

  @keyframes slideUp {
    from { opacity: 0; transform: translateY(30px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .step-box:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(6, 182, 212, 0.2);
    border-color: var(--ocean-cyan);
  }

  .step-number {
    flex-shrink: 0;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--ocean-blue) 0%, var(--ocean-cyan) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.5rem;
    font-weight: bold;
    color: var(--text-primary);
    box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3);
  }

  .step-content {
    flex: 1;
  }

  .step-content h3 {
    margin: 0 0 0.75rem 0;
    color: var(--ocean-cyan);
    font-size: 1.5rem;
  }

  .step-content p {
    margin: 0 0 1rem 0;
    color: var(--text-secondary);
    line-height: 1.6;
  }

  .platform-tabs {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1rem;
    margin-top: 1rem;
  }

  .platform-box {
    background: var(--bg-elevated);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 1rem;
  }

  .platform-box h4 {
    margin: 0 0 0.75rem 0;
    color: var(--text-primary);
    font-size: 1.1rem;
  }

  .code-box {
    margin: 0.75rem 0;
  }

  .code-box code {
    display: block;
    color: var(--ocean-cyan);
    font-family: 'Courier New', monospace;
    font-size: 0.95rem;
    margin: 0.5rem 0;
    line-height: 1.5;
  }
  
  .code-box code:first-child {
    margin-top: 0;
  }
  
  .code-box code:last-child {
    margin-bottom: 0;
  }

  .info-box {
    display: flex;
    gap: 1.5rem;
    background: rgba(6, 182, 212, 0.1);
    border: 2px solid var(--accent-primary);
    border-radius: 12px;
    padding: 2rem;
    margin: 3rem 0;
    animation: slideUp 0.6s ease 0.2s both;
  }

  .info-icon {
    flex-shrink: 0;
    font-size: 3rem;
  }

  .info-content {
    flex: 1;
  }

  .info-content h4 {
    margin: 0 0 0.75rem 0;
    color: var(--accent-primary);
    font-size: 1.25rem;
  }

  .info-content p {
    margin: 0 0 1rem 0;
    color: var(--text-secondary);
    line-height: 1.6;
  }

  .help-section {
    text-align: center;
    margin-top: 4rem;
    padding: 2rem;
    animation: fadeIn 0.6s ease 0.3s both;
  }

  .help-section h3 {
    margin: 0 0 2rem 0;
    color: var(--text-primary);
    font-size: 2rem;
  }

  .help-links {
    display: flex;
    gap: 1.5rem;
    justify-content: center;
    flex-wrap: wrap;
  }

  .help-link {
    padding: 1rem 2rem;
    background: var(--bg-elevated);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    color: var(--ocean-cyan);
    text-decoration: none;
    font-weight: 500;
    transition: all 0.3s ease;
  }

  .help-link:hover {
    background: var(--ocean-medium);
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(6, 182, 212, 0.3);
    border-color: var(--ocean-cyan);
  }

  @media (max-width: 768px) {
    .install-page {
      padding: 2rem 0;
    }

    .install-header h1 {
      font-size: 2rem;
    }

    .subtitle {
      font-size: 1rem;
    }

    .step-box {
      flex-direction: column;
      gap: 1rem;
      padding: 1.5rem;
    }

    .platform-tabs {
      grid-template-columns: 1fr;
    }

    .info-box {
      flex-direction: column;
      gap: 1rem;
      padding: 1.5rem;
    }

    .help-links {
      flex-direction: column;
      align-items: stretch;
    }
  }
</style>

