# Design Guidelines

Design system extracted from <https://devpowers.com/> on 2026-11-25 with the `create-design-system` skill (Playwright CLI, headed Chrome, 920px viewport). The machine-readable source of truth is [`assets/design-tokens.json`](../assets/design-tokens.json).

## Assets

| File | Description |
|---|---|
| [`../assets/homepage.png`](../assets/homepage.png) | Full-page screenshot of the reference homepage (light theme, no overlays) |
| [`../assets/logo.svg`](../assets/logo.svg) | DevPowers wordmark + hero mascot, inline SVG, `fill="currentColor"` |
| [`../assets/favicon.svg`](../assets/favicon.svg) | Mascot icon, `#004581`, switches to white under `prefers-color-scheme: dark` |
| [`../assets/design-tokens.json`](../assets/design-tokens.json) | Colors, light/dark themes, typography, spacing, radii, shadows, components |

> Logo, favicon and the DevPowers name are brand assets. Verify usage rights before using or redistributing them.

## Colors

The site is built on CSS custom properties; reuse the same names so a dark theme is a single class switch (`html.dark`).

### Light theme (default)

| Token | Hex | Usage |
|---|---|---|
| `--surface` | `#fff9eb` | Page background (warm cream paper) |
| `--surface-raised` | `#ffffff` | Panels, cards, light buttons, alternating "raised" sections |
| `--surface-strong` | `#004581` | Blue bands, icon boxes, brand secondary |
| `--on-surface` | `#101820` | Body text, headings, all outlines and shadows (ink) |
| `--on-surface-strong` | `#ffffff` | Text on blue bands / footer |
| `--muted` | `#4c5b66` | Secondary text, timestamps |
| `--accent` | `#d30f10` | Primary CTA, highlighted headline word ("SUPERPOWERS."), negative state |
| `--on-accent` | `#ffffff` | Text on accent |
| `--accent-warm` | `#ffd447` | Stickers/badges, active tab, warm button |
| `--on-accent-warm` | `#101820` | Text on warm accent |
| `--positive` | `#167a3e` | Success checks |
| `--chat-client` | `#fff1b8` | Client chat bubble |
| `--chat-agent` | `#d9ecff` | Agent chat bubble |
| `--soft-red` / `--soft-blue` | `#fff0ed` / `#eaf5ff` | Tinted card backgrounds |

Additional section colors: electric blue `#038bfe` ("villain" band with halftone dots), deep blue `#003761` (band variant), footer navy `#002d52`.

### Dark theme (`html.dark`)

| Token | Hex |
|---|---|
| `--surface` | `#17121d` |
| `--surface-raised` | `#241c2e` |
| `--surface-strong` | `#0d3b66` |
| `--on-surface` / `--hairline` | `#f7efe1` |
| `--muted` | `#c3b4a6` |
| `--accent` | `#ff3b30` (text on it: `#1a0d0c`) |
| `--accent-warm` | `#ffd233` |
| `--positive` / `--negative` | `#6bdd97` / `#ff5a4d` |
| `--chat-client` / `--chat-agent` | `#4a3a12` / `#14395c` |
| `--shadow` | `#000000` |

In dark mode outlines become cream (`--hairline`) while hard shadows become black.

## Typography

| Role | Family | Notes |
|---|---|---|
| Display (headings, buttons, stickers, labels) | **Bangers**, sans-serif | Comic all-caps face, weight 400, letter-spacing ≈ 0.02em |
| Body, nav, UI text | **Atkinson Hyperlegible**, sans-serif | Weights 400 and 700, accessibility-focused |

Both fonts load from Google Fonts (third-party CDN, not self-hosted, so no font files were downloaded):

```html
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:wght@400;700&family=Bangers&display=swap">
```

Both families are published under the SIL Open Font License on Google Fonts; confirm the license for your use case.

| Token | Size / line height | Usage |
|---|---|---|
| `h1` | ~55px / 1.0 (fluid) | Hero headline; one word may be accent red, one outlined |
| `h2` | 28px / 32px | Section titles |
| `h3` | 29.6px / 1.08 | Card titles |
| `lead` | 21.6px / 1.625 | Hero intro paragraph |
| `button` | 20px / 1 | Bangers, all buttons |
| `base` | 16px / 1.65 | Body copy |
| `sm` | 14px / 20px, 700 | Navigation links |

