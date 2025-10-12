<script lang="ts">
</script>

<div class="landing">
  
  <!-- Hero Section -->
  <section class="hero">
    <div class="container">
      <div class="hero-content">
        <h1 class="hero-title">
          Flow
          <span class="gradient-text">elegantly</span>
        </h1>
        <p class="hero-subtitle">
          A modern compiled systems programming language with seamless interoperability.
          Combining native performance via LLVM with the ease of high-level languages.
        </p>
        <div class="hero-actions">
          <a href="https://github.com/theDevJade/flow" target="_blank" class="btn-hero-primary">
            View on GitHub
          </a>
          <a href="https://github.com/theDevJade/flow/releases" target="_blank" class="btn-hero-secondary">
            Download
          </a>
        </div>
        
        <!-- Code Example -->
        <div class="code-showcase glass">
          <div class="code-header">
            <div class="code-dots">
              <span class="dot red"></span>
              <span class="dot yellow"></span>
              <span class="dot green"></span>
            </div>
            <span class="code-title">hello.flow</span>
          </div>
          <pre><code>{`link "c" {
    func sqrt(x: float) -> float;
    func pow(x: float, y: float) -> float;
}

struct Point {
    float x;
    float y;
}

export func calculate(p: Point) -> float {
    let distance: float = sqrt(pow(p.x, 2.0) + pow(p.y, 2.0));
    return distance;
}

func main() {
    let point: Point = { x: 3.0, y: 4.0 };
    print("Distance: " + calculate(point));  // 5.0
}`}</code></pre>
        </div>
      </div>
    </div>
    
  </section>
  
  <!-- Features Section -->
  <section class="features">
    <div class="container">
      <h2 class="section-title animate-fade-in">Why Flow?</h2>
      <div class="features-grid">
        <div class="feature-card glass animate-slide-up" style="animation-delay: 0.1s">
          <div class="feature-icon">⚡</div>
          <h3>Native Performance</h3>
          <p>Compiles to optimized machine code via LLVM. Zero-cost abstractions with systems-level control.</p>
        </div>
        
        <div class="feature-card glass animate-slide-up" style="animation-delay: 0.2s">
          <div class="feature-icon">🔗</div>
          <h3>C Interoperability</h3>
          <p>Call C functions directly with link blocks. Seamless integration with existing C libraries.</p>
        </div>
        
        <div class="feature-card glass animate-slide-up" style="animation-delay: 0.3s">
          <div class="feature-icon">☕</div>
          <h3>Java Integration</h3>
          <p>Full JNI bindings for embedding Flow in Java applications. Type-safe Java API included.</p>
        </div>
        
        <div class="feature-card glass animate-slide-up" style="animation-delay: 0.4s">
          <div class="feature-icon">📦</div>
          <h3>River Package Manager</h3>
          <p>Powerful package manager built with Rust. Create, build, and publish Flow packages effortlessly.</p>
        </div>
        
        <div class="feature-card glass animate-slide-up" style="animation-delay: 0.5s">
          <div class="feature-icon">🎯</div>
          <h3>Type Safe</h3>
          <p>Static typing with type inference. Optional types and rich type system for safer code.</p>
        </div>
        
        <div class="feature-card glass animate-slide-up" style="animation-delay: 0.6s">
          <div class="feature-icon">🛠️</div>
          <h3>Modern Tooling</h3>
          <p>VSCode extension with LSP support. Syntax highlighting, completion, and diagnostics built-in.</p>
        </div>
      </div>
    </div>
  </section>
  
  <!-- CTA Section -->
  <section class="cta">
    <div class="container">
      <div class="cta-content glass">
        <h2>Ready to dive in?</h2>
        <p>Get started with Flow and River package manager</p>
        <div class="install-command glass">
          <code>git clone https://github.com/theDevJade/flow.git</code>
        </div>
        <div class="cta-actions">
          <a href="https://github.com/theDevJade/flow" target="_blank" class="btn-cta">
            View on GitHub →
          </a>
          <a href="https://github.com/theDevJade/flow#installation" target="_blank" class="btn-cta-secondary">
            Installation Guide
          </a>
        </div>
      </div>
    </div>
  </section>
  
  <!-- Footer -->
  <footer class="footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-section">
          <h4>Flow</h4>
          <p>A modern compiled language for the modern web.</p>
        </div>
        <div class="footer-section">
          <h4>Resources</h4>
          <a href="https://github.com/theDevJade/flow#readme" target="_blank" rel="noopener">Documentation</a>
          <a href="https://github.com/theDevJade/flow/tree/main/test-packages" target="_blank" rel="noopener">Examples</a>
        </div>
        <div class="footer-section">
          <h4>Community</h4>
          <a href="https://github.com/theDevJade/flow" target="_blank" rel="noopener">GitHub</a>
          <a href="https://discord.gg/flowlang" target="_blank" rel="noopener">Discord</a>
          <a href="https://twitter.com/flowlang" target="_blank" rel="noopener">Twitter</a>
        </div>
        <div class="footer-section">
          <h4>Legal</h4>
          <a href="/privacy">Privacy</a>
          <a href="/terms">Terms</a>
          <a href="/license">License</a>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2025 Flow Language. MIT License.</p>
      </div>
    </div>
  </footer>
</div>

<style>
  .landing {
    width: 100%;
    min-height: 100vh;
    background: var(--bg-primary);
  }
  
  /* Hero Section */
  .hero {
    position: relative;
    min-height: 90vh;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    padding: 4rem 0;
  }
  
  .hero-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2rem;
    text-align: center;
    max-width: 900px;
    margin: 0 auto;
    position: relative;
    z-index: 2;
  }
  
  .hero-title {
    font-size: 4.5rem;
    font-weight: 800;
    margin: 0;
    line-height: 1.1;
    animation: fadeIn 0.8s ease-out;
  }
  
  .gradient-text {
    background: linear-gradient(135deg, var(--ocean-cyan) 0%, var(--ocean-foam) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    animation: shimmer 3s ease-in-out infinite;
    background-size: 200% 200%;
  }
  
  @keyframes shimmer {
    0%, 100% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
  }
  
  .hero-subtitle {
    font-size: 1.25rem;
    color: var(--text-secondary);
    max-width: 700px;
    margin: 0;
    animation: fadeIn 1s ease-out 0.2s both;
  }
  
  .hero-actions {
    display: flex;
    gap: 1rem;
    margin-top: 1rem;
    animation: fadeIn 1s ease-out 0.4s both;
  }
  
  .btn-hero-primary, .btn-hero-secondary {
    padding: 1rem 2rem;
    border-radius: 12px;
    font-weight: 600;
    font-size: 1.1rem;
    text-decoration: none;
    transition: all 0.3s ease;
  }
  
  .btn-hero-primary {
    background: linear-gradient(135deg, var(--ocean-blue) 0%, var(--ocean-cyan) 100%);
    color: var(--text-primary);
    box-shadow: 0 8px 24px rgba(6, 182, 212, 0.3);
  }
  
  .btn-hero-primary:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 32px rgba(6, 182, 212, 0.5);
  }
  
  .btn-hero-secondary {
    background: transparent;
    color: var(--accent-primary);
    border: 2px solid var(--accent-primary);
  }
  
  .btn-hero-secondary:hover {
    background: var(--accent-primary);
    color: var(--text-primary);
    transform: translateY(-3px);
  }
  
  .code-showcase {
    width: 100%;
    max-width: 650px;
    margin-top: 2rem;
    overflow: hidden;
    animation: fadeIn 1s ease-out 0.6s both;
  }
  
  .code-header {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem 1.5rem;
    background: rgba(10, 22, 40, 0.6);
    border-bottom: 1px solid var(--border-color);
  }
  
  .code-dots {
    display: flex;
    gap: 0.5rem;
  }
  
  .dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
  }
  
  .dot.red { background: #ff5f56; }
  .dot.yellow { background: #ffbd2e; }
  .dot.green { background: #27c93f; }
  
  .code-title {
    color: var(--text-muted);
    font-size: 0.9rem;
  }
  
  .code-showcase pre {
    margin: 0;
    padding: 1.5rem;
    background: transparent;
    border: none;
    text-align: left;
  }
  
  .code-showcase code {
    background: transparent;
    color: var(--ocean-foam);
    font-size: 0.95rem;
    line-height: 1.8;
  }
  
  
  /* Features Section */
  .features {
    padding: 6rem 0;
    position: relative;
    z-index: 2;
  }
  
  .section-title {
    text-align: center;
    margin-bottom: 3rem;
    font-size: 3rem;
  }
  
  .features-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 2rem;
  }
  
  .feature-card {
    padding: 2rem;
    text-align: center;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
  }
  
  .feature-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.1), transparent);
    transition: left 0.5s;
  }
  
  .feature-card:hover::before {
    left: 100%;
  }
  
  .feature-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 32px rgba(6, 182, 212, 0.3);
  }
  
  .feature-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
    display: inline-block;
    transition: transform 0.3s ease;
  }
  
  .feature-card:hover .feature-icon {
    transform: scale(1.2) rotate(5deg);
  }
  
  .feature-card h3 {
    margin: 0.5rem 0;
    font-size: 1.5rem;
  }
  
  .feature-card p {
    margin: 0;
    font-size: 1rem;
  }
  
  /* CTA Section */
  .cta {
    padding: 6rem 0;
    position: relative;
    z-index: 2;
  }
  
  .cta-content {
    text-align: center;
    padding: 4rem 2rem;
    max-width: 700px;
    margin: 0 auto;
  }
  
  .cta-content h2 {
    margin: 0 0 1rem 0;
    font-size: 2.5rem;
  }
  
  .cta-content p {
    margin: 0 0 2rem 0;
    font-size: 1.2rem;
  }
  
  .install-command {
    padding: 1.5rem;
    margin: 2rem 0;
    background: var(--bg-secondary);
  }
  
  .install-command code {
    font-size: 1.1rem;
    color: var(--ocean-foam);
  }
  
  .cta-actions {
    display: flex;
    gap: 1rem;
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .btn-cta, .btn-cta-secondary {
    display: inline-block;
    padding: 1rem 2.5rem;
    text-decoration: none;
    border-radius: 12px;
    font-weight: 600;
    font-size: 1.1rem;
    transition: all 0.3s ease;
  }
  
  .btn-cta {
    background: linear-gradient(135deg, var(--ocean-blue) 0%, var(--ocean-cyan) 100%);
    color: var(--text-primary);
    box-shadow: 0 8px 24px rgba(6, 182, 212, 0.3);
  }
  
  .btn-cta:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 32px rgba(6, 182, 212, 0.5);
  }
  
  .btn-cta-secondary {
    background: transparent;
    color: var(--accent-primary);
    border: 2px solid var(--accent-primary);
  }
  
  .btn-cta-secondary:hover {
    background: var(--accent-primary);
    color: var(--text-primary);
    transform: translateY(-3px);
  }
  
  /* Footer */
  .footer {
    background: var(--ocean-deep);
    border-top: 1px solid var(--border-color);
    padding: 3rem 0 1rem;
    position: relative;
    z-index: 2;
  }
  
  .footer-content {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 2rem;
    margin-bottom: 2rem;
  }
  
  .footer-section h4 {
    margin: 0 0 1rem 0;
    color: var(--text-primary);
    font-size: 1.1rem;
  }
  
  .footer-section p {
    margin: 0;
    color: var(--text-muted);
    font-size: 0.95rem;
  }
  
  .footer-section a {
    display: block;
    color: var(--text-secondary);
    text-decoration: none;
    margin-bottom: 0.5rem;
    transition: color 0.3s ease;
  }
  
  .footer-section a:hover {
    color: var(--accent-primary);
  }
  
  .footer-bottom {
    text-align: center;
    padding-top: 2rem;
    border-top: 1px solid var(--border-color);
  }
  
  .footer-bottom p {
    margin: 0;
    color: var(--text-muted);
    font-size: 0.9rem;
  }
  
  @media (max-width: 768px) {
    .hero-title {
      font-size: 3rem;
    }
    
    .hero-subtitle {
      font-size: 1.1rem;
    }
    
    .hero-actions {
      flex-direction: column;
      width: 100%;
    }
    
    .btn-hero-primary, .btn-hero-secondary {
      width: 100%;
    }
    
    .section-title {
      font-size: 2rem;
    }
    
    .features-grid {
      grid-template-columns: 1fr;
    }
  }
</style>

