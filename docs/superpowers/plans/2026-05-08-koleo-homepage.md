# KOLEO Homepage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the default Angular welcome screen in `frontend/` with a KOLEO-styled homepage (hero with search bar + operators band) — UI/markup only, Polish copy, pure SCSS.

**Architecture:** Single-component implementation in the existing `App` root component. Global tokens + font in `styles.scss`, all layout/markup in `app/app.html`, all component styles in `app/app.scss`. Inline SVG icons (no library). Operator logos rendered as styled text in brand colors.

**Tech Stack:** Angular 21, SCSS, CSS custom properties, Inter font (Google Fonts).

**Spec:** `docs/superpowers/specs/2026-05-08-koleo-homepage-design.md`

**Working directory for all commands:** `C:\Users\crulp\Zako\frontend`

**Verification model:** Static markup with no logic — there is no unit-test to write per task. Verification is visual: after each commit, the dev server (`ng serve`) must build clean (no errors/warnings). The final task does a browser check at 1440 / 1024 / 768 / 375 px against the reference screenshots.

---

## Task 0: Install dependencies and confirm baseline build

**Files:** none modified

- [ ] **Step 1:** Install npm deps

```powershell
cd C:\Users\crulp\Zako\frontend
npm install
```

Expected: completes without errors.

- [ ] **Step 2:** Confirm the default app builds clean

```powershell
npm run build
```

Expected: `Application bundle generation complete` and no errors. (Warnings about budget on the placeholder are OK — we are about to replace that file.)

- [ ] **Step 3:** No commit (no source change yet).

---

## Task 1: Global tokens, reset, and Inter font

**Files:**
- Modify: `frontend/src/index.html`
- Modify: `frontend/src/styles.scss`

- [ ] **Step 1:** Replace `index.html` with:

```html
<!doctype html>
<html lang="pl">
<head>
  <meta charset="utf-8">
  <title>KOLEO – Rozkład jazdy PKP i bilety</title>
  <base href="/">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="icon" type="image/x-icon" href="favicon.ico">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
</head>
<body>
  <app-root></app-root>
</body>
</html>
```

- [ ] **Step 2:** Overwrite `styles.scss` with tokens + reset:

```scss
:root {
  --koleo-navy: #1a1854;
  --koleo-navy-deep: #13123f;
  --koleo-pink: #e91e63;
  --koleo-pink-hover: #c2185b;
  --koleo-teal: #3ed3c4;
  --koleo-bg: #f4f5f9;
  --koleo-input-bg: #ffffff;
  --koleo-input-border: #e0e0e8;
  --koleo-muted: #7a7d99;
  --koleo-text: #1a1854;
}

*, *::before, *::after { box-sizing: border-box; }

html, body {
  margin: 0;
  padding: 0;
  font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: var(--koleo-text);
  background: var(--koleo-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

button { font-family: inherit; }
input  { font-family: inherit; }
```

- [ ] **Step 3:** Build and verify

```powershell
npm run build
```

Expected: build succeeds. (Component-style budget warnings about the placeholder `app.scss` are still OK — Task 2 replaces it.)

- [ ] **Step 4:** Commit

```powershell
git add frontend/src/index.html frontend/src/styles.scss
git commit -m "feat(frontend): add Inter font and KOLEO design tokens"
```

---

## Task 2: Strip the Angular welcome placeholder

**Files:**
- Modify: `frontend/src/app/app.html`
- Modify: `frontend/src/app/app.scss`
- Modify: `frontend/src/app/app.ts`

- [ ] **Step 1:** Replace `app/app.html` entirely with a minimal scaffold:

```html
<router-outlet />

<main class="page">
  <!-- hero will go here in Task 3 -->
  <!-- operators band will go here in Task 4 -->
</main>
```

- [ ] **Step 2:** Replace `app/app.scss` with a minimal stub:

```scss
.page {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}
```

- [ ] **Step 3:** Update `app/app.ts` — remove the `title` signal (template no longer uses it):

```ts
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
```

- [ ] **Step 4:** Update `app/app.spec.ts` if it references `title()` — open the file and replace its body with:

