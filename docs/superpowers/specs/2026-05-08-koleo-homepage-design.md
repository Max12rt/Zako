# KOLEO-Style Homepage — Design Spec

**Date:** 2026-05-08
**Scope:** `frontend/` (Angular 21)
**Goal:** Replace the default Angular welcome screen with a 1:1-styled clone of the [koleo.pl](https://koleo.pl) homepage hero + operator-logos band. UI/markup only — no functionality, no backend integration.

## Reference

User-provided screenshots of the live KOLEO homepage:
- Hero with dark navy background, search bar, pink CTA
- Light section with operator-logo grid

## Out of Scope

Explicitly excluded for this iteration (may come later):
- The `Zaloguj się` dropdown / mobile slide-out menu (second screenshot)
- Routing — stays as the empty `routes = []` from CLI
- Search functionality, station autocomplete, date picker behavior
- Backend integration with the Spring Boot services
- Any pages other than `/`

## Visual Tokens

Defined as CSS custom properties on `:root` in `frontend/src/styles.scss`.

| Token | Value | Use |
|---|---|---|
| `--koleo-navy` | `#1a1854` | Hero background, primary text |
| `--koleo-navy-deep` | `#13123f` | Subtle shadow under hero (optional) |
| `--koleo-pink` | `#e91e63` | CTA button |
| `--koleo-pink-hover` | `#c2185b` | CTA hover |
| `--koleo-teal` | `#3ed3c4` | Logo accent mark, language switcher |
| `--koleo-bg` | `#f4f5f9` | Operators-band background |
| `--koleo-input-bg` | `#ffffff` | Search inputs |
| `--koleo-input-border` | `#e0e0e8` | Input dividers |
| `--koleo-muted` | `#7a7d99` | Placeholder, secondary icons |

Typography:
- Family: `'Inter', system-ui, sans-serif` (loaded from Google Fonts in `index.html`)
- Hero heading `Dokąd jedziemy?`: 56px / 600 weight / white
- Logo wordmark `KOLEO`: 40px / 700 weight / white, letter-spacing 0.05em
- Tagline `ROZKŁAD JAZDY PKP I BILETY`: 13px / 500 / white at 80% opacity, letter-spacing 0.15em
- Section heading `Wszystkie bilety w jednym miejscu`: 28px / 600 / `--koleo-navy`
- Input text / placeholder: 16px / 400

## Layout Structure

```
<app-root>
  <section class="hero">
    <header class="hero__topbar">
      <div class="logo-group">
        <div class="logo">…SVG accent + KOLEO wordmark…</div>
        <p class="logo__tagline">ROZKŁAD JAZDY PKP I BILETY</p>
      </div>
      <div class="hero__actions">
        <button class="login-btn">Zaloguj się <icon-user/></button>
        <button class="menu-btn"><icon-burger/></button>
      </div>
    </header>

    <h1 class="hero__title">Dokąd jedziemy?</h1>

    <form class="search">
      <div class="search__field">
        <icon-train-out/>
        <input placeholder="Stacja odjazdu" />
        <icon-pin/>
      </div>
      <div class="search__field">
        <icon-train-in/>
        <input placeholder="Stacja przyjazdu" />
        <icon-chevron/>
      </div>
      <div class="search__field">
        <icon-clock/>
        <input value="8 maja, 17:15" readonly />
        <icon-calendar/>
      </div>
      <button class="search__cta">
        <icon-search/> Znajdź połączenie
      </button>
    </form>
  </section>

  <section class="operators">
    <h2>Wszystkie bilety w jednym miejscu</h2>
    <div class="operators__grid">
      <!-- 12 operator logos -->
    </div>
  </section>
</app-root>
```

## Layout Rules

**Hero:**
- Full-width, dark navy background
- Inner content max-width 1200px, centered, horizontal padding 32px
- Topbar (`.hero__topbar`): `.logo-group` on the left (logo wordmark + tagline stacked vertically), `.hero__actions` on the right; flex row with `justify-content: space-between`, padding 32px 0
- Title `Dokąd jedziemy?` sits below the topbar, left-aligned, with 56px top margin
- Search form: 4-column grid (`1fr 1fr 1fr auto`) with 12px gap; each field is white, rounded 8px, 56px tall, with leading + trailing icons inside
- CTA button: pink, rounded 8px, 56px tall, padding 0 28px, white text, magnifier icon

**Operators band:**
- Light bg `--koleo-bg`, vertical padding 64px
- Inner max-width 1100px centered
- 4-column grid for logos, 32px gap, logos vertically centered, max-height 60px
- On smaller screens, drop to 3 → 2 columns

**Responsive breakpoints:**
- ≥1024px: as designed above
- 768–1023px: search becomes 2 columns + CTA on its own row, operators 3 columns
- <768px: search stacks to 1 column, CTA full-width; operators 2 columns; topbar stays horizontal but logo tagline hides; hero title 36px

## Logo & Operator Assets

- **KOLEO logo:** approximated with custom inline SVG — a teal chevron-arrow accent (▶/) above white "KOLEO" wordmark. No raster image.
- **Operator logos:** for this iteration, render as styled text in each brand's color (e.g. PKP Intercity in red/blue, FlixBus in green, Koleje Śląskie etc.). This avoids copyright concerns and missing assets. A later iteration can swap in real SVGs.
- **Inline icons** (search, train, clock, etc.): hand-rolled inline SVGs, 20px stroke-based, `currentColor` for theming. No icon library dependency.

## Files Changed

- `frontend/src/index.html` — add Google Fonts link for Inter, update `<title>`
- `frontend/src/styles.scss` — global reset, body bg, font, design tokens on `:root`
- `frontend/src/app/app.html` — replace placeholder with KOLEO markup above
- `frontend/src/app/app.scss` — all component styles
- `frontend/src/app/app.ts` — keep as-is (no logic needed); remove `title` signal usage from template

## Acceptance Criteria

- Visual match to provided screenshots at 1440px viewport: navy hero, white logo with teal accent, pink CTA, light operator band
- All Polish copy renders correctly (UTF-8 diacritics: ą, ż, ś, ł)
- Keyboard tab order works through inputs and CTA
- Layout doesn't break at 1440px / 1024px / 768px / 375px
- No Angular CLI build warnings
- `ng serve` boots and shows the new homepage at `/`