## Spacing

Base unit **4px** (Tailwind scale). Common values: 8, 12, 16, 24, 32, 48px. Sections use ~64px vertical padding (`pt-14 md:pt-24` for the hero); the footer uses 48px. Header inner content is inset 16px on mobile.

## Border radius and outlines

| Token | Value | Usage |
|---|---|---|
| `sm` | 6px | Small chips |
| `button` | 7px | All buttons |
| `md` | 8px | Icon boxes |
| `lg` | 10px | Panels, cards, stickers (most common) |
| `xl` | 16px | Large media frames |
| `bubble` | 14px 14px 14px 4px | Chat bubbles (one sharp corner = tail side) |
| `circle` | 50% | Avatars, numbered tabs |

**Signature style:** every interactive or framed element has a **3px solid ink border** (`--hairline`) and a **hard offset shadow with no blur**: `3px 3px 0` (stickers, tabs), `4px 4px 0` (buttons), `6px 6px 0` (panels). Never use soft/blurred shadows.

## Components

- **Header** — sticky, 76px tall, same background as the page (`--surface`). Left: logo (158px wide). Right: icon buttons (theme toggle, palette) as light buttons, red "Get a quote" primary button, hamburger menu.
- **Nav links** — Atkinson Hyperlegible 14px bold, ink color, no uppercase transform.
- **Buttons** (`.btn`) — Bangers 20px, padding `10.4px 16px`, 3px ink border, radius 7px, shadow `4px 4px 0` ink, optional leading Lucide icon.
  - `.btn-primary` — red `#d30f10`, white text (main CTA).
  - `.btn-light` — white, ink text (secondary).
  - `.btn-warm` — yellow `#ffd447`, ink text.
- **Sticker** (`.sticker`) — yellow badge, Bangers, 3px border, radius 10px, shadow 3px, `rotate(-1deg)`. Used for eyebrows like "AI-first web studio" and corner labels like "5 working days".
- **Panel** (`.panel`) — white card, 3px border, radius 10px, shadow `6px 6px 0`. Slight rotations (±1–3°) are used for a hand-placed comic feel.
- **Icon box** (`.icon-box`) — 42×42px, blue `--surface-strong`, white icon, 3px border, radius 8px.
- **Band** (`.band`) — full-width blue section with white text and 3px ink borders top and bottom; variants add a halftone dot pattern.
- **Speech-bubble cards** — feature cards with a small tail at the bottom, Bangers title + body text.
- **Chat bubbles** — client `#fff1b8`, agent `#d9ecff`, with timestamps in `--muted`; check-list chips use `--positive` icons.
- **CTA section** — red background with yellow conic "rays" pattern and white Bangers headline.
- **Footer** — navy `#002d52`, white text, 48px vertical padding.
- **Icons** — [Lucide](https://lucide.dev/) outline icons, 24px, stroke inherits text color.

No form inputs were present on the homepage; for inputs follow the same pattern (white background, 3px ink border, radius 8px, 16px Atkinson text, `4px 4px 0` shadow on focus).

## Logo usage

- Use [`assets/logo.svg`](../assets/logo.svg) in the header top-left at ~158px wide (height ≈ 35px), linked to home.
- The SVG uses `fill="currentColor"`: set `color: var(--on-surface)` on light backgrounds; on dark backgrounds or blue bands set `color: #fff` (or `var(--on-surface)` in dark theme). No separate inverted file is needed.
- Use [`assets/favicon.svg`](../assets/favicon.svg) as `<link rel="icon" type="image/svg+xml">`; it adapts to dark mode automatically.
- Keep clear space of at least the mascot head height around the logo; do not add borders or shadows to it.

## Visual style summary

DevPowers uses a bold **comic-book / superhero** aesthetic: cream paper background, thick black ink outlines, hard offset shadows and slightly rotated stickers and panels. The palette is a primary-color trio — hero red for action, sunny yellow for highlights and labels, deep blue for bands and trust — on top of near-black ink. Bangers display type gives headlines and buttons energy, while Atkinson Hyperlegible keeps body text calm and highly readable. Halftone dots and sunburst rays add texture to section breaks. The tone is playful and confident, but the layout stays clean, accessible and supports a full dark theme.
