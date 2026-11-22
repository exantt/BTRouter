# BTRouter Brand & Editorial Design Guide

## 1. Surface Depth System (`base → recessed → elevated → floating`)
Warm studio graphite hierarchy — never sterile pitch black:
- `--surface-base: #0E1013` — warm studio graphite canvas
- `--surface-recessed: #090A0D` — recessed wells, OTP sandbox, phone screens, code blocks
- `--surface-elevated: #15181E` — primary elevated surfaces with top specular rim (`inset 0 1px 0 rgba(255, 255, 255, 0.08)`)
- `--surface-floating: #1D2129` — floating architectural dock, phone frames, active scenario stage

## 2. Custom Brand Palette & Restraint
- **Ink Primary:** `#F3F2EE` (warm editorial ivory, also used for primary CTA fill)
- **Ink Secondary:** `#A8ADB8` (readable warm slate body)
- **Ink Muted:** `#868D9B` (quiet mono index labels and metadata)
- **Client Signal Emerald:** `#26D97F` (`--emerald-soft: rgba(38, 217, 127, 0.12)`) — surgical accent only
- **Server Ice-Cyan:** `#40CCF5` (`--cyan-soft: rgba(64, 204, 245, 0.12)`) — surgical accent only
- **Alert Coral:** `#FF5E52` (`--coral-soft: rgba(255, 94, 82, 0.14)`)
- **Hairline Borders:** `rgba(243, 242, 238, 0.09)` / `--border-strong: rgba(243, 242, 238, 0.18)`

## 3. Typography Pairing
- **Modern Display (`H1`, `H2`):** `'Onest', sans-serif` (weights 600, 700), tight `-0.035em` on H1 and `-0.025em` on H2
- **UI & Subheadings (`H3`, `H4`, Body):** `'Golos Text', sans-serif` (600 with `-0.015em` for headings; 400 with `line-height: 1.68` for prose)
- **Index Kickers, Telemetry & Code:** `'JetBrains Mono', monospace` with `font-variant-numeric: tabular-nums`

## 4. Shadows, Grain & Micro-Interactions
- Multi-layered color-tinted shadows (`--shadow-elevated`, `--shadow-floating`, `--shadow-emerald-glow`, `--shadow-cyan-glow`).
- Subtle atmospheric radial stops + SVG `feTurbulence` grain overlay (`opacity: 0.032`).
- Zero `transition: all`; explicit `transform`, `opacity`, `border-color`, `background-color`, `box-shadow`, `color` with `cubic-bezier(0.22, 1, 0.36, 1)`.
- Distinct `:hover`, `:focus-visible`, and `:active` states on every interactive control.
