<template>
  <router-view />
</template>

<style>
:root {
  color-scheme: light;
  --bg-base: #f3f5fb;
  --bg-elevated: rgba(255, 255, 255, 0.78);
  --bg-strong: #ffffff;
  --text-primary: #132238;
  --text-secondary: #5f6f86;
  --border-soft: rgba(148, 163, 184, 0.2);
  --brand-primary: #1f6feb;
  --brand-secondary: #13b8a6;
  --shadow-soft: 0 24px 60px rgba(15, 23, 42, 0.08);
  --shadow-card: 0 18px 40px rgba(31, 41, 55, 0.08);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
}

body {
  font-family: 'Avenir Next', 'SF Pro Display', 'PingFang SC', 'Segoe UI', sans-serif;
  color: var(--text-primary);
  background:
    radial-gradient(circle at top left, rgba(31, 111, 235, 0.14), transparent 28%),
    radial-gradient(circle at 85% 20%, rgba(19, 184, 166, 0.16), transparent 24%),
    linear-gradient(180deg, #f7f9fc 0%, #eef2f9 100%);
}

body::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.26) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.24) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.3), transparent 85%);
}

a {
  color: inherit;
}
</style>