```ts
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [App] }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
```

- [ ] **Step 5:** Build and verify

```powershell
npm run build
```

Expected: build succeeds with no errors and no budget warnings (placeholder is gone).

- [ ] **Step 6:** Commit

```powershell
git add frontend/src/app/app.html frontend/src/app/app.scss frontend/src/app/app.ts frontend/src/app/app.spec.ts
git commit -m "chore(frontend): remove Angular welcome placeholder"
```

---

## Task 3: Hero section — topbar, title, and search form

**Files:**
- Modify: `frontend/src/app/app.html`
- Modify: `frontend/src/app/app.scss`

- [ ] **Step 1:** Replace `app/app.html` with the full hero markup. The `<router-outlet />` stays at the top so it doesn't break Angular's expectations even though no routes are configured.

```html
<router-outlet />

<main class="page">
  <section class="hero">
    <div class="hero__inner">
      <header class="hero__topbar">
        <div class="logo-group">
          <a class="logo" href="/" aria-label="KOLEO – strona główna">
            <svg class="logo__mark" viewBox="0 0 32 32" aria-hidden="true">
              <path d="M6 4 L24 16 L6 28 Z" fill="#3ed3c4"/>
              <path d="M14 12 L26 20 L14 28 Z" fill="#ffffff"/>
            </svg>
            <span class="logo__word">KOLEO</span>
          </a>
          <p class="logo__tagline">ROZKŁAD JAZDY PKP I BILETY</p>
        </div>
        <div class="hero__actions">
          <button type="button" class="login-btn">
            <span>Zaloguj się</span>
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
              <circle cx="12" cy="8" r="4" fill="none" stroke="currentColor" stroke-width="1.8"/>
              <path d="M4 21c1.5-4 5-6 8-6s6.5 2 8 6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </button>
          <button type="button" class="menu-btn" aria-label="Menu">
            <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
              <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </header>

      <h1 class="hero__title">Dokąd jedziemy?</h1>

      <form class="search" (submit)="$event.preventDefault()">
        <label class="search__field">
          <svg class="search__icon-leading" viewBox="0 0 24 24" aria-hidden="true">
            <rect x="6" y="4" width="12" height="14" rx="3" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <path d="M9 19l-2 2M15 19l2 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M3 11h3M18 11h3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <input type="text" placeholder="Stacja odjazdu" />
          <svg class="search__icon-trailing" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 21s7-7.5 7-12a7 7 0 1 0-14 0c0 4.5 7 12 7 12z" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <circle cx="12" cy="9" r="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/>
          </svg>
        </label>

        <label class="search__field">
          <svg class="search__icon-leading" viewBox="0 0 24 24" aria-hidden="true">
            <rect x="6" y="4" width="12" height="14" rx="3" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <path d="M9 19l-2 2M15 19l2 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M3 11h3M18 11h3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <input type="text" placeholder="Stacja przyjazdu" />
          <svg class="search__icon-trailing" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </label>

        <label class="search__field">
          <svg class="search__icon-leading" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <path d="M12 7v5l3 2" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <input type="text" value="8 maja, 17:15" readonly />
          <svg class="search__icon-trailing" viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="5" width="18" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.8"/>
            <path d="M3 10h18M8 3v4M16 3v4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <circle cx="8" cy="14" r="1" fill="currentColor"/>
            <circle cx="12" cy="14" r="1" fill="currentColor"/>
            <circle cx="16" cy="14" r="1" fill="currentColor"/>
            <circle cx="8" cy="17" r="1" fill="currentColor"/>
            <circle cx="12" cy="17" r="1" fill="currentColor"/>
          </svg>
        </label>

        <button type="submit" class="search__cta">
          <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="2"/>
            <path d="M16 16l4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span>Znajdź połączenie</span>
        </button>
      </form>
    </div>
  </section>

  <!-- operators band will go here in Task 4 -->
</main>
```

- [ ] **Step 2:** Replace `app/app.scss` with hero styles:

```scss
.page {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}

.hero {
  background: var(--koleo-navy);
  color: #fff;
  padding-bottom: 64px;
}

.hero__inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
}

.hero__topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 32px 0 24px;
  gap: 24px;
}

.logo-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #fff;
}

.logo__mark {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.logo__word {
  font-size: 40px;
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 1;
}

.logo__tagline {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.15em;
  color: rgba(255, 255, 255, 0.78);
}

.hero__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.login-btn,
.menu-btn {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: transparent;
  border: 1.5px solid rgba(255, 255, 255, 0.35);
  color: #fff;
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.login-btn:hover,
.menu-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.6);
}

.menu-btn {
  padding: 10px;
}

.hero__title {
  margin: 56px 0 28px;
  font-size: 56px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.01em;
}

.search {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 12px;
  background: transparent;
}

.search__field {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--koleo-input-bg);
  border-radius: 10px;
  height: 64px;
  padding: 0 16px;
  gap: 12px;
  color: var(--koleo-text);
  cursor: text;
}

.search__field input {
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 17px;
  color: var(--koleo-text);
  min-width: 0;
}

.search__field input::placeholder {
  color: var(--koleo-muted);
}

.search__icon-leading,
.search__icon-trailing {
  width: 22px;
  height: 22px;
  color: var(--koleo-muted);
  flex-shrink: 0;
}

.search__cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 64px;
  padding: 0 28px;
  background: var(--koleo-pink);
  color: #fff;
  border: 0;
  border-radius: 10px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.search__cta:hover {
  background: var(--koleo-pink-hover);
}
```

- [ ] **Step 3:** Build and verify

```powershell
npm run build
```

Expected: build succeeds.

- [ ] **Step 4:** Commit

```powershell
git add frontend/src/app/app.html frontend/src/app/app.scss
git commit -m "feat(frontend): add KOLEO hero with topbar, title and search form"
```

---

## Task 4: Operators band

**Files:**
- Modify: `frontend/src/app/app.html`
- Modify: `frontend/src/app/app.scss`

- [ ] **Step 1:** In `app/app.html`, replace the comment `<!-- operators band will go here in Task 4 -->` with this section:

```html
<section class="operators">
  <div class="operators__inner">
    <h2 class="operators__title">Wszystkie bilety w jednym miejscu</h2>
    <ul class="operators__grid" role="list">
      <li class="op op--pkp">PKP <span>INTERCITY</span></li>
      <li class="op op--polregio">PolREGIO</li>
      <li class="op op--flixbus">FLIXBUS</li>
      <li class="op op--arriva">arriva</li>
      <li class="op op--kw">Koleje<br/>Wielkopolskie</li>
      <li class="op op--kd">Koleje Dolnośląskie</li>
      <li class="op op--ksl">Koleje Śląskie</li>
      <li class="op op--km">Koleje<br/>Mazowieckie</li>
      <li class="op op--kmal">Koleje<br/>Małopolskie</li>
      <li class="op op--skm">SKM</li>
      <li class="op op--lka">ŁKA</li>
      <li class="op op--leo">Leo Express</li>
    </ul>
  </div>
</section>
```

- [ ] **Step 2:** Append these styles to `app/app.scss`:

```scss
.operators {
  background: var(--koleo-bg);
  padding: 72px 0 96px;
}

.operators__inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 32px;
}

.operators__title {
  margin: 0 0 56px;
  font-size: 28px;
  font-weight: 600;
  text-align: center;
  color: var(--koleo-navy);
}

.operators__grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  row-gap: 56px;
  column-gap: 32px;
  align-items: center;
  justify-items: center;
}

.op {
  font-weight: 700;
  font-size: 20px;
  text-align: center;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.op span { font-weight: 800; }

.op--pkp       { color: #ed1c24; }
.op--pkp span  { color: #1f3a93; margin-left: 6px; }
.op--polregio  { color: #d6202f; }
.op--flixbus   { color: #73d700; }
.op--arriva    { color: #00b8d4; }
.op--kw        { color: #b71c1c; }
.op--kd        { color: #1a1a1a; }
.op--ksl       { color: #f9a825; }
.op--km        { color: #f4b400; }
.op--kmal      { color: #2e7d32; }
.op--skm       { color: #00bfa5; }
.op--lka       { color: #c62828; }
.op--leo       { color: #fdd835; background: #1a1a1a; padding: 6px 14px; border-radius: 4px; }
```

