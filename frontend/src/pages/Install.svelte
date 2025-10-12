<script lang="ts">
  import { onMount } from 'svelte';
  
  let platform: 'macos' | 'linux' | 'windows' = 'linux';
  let installCommand = '';
  
  const installCommands = {
    macos: "curl --proto '=https' --tlsv1.2 -sSf https://install.flowc.dev | sh",
    linux: "curl --proto '=https' --tlsv1.2 -sSf https://install.flowc.dev | sh",
    windows: "iwr -useb https://installwindows.flowc.dev/init.ps1 | iex"
  };
  
  onMount(() => {
    // Detect platform
    const userAgent = navigator.userAgent.toLowerCase();
    if (userAgent.indexOf('mac') !== -1) {
      platform = 'macos';
    } else if (userAgent.indexOf('win') !== -1) {
      platform = 'windows';
    } else {
      platform = 'linux';
    }
    installCommand = installCommands[platform];
  });
  
  function selectPlatform(p: 'macos' | 'linux' | 'windows') {
    platform = p;
    installCommand = installCommands[p];
  }
  
  function copyCommand() {
    navigator.clipboard.writeText(installCommand);
  }
</script>

<div class="install-page">
  <div class="container">
    <div class="install-header">
      <h1>💾 Installing Flow</h1>
      <p class="subtitle">Get up and running with Flow in just a few steps</p>
    </div>

    <div class="install-content">
      <div class="quick-install">
        <h2>⚡ Quick Install</h2>
        <p class="quick-install-subtitle">Install Flow with a single command</p>
        
        <div class="platform-selector">
          <button 
            class="platform-btn" 
            class:active={platform === 'macos'}
            on:click={() => selectPlatform('macos')}
          >
            🍎 macOS
          </button>
          <button 
            class="platform-btn" 
            class:active={platform === 'linux'}
            on:click={() => selectPlatform('linux')}
          >
            🐧 Linux
          </button>
          <button 
            class="platform-btn" 
            class:active={platform === 'windows'}
            on:click={() => selectPlatform('windows')}
          >
            🪟 Windows
          </button>
        </div>
        
        <div class="install-command-box">
          <code>{installCommand}</code>
          <button class="copy-btn" on:click={copyCommand}>
            📋 Copy
          </button>
        </div>
        
        <p class="install-note">
          This will install the Flow compiler, LSP server, and River package manager
        </p>
      </div>
      <div class="manual-install-section">
        <h2>🔧 Manual Installation</h2>
        <p class="section-subtitle">Or build from source for development</p>
      </div>

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

  .quick-install {
    background: linear-gradient(135deg, rgba(6, 182, 212, 0.15) 0%, rgba(34, 211, 238, 0.1) 100%);
    border: 2px solid var(--ocean-cyan);
    border-radius: 16px;
    padding: 2.5rem;
    margin-bottom: 4rem;
    text-align: center;
    animation: slideUp 0.6s ease;
  }

  .quick-install h2 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    background: linear-gradient(135deg, var(--ocean-cyan) 0%, var(--ocean-foam) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .quick-install-subtitle {
    color: var(--text-secondary);
    margin: 0 0 2rem 0;
    font-size: 1.1rem;
  }

  .platform-selector {
    display: flex;
    gap: 1rem;
    justify-content: center;
    margin-bottom: 1.5rem;
    flex-wrap: wrap;
  }

  .platform-btn {
    padding: 0.75rem 1.5rem;
    background: var(--bg-secondary);
    border: 2px solid var(--border-color);
    border-radius: 8px;
    color: var(--text-primary);
    font-size: 1rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
  }

  .platform-btn:hover {
    border-color: var(--ocean-cyan);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(6, 182, 212, 0.2);
  }

  .platform-btn.active {
    background: var(--ocean-medium);
    border-color: var(--ocean-cyan);
    color: var(--ocean-cyan);
  }

  .install-command-box {
    display: flex;
    align-items: center;
    gap: 1rem;
    background: #0a0e27;
    border: 1px solid var(--ocean-cyan);
    border-radius: 8px;
    padding: 1.25rem;
    margin: 0 auto;
    max-width: 800px;
  }

  .install-command-box code {
    flex: 1;
    color: var(--ocean-foam);
    font-family: 'Courier New', monospace;
    font-size: 1rem;
    text-align: left;
    word-break: break-all;
  }

  .copy-btn {
    flex-shrink: 0;
    padding: 0.5rem 1rem;
    background: var(--ocean-cyan);
    border: none;
    border-radius: 6px;
    color: var(--bg-primary);
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
  }

  .copy-btn:hover {
    background: var(--ocean-foam);
    transform: scale(1.05);
  }

  .install-note {
    margin: 1rem 0 0 0;
    color: var(--text-secondary);
    font-size: 0.9rem;
  }

  .manual-install-section {
    text-align: center;
    margin: 4rem 0 2rem 0;
  }

  .manual-install-section h2 {
    font-size: 2rem;
    color: var(--text-primary);
    margin: 0 0 0.5rem 0;
  }

  .section-subtitle {
    color: var(--text-secondary);
    margin: 0;
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

    .quick-install {
      padding: 1.5rem;
    }

    .quick-install h2 {
      font-size: 1.5rem;
    }

    .platform-selector {
      flex-direction: column;
    }

    .platform-btn {
      width: 100%;
    }

    .install-command-box {
      flex-direction: column;
      gap: 0.75rem;
    }

    .install-command-box code {
      font-size: 0.85rem;
    }

    .copy-btn {
      width: 100%;
    }
  }
</style>