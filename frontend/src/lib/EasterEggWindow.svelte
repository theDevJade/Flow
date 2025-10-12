<script lang="ts">
  let sparkles = $state<Array<{x: number, y: number, id: number}>>([]);
  let nextId = 0;
  
  function addSparkle(e: MouseEvent) {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    sparkles = [...sparkles, { x, y, id: nextId++ }];
    
    setTimeout(() => {
      sparkles = sparkles.filter(s => s.id !== nextId - 1);
    }, 1000);
  }
</script>

<div class="myspace-page" onmousemove={addSparkle}>
  <!-- Sparkles -->
  {#each sparkles as sparkle (sparkle.id)}
    <div class="sparkle" style="left: {sparkle.x}px; top: {sparkle.y}px;">✨</div>
  {/each}
  
  <!-- UNDER CONSTRUCTION GIF -->
  <div class="construction-gif">
    <svg width="88" height="31" viewBox="0 0 88 31">
      <rect fill="#000" width="88" height="31"/>
      <rect class="construction-stripe" fill="#ffff00" x="0" y="0" width="44" height="31"/>
      <text x="44" y="21" fill="#ff0000" font-family="Arial" font-weight="bold" font-size="12">UNDER</text>
      <text x="44" y="9" fill="#000" font-family="Arial" font-weight="bold" font-size="8">⚠️</text>
      <rect class="construction-stripe" fill="#ffff00" x="44" y="0" width="44" height="31" opacity="0.5"/>
      <text x="10" y="21" fill="#000" font-family="Arial" font-weight="bold" font-size="10">CONSTRUCTION</text>
    </svg>
  </div>
  
  <!-- Spinning GIF -->
  <div class="spinning-gif">💿</div>
  <div class="spinning-gif-2">💾</div>
  
  <!-- NEW BLINK GIF -->
  <div class="new-blink">
    <svg width="50" height="20">
      <text x="5" y="15" fill="#ff00ff" font-family="Arial" font-weight="bold" font-size="14" class="blink-svg">NEW!</text>
    </svg>
  </div>
  
  <!-- FIRE GIF -->
  <div class="fire-gif">
    <svg width="100" height="80" viewBox="0 0 100 80">
      <path class="fire-flame1" d="M50 70 Q30 50 40 30 T50 10" fill="#ff4500" opacity="0.8"/>
      <path class="fire-flame2" d="M50 70 Q50 50 50 30 T50 5" fill="#ff6600" opacity="0.9"/>
      <path class="fire-flame3" d="M50 70 Q70 50 60 30 T50 10" fill="#ff8800" opacity="0.8"/>
    </svg>
  </div>
  
  <!-- EMAIL GIF -->
  <div class="email-gif">
    <svg width="40" height="30" viewBox="0 0 40 30">
      <rect fill="#0000ff" width="40" height="30" rx="3"/>
      <path d="M0 0 L20 15 L40 0" stroke="#fff" stroke-width="2" fill="none" class="email-flap"/>
      <text x="8" y="22" fill="#ffff00" font-family="Arial" font-size="10" font-weight="bold">✉️</text>
    </svg>
  </div>
  
  <!-- SKULL GIF -->
  <div class="skull-gif">💀</div>
  
  <!-- STAR GIF -->
  <div class="star-gif">⭐</div>
  <div class="star-gif-2">✨</div>
  
  <!-- Marquee -->
  <div class="marquee marquee-1">
    <span>★ WELCOME TO MY PAGE ★ UNDER CONSTRUCTION ★ BEST VIEWED IN NETSCAPE NAVIGATOR ★ SIGN MY GUESTBOOK ★</span>
  </div>
  <div class="marquee marquee-2">
    <span>💖 THANKS 4 VISITING 💖 YOU ARE VISITOR #999999 💖 ADD ME AS UR FRIEND 💖</span>
  </div>
  
  <!-- Main Content -->
  <div class="content-wrapper">
    <div class="profile-pic">
      <div class="rotating-border">
        <div class="pic">🤖</div>
      </div>
    </div>
    
    <h1 class="glitter-text">~*~WeLcOmE tO mY pAgE~*~</h1>
    
    <div class="status-bar">
      <div class="status-item blink">● ONLINE</div>
      <div class="status-item">Mood: ✨VIBING✨</div>
      <div class="status-item">Song: Sandstorm - Darude</div>
    </div>
    
    <div class="about-me">
      <h2>~About Me~</h2>
      <p class="rainbow-text">
        hey!!!1! thanx 4 visiting my page!!! <br>
        i ❤️ computers & the internet is SO COOL!!!<br>
        <span class="blink">ADD ME</span> as ur friend!! :D :D :D
      </p>
    </div>
    
    <div class="dancing-emoji">🕺</div>
    <div class="dancing-emoji-2">💃</div>
    
    <div class="flames">
      <span>🔥</span><span>🔥</span><span>🔥</span><span>🔥</span><span>🔥</span>
    </div>
    
    <div class="guestbook">
      <h3>✍️ Guestbook ✍️</h3>
      <div class="entry">
        <strong>xXx_CoolKid99_xXx:</strong> awesome page!!! check out mine! ⭐⭐⭐
      </div>
      <div class="entry">
        <strong>NetRunner2000:</strong> sick gifs dude!! 🔥🔥🔥
      </div>
      <div class="entry">
        <strong>~*AngelBaby*~:</strong> ur page is da BOMB dot com!! 💖💖
      </div>
    </div>
    
    <div class="webring">
      <p>This page is part of the 🌐 COOL CODERS WEBRING 🌐</p>
      <button class="prev">← Prev</button>
      <button class="random">Random</button>
      <button class="next">Next →</button>
    </div>
    
    <div class="counter">
      <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='20'%3E%3Crect fill='%23000' width='100' height='20'/%3E%3Ctext x='10' y='15' fill='%2300ff00' font-family='monospace' font-size='14'%3E👁️ 999999%3C/text%3E%3C/svg%3E" alt="visitor counter">
    </div>
  </div>
  
  <!-- Bottom Marquees -->
  <div class="marquee marquee-bottom">
    <span>💫 You are visitor #999999! 💫 This page best viewed at 800x600 💫 Geocities Forever! 💫</span>
  </div>
  <div class="marquee marquee-bottom-2">
    <span>🌟 FREE CURSORS 🌟 FREE BACKGROUNDS 🌟 FREE MIDI FILES 🌟 WEBMASTER ZONE 🌟</span>
  </div>
  
  <!-- Floating elements -->
  <div class="floating-element floating-1">🎵</div>
  <div class="floating-element floating-2">💝</div>
  <div class="floating-element floating-3">🌈</div>
  <div class="floating-element floating-4">🎸</div>
</div>

<style>
  .myspace-page {
    background: 
      repeating-linear-gradient(45deg, #ff00ff 0px, #ff00ff 10px, #00ffff 10px, #00ffff 20px),
      repeating-linear-gradient(-45deg, transparent 0px, transparent 10px, rgba(255,255,0,0.3) 10px, rgba(255,255,0,0.3) 20px),
      radial-gradient(circle at 20% 50%, #ff1493, transparent),
      radial-gradient(circle at 80% 50%, #00ffff, transparent),
      linear-gradient(180deg, #8b008b, #000);
    min-height: 100%;
    padding: 20px;
    position: relative;
    overflow: auto;
    font-family: 'Comic Sans MS', cursive;
    color: #fff;
    animation: backgroundShift 5s ease infinite, backgroundPulse 3s ease-in-out infinite;
  }
  
  @keyframes backgroundShift {
    0%, 100% { filter: hue-rotate(0deg) saturate(1.5); }
    50% { filter: hue-rotate(180deg) saturate(2); }
  }
  
  @keyframes backgroundPulse {
    0%, 100% { background-size: 100% 100%; }
    50% { background-size: 110% 110%; }
  }
  
  .sparkle {
    position: absolute;
    pointer-events: none;
    animation: sparkleFloat 1s ease-out forwards;
    font-size: 20px;
  }
  
  @keyframes sparkleFloat {
    0% { transform: scale(0) rotate(0deg); opacity: 1; }
    100% { transform: scale(2) rotate(180deg); opacity: 0; }
  }
  
  .construction-gif {
    position: absolute;
    top: 10px;
    left: 50%;
    transform: translateX(-50%);
    animation: shake 0.3s infinite;
    z-index: 100;
  }
  
  .construction-stripe {
    animation: stripeMove 1s linear infinite;
  }
  
  @keyframes stripeMove {
    from { transform: translateX(0); }
    to { transform: translateX(10px); }
  }
  
  @keyframes shake {
    0%, 100% { transform: translateX(-50%) rotate(0deg); }
    25% { transform: translateX(-50%) rotate(-2deg); }
    75% { transform: translateX(-50%) rotate(2deg); }
  }
  
  .spinning-gif {
    position: absolute;
    top: 100px;
    left: 30px;
    font-size: 80px;
    animation: spin 2s linear infinite, colorShift 3s ease infinite;
    filter: drop-shadow(0 0 10px #ff00ff);
  }
  
  .spinning-gif-2 {
    position: absolute;
    top: 100px;
    right: 30px;
    font-size: 80px;
    animation: spin 1.5s linear infinite reverse, colorShift 3s ease infinite 1s;
    filter: drop-shadow(0 0 10px #00ffff);
  }
  
  @keyframes spin {
    from { transform: rotate(0deg) scale(1); }
    50% { transform: rotate(180deg) scale(1.2); }
    to { transform: rotate(360deg) scale(1); }
  }
  
  @keyframes colorShift {
    0%, 100% { filter: drop-shadow(0 0 10px #ff00ff) hue-rotate(0deg); }
    50% { filter: drop-shadow(0 0 20px #00ffff) hue-rotate(180deg); }
  }
  
  .new-blink {
    position: absolute;
    top: 60px;
    right: 150px;
    animation: blink 0.5s infinite, bounce 1s ease infinite;
  }
  
  .blink-svg {
    animation: colorCycle 1s infinite;
  }
  
  @keyframes colorCycle {
    0% { fill: #ff00ff; }
    33% { fill: #00ffff; }
    66% { fill: #ffff00; }
    100% { fill: #ff00ff; }
  }
  
  @keyframes bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-10px); }
  }
  
  .fire-gif {
    position: absolute;
    top: 200px;
    left: 50px;
    animation: wiggle 0.5s ease infinite;
  }
  
  .fire-flame1, .fire-flame2, .fire-flame3 {
    animation: flicker 0.3s ease-in-out infinite;
  }
  
  .fire-flame2 { animation-delay: 0.1s; }
  .fire-flame3 { animation-delay: 0.2s; }
  
  @keyframes flicker {
    0%, 100% { opacity: 0.8; transform: scaleY(1); }
    50% { opacity: 1; transform: scaleY(1.1); }
  }
  
  @keyframes wiggle {
    0%, 100% { transform: rotate(-3deg); }
    50% { transform: rotate(3deg); }
  }
  
  .email-gif {
    position: absolute;
    top: 200px;
    right: 50px;
    animation: bounce 1s ease infinite;
  }
  
  .email-flap {
    animation: emailFlap 2s ease-in-out infinite;
  }
  
  @keyframes emailFlap {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-5px); }
  }
  
  .skull-gif {
    position: absolute;
    top: 300px;
    left: 20px;
    font-size: 50px;
    animation: shake 0.5s infinite, glow 1s ease-in-out infinite;
  }
  
  @keyframes glow {
    0%, 100% { filter: drop-shadow(0 0 5px #ff00ff); }
    50% { filter: drop-shadow(0 0 20px #00ffff); }
  }
  
  .star-gif {
    position: absolute;
    top: 300px;
    right: 20px;
    font-size: 50px;
    animation: twinkle 1s ease-in-out infinite;
  }
  
  .star-gif-2 {
    position: absolute;
    top: 350px;
    right: 80px;
    font-size: 40px;
    animation: twinkle 1s ease-in-out infinite 0.5s;
  }
  
  @keyframes twinkle {
    0%, 100% { transform: scale(1) rotate(0deg); opacity: 1; }
    50% { transform: scale(1.5) rotate(180deg); opacity: 0.5; }
  }
  
  .marquee {
    background: linear-gradient(90deg, #ff00ff, #00ffff, #ffff00, #ff00ff);
    background-size: 200% 100%;
    animation: gradientSlide 3s linear infinite;
    padding: 12px;
    border: 4px solid #ffff00;
    box-shadow: 0 0 30px #ff00ff, inset 0 0 20px rgba(255,255,255,0.3);
    overflow: hidden;
    white-space: nowrap;
    margin-bottom: 5px;
  }
  
  @keyframes gradientSlide {
    0% { background-position: 0% 50%; }
    100% { background-position: 200% 50%; }
  }
  
  .marquee span {
    display: inline-block;
    animation: marqueeScroll 12s linear infinite;
    font-weight: bold;
    font-size: 16px;
    text-shadow: 2px 2px 4px #000, 0 0 10px #fff;
  }
  
  @keyframes marqueeScroll {
    from { transform: translateX(100%); }
    to { transform: translateX(-100%); }
  }
  
  .marquee-1 {
    border-color: #ff00ff;
  }
  
  .marquee-2 {
    background: linear-gradient(90deg, #ff0000, #ffff00, #00ff00, #ff0000);
    background-size: 200% 100%;
    border-color: #00ff00;
    box-shadow: 0 0 30px #00ff00, inset 0 0 20px rgba(255,255,255,0.3);
  }
  
  .marquee-bottom {
    margin-top: 30px;
    background: linear-gradient(90deg, #00ff00, #00ffff, #ff00ff, #00ff00);
    background-size: 200% 100%;
    border-color: #00ffff;
    box-shadow: 0 0 30px #00ffff, inset 0 0 20px rgba(255,255,255,0.3);
  }
  
  .marquee-bottom-2 {
    background: linear-gradient(90deg, #ffff00, #ff00ff, #00ffff, #ffff00);
    background-size: 200% 100%;
    border: 4px solid #ff00ff;
    box-shadow: 0 0 30px #ff00ff, inset 0 0 20px rgba(255,255,255,0.3);
    padding: 12px;
    overflow: hidden;
    white-space: nowrap;
    margin-top: 5px;
  }
  
  .marquee-bottom-2 span {
    display: inline-block;
    animation: marqueeScroll 10s linear infinite reverse;
    font-weight: bold;
    font-size: 16px;
    text-shadow: 2px 2px 4px #000, 0 0 10px #fff;
  }
  
  .content-wrapper {
    background: rgba(0, 0, 0, 0.7);
    border: 5px ridge #ff00ff;
    margin: 30px auto;
    padding: 30px;
    max-width: 600px;
    box-shadow: 0 0 30px #00ffff;
    position: relative;
  }
  
  .profile-pic {
    text-align: center;
    margin-bottom: 20px;
  }
  
  .rotating-border {
    width: 120px;
    height: 120px;
    margin: 0 auto;
    background: linear-gradient(45deg, #ff00ff, #00ffff, #ffff00, #ff00ff);
    background-size: 400% 400%;
    animation: gradient 3s ease infinite, pulse 2s ease-in-out infinite;
    border-radius: 50%;
    padding: 5px;
  }
  
  @keyframes gradient {
    0%, 100% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
  }
  
  @keyframes pulse {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.1); }
  }
  
  .pic {
    width: 100%;
    height: 100%;
    background: #000;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 60px;
  }
  
  .glitter-text {
    font-size: 2.5rem;
    font-weight: bold;
    text-align: center;
    background: linear-gradient(90deg, #ff0000, #ff7f00, #ffff00, #00ff00, #0000ff, #4b0082, #9400d3);
    background-size: 200% 100%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    animation: rainbow 2s linear infinite;
    text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
  }
  
  @keyframes rainbow {
    0% { background-position: 0% 50%; }
    100% { background-position: 200% 50%; }
  }
  
  .status-bar {
    display: flex;
    justify-content: space-around;
    background: #000;
    border: 2px solid #00ff00;
    padding: 10px;
    margin: 20px 0;
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .status-item {
    color: #00ff00;
    font-family: 'Courier New', monospace;
    font-weight: bold;
  }
  
  .blink {
    animation: blink 1s infinite;
  }
  
  @keyframes blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
  }
  
  .about-me {
    background: linear-gradient(135deg, #ff1493, #ff69b4);
    border: 3px dotted #ffff00;
    padding: 20px;
    margin: 20px 0;
    text-align: center;
  }
  
  .about-me h2 {
    margin-top: 0;
    text-shadow: 3px 3px 0 #000;
    font-size: 2rem;
  }
  
  .rainbow-text {
    font-size: 1.2rem;
    line-height: 1.8;
  }
  
  .dancing-emoji {
    position: absolute;
    bottom: 100px;
    left: 20px;
    font-size: 50px;
    animation: dance 0.5s infinite;
  }
  
  .dancing-emoji-2 {
    position: absolute;
    bottom: 100px;
    right: 20px;
    font-size: 50px;
    animation: dance 0.5s infinite 0.25s;
  }
  
  @keyframes dance {
    0%, 100% { transform: translateY(0) rotate(0deg); }
    25% { transform: translateY(-10px) rotate(-10deg); }
    75% { transform: translateY(-10px) rotate(10deg); }
  }
  
  .flames {
    text-align: center;
    margin: 20px 0;
    font-size: 40px;
  }
  
  .flames span {
    display: inline-block;
    animation: flame 0.5s ease-in-out infinite;
  }
  
  .flames span:nth-child(1) { animation-delay: 0s; }
  .flames span:nth-child(2) { animation-delay: 0.1s; }
  .flames span:nth-child(3) { animation-delay: 0.2s; }
  .flames span:nth-child(4) { animation-delay: 0.3s; }
  .flames span:nth-child(5) { animation-delay: 0.4s; }
  
  @keyframes flame {
    0%, 100% { transform: translateY(0) scale(1); }
    50% { transform: translateY(-10px) scale(1.2); }
  }
  
  .guestbook {
    background: #ffffcc;
    color: #000;
    border: 3px groove #ff00ff;
    padding: 20px;
    margin: 20px 0;
  }
  
  .guestbook h3 {
    margin-top: 0;
    text-align: center;
    color: #ff00ff;
  }
  
  .entry {
    background: #fff;
    border: 1px solid #ccc;
    padding: 10px;
    margin: 10px 0;
    border-radius: 5px;
  }
  
  .entry strong {
    color: #0000ff;
  }
  
  .webring {
    text-align: center;
    background: #000;
    border: 2px solid #00ff00;
    padding: 20px;
    margin: 20px 0;
  }
  
  .webring p {
    margin: 0 0 15px 0;
    font-weight: bold;
    color: #00ff00;
  }
  
  .webring button {
    background: linear-gradient(180deg, #ff00ff, #8b008b);
    border: 2px solid #fff;
    color: #fff;
    padding: 8px 16px;
    margin: 0 5px;
    cursor: pointer;
    font-weight: bold;
    font-family: 'Comic Sans MS', cursive;
  }
  
  .webring button:hover {
    background: linear-gradient(180deg, #00ffff, #008b8b);
  }
  
  .counter {
    text-align: center;
    margin: 20px 0;
    animation: pulse 2s ease-in-out infinite;
  }
  
  .floating-element {
    position: absolute;
    font-size: 40px;
    animation: float 4s ease-in-out infinite;
    filter: drop-shadow(0 0 10px #fff);
  }
  
  .floating-1 {
    top: 450px;
    left: 10%;
    animation-delay: 0s;
  }
  
  .floating-2 {
    top: 500px;
    right: 10%;
    animation-delay: 1s;
  }
  
  .floating-3 {
    top: 400px;
    left: 30%;
    animation-delay: 2s;
  }
  
  .floating-4 {
    top: 520px;
    right: 30%;
    animation-delay: 3s;
  }
  
  @keyframes float {
    0%, 100% { transform: translateY(0) rotate(0deg); }
    25% { transform: translateY(-20px) rotate(10deg); }
    50% { transform: translateY(0) rotate(0deg); }
    75% { transform: translateY(-10px) rotate(-10deg); }
  }
</style>