- [ ] **Step 3:** Build and verify

```powershell
npm run build
```

Expected: build succeeds.

- [ ] **Step 4:** Commit

```powershell
git add frontend/src/app/app.html frontend/src/app/app.scss
git commit -m "feat(frontend): add operators band with brand-colored logos"
```

---

## Task 5: Responsive breakpoints

**Files:**
- Modify: `frontend/src/app/app.scss`

- [ ] **Step 1:** Append responsive rules to the end of `app/app.scss`:

```scss
@media (max-width: 1023px) {
  .search {
    grid-template-columns: 1fr 1fr;
  }
  .search__cta {
    grid-column: 1 / -1;
  }
  .operators__grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .hero__title {
    font-size: 44px;
  }
}

@media (max-width: 767px) {
  .hero__inner,
  .operators__inner {
    padding: 0 20px;
  }
  .hero__topbar {
    padding: 20px 0 16px;
  }
  .logo__word {
    font-size: 32px;
  }
  .logo__tagline {
    display: none;
  }
  .login-btn span {
    display: none;
  }
  .login-btn {
    padding: 10px;
  }
  .hero__title {
    font-size: 32px;
    margin: 32px 0 20px;
  }
  .search {
    grid-template-columns: 1fr;
  }
  .search__cta {
    grid-column: auto;
    width: 100%;
  }
  .operators {
    padding: 48px 0 64px;
  }
  .operators__title {
    font-size: 22px;
    margin-bottom: 32px;
  }
  .operators__grid {
    grid-template-columns: repeat(2, 1fr);
    row-gap: 40px;
  }
  .op {
    font-size: 16px;
  }
}
```

- [ ] **Step 2:** Build and verify

```powershell
npm run build
```

Expected: build succeeds.

- [ ] **Step 3:** Commit

```powershell
git add frontend/src/app/app.scss
git commit -m "feat(frontend): add responsive breakpoints for KOLEO homepage"
```

---

## Task 6: Final visual verification in dev server

**Files:** none modified (verification only)

- [ ] **Step 1:** Start the dev server in the background

```powershell
cd C:\Users\crulp\Zako\frontend
npm start
```

Expected: server boots and prints `Local: http://localhost:4200/`. (Use `run_in_background` in the agent harness so it stays running.)

- [ ] **Step 2:** Open `http://localhost:4200/` in a browser. Verify against the reference screenshots:
  - Hero is dark navy, full-width
  - "KOLEO" wordmark is white, 40px, with a teal triangle/chevron mark to its left
  - Tagline "ROZKŁAD JAZDY PKP I BILETY" sits directly under the wordmark
  - "Zaloguj się" button (top right) and burger button next to it, both outlined in semi-transparent white
  - Heading "Dokąd jedziemy?" is white, large, left-aligned
  - Search form: 3 white pill inputs + pink "Znajdź połączenie" button, all in one row
  - Light gray section below: "Wszystkie bilety w jednym miejscu" centered, 12 brand-colored operator logos in a 4-column grid
  - Polish diacritics render correctly: ą, ż, ś, ł, ó

- [ ] **Step 3:** Resize the browser to verify breakpoints
  - 1440px → as designed (4 search cells, 4-col operators)
  - 1024px → search 2+1 (CTA full row), operators 3-col
  - 768px → search 1-col stacked, operators 2-col
  - 375px (mobile) → tagline hidden, "Zaloguj się" text hidden (icon only), title 32px

- [ ] **Step 4:** Stop the dev server.

- [ ] **Step 5:** No commit (verification only). If any breakpoint or visual issue was found, go back to the relevant task, fix, and commit on that fix.

---

## Done criteria

- All 6 tasks committed
- `npm run build` clean (no errors, no budget warnings)
- Visual match at all four reference widths
- No backend, no routing, no logic added (consistent with spec scope)
