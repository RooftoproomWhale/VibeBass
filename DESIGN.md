# VibeBass Design System

## 1. Visual Theme & Atmosphere

A warm practice desk: quiet surfaces, clear controls and an uninterrupted score. The interface is a working instrument. Its memorable details are restrained rust-colored actions and a large, steady time display. Real score pages and the actual video provide the imagery.

## 2. Color Palette & Roles

- Desk (#F4F2EC): application background and score surround.
- Surface (#FCFBF8): controls, toolbars and empty states.
- Paper (#FFFFFF): actual PDF pages.
- Ink (#242923): headings, primary text and neutral primary controls.
- Muted (#656B61): supporting copy and inactive labels.
- Divider (#DDDCD3): decorative separation; use Ink or Muted for interactive outlines.
- Rust (#A33F24): primary upload/save actions and selected states.

Errors use Rust plus explicit text. Do not introduce another accent. Opacity is allowed for disabled states, decorative borders and subtle selection fills. Kotlin tokens live in `PracticeTheme.kt`; equivalent browser tokens live in `styles.css`.

## 3. Typography Rules

| Role | Font | Size | Weight | Line Height | Letter Spacing | Features | Notes |
|---|---|---|---|---|---|---|---|
| Session heading | Noto Sans KR / system sans | 28px desktop, 24px compact | 700 | 1.4 | -0.02em | none | One line with overflow handling |
| Brand / pane heading | Noto Sans KR / system sans | 20px | 700 | 1.4 | -0.02em | none | Retain VibeBass Studio |
| Body | Noto Sans KR / system sans | 16px | 400 | 1.7 | 0 | none | Korean text remains readable |
| Control | Noto Sans KR / system sans | 14px | 600 | 1.5 | 0 | none | 48px minimum target height |
| Metadata | Noto Sans KR / system sans | 12px | 400 | 1.7 | 0 | none | Optional supporting information |
| Time | system monospace | 32px | 500 | 1.2 | -0.02em | tabular numbers | Minutes and seconds |

Reuse the bundled Noto Sans KR font. No remote font or icon dependency. Use labels for actions; do not substitute arbitrary emoji or tiny abbreviations.

## 4. Component Stylings

- Buttons: 48px minimum height, 8px radius, 16px horizontal padding. Primary Rust/Paper; secondary Surface/Ink with Muted outline. Material interaction feedback and visible focus outline; no motion-dependent meaning.
- Containers: flat Surface, 1px Divider, 16px or 24px padding. No nested decorative cards. Only the score page has a subtle downward shadow.
- State indicators: plain text plus a small accent marker when needed. Selection also changes weight or adds an underline.
- Inputs: 56px minimum height, persistent visible label, accessible error text. Avoid adding forms that have no backend or user need.
- Navigation: 64px header. Compact tab targets are at least 48px high. Tabs have selected semantics.
- Lists: lazy rows, 16px spacing/padding, truncated long titles, full labels for delete actions. Undo is available after removing sync points.
- Empty states: one clear heading, one short instruction and a working action. Never render invented songs or sync points as real data.

## 5. Layout Principles

Spacing scale: 4, 8, 12, 16, 24, 32, 48, 64px. The desktop control rail is 344px; the score consumes the remaining width. At 900px and below, switch to score/control tabs. Each pane owns its scroll region. Keep the save action and feedback in Compose space, outside the PDF/iframe rectangle. All interactive shapes use 8px radii; structural panes remain square.

## 6. Depth & Elevation

Flat controls; score shadow only. Compose is the base layer; browser media overlays are a single small, defined layer above it. Their bounds come from the measured Compose component, including clipping and pixel density. Never use large z-index values to hide geometry bugs. No blur or glass surfaces.

## 7. Do's and Don'ts

- DO prioritize reading space. DON'T insert a landing page, metrics, testimonial or stock photo.
- DO preserve the existing Compose/KMP stack. DON'T install a UI framework for this redesign.
- DO use one Rust accent. DON'T reintroduce neon gradients or fake album art.
- DO expose useful empty, loading, retry and save states. DON'T claim a file is loaded when only sync metadata exists.
- DO keep raw PDF scroll coordinates consistent. DON'T subtract a fixed guide-line offset in playback.
- DO use real media and stable page order. DON'T let a previous PDF request populate a new session.

## 8. Responsive Behavior

- 320-767px: compact header and heading; full-width score/control tabs, one visible workspace pane. Long filenames truncate. All primary targets stay reachable.
- 768-899px: the same compact navigation with more generous pane width.
- 900px+: control rail and score visible together, 16-24px gutters.
- 1280px+: wider reading surface; do not stretch buttons or add empty panels.
- Hidden browser media has no visible or clickable rectangle. Video state survives compact navigation where possible.
- No decorative motion. Native media respects its own controls; scroll transitions respect reduced-motion settings.
- Source-level responsive review is required. Browser checks of the bridge do not substitute for a compiled Compose viewport review.

## 9. Agent Prompt Guide

### Quick Color Reference

Background #F4F2EC; surface #FCFBF8; paper #FFFFFF; text #242923; muted #656B61; divider #DDDCD3; action/selection #A33F24.

### Example Component Prompts

1. Build the VibeBass Studio header in existing Compose Material 3: 64px height, #FCFBF8 surface, #242923 wordmark at 20px/700, 24px desktop or 16px compact horizontal padding, 1px #DDDCD3 lower divider.
2. Build a score empty state on #F4F2EC with a 24px/700 Korean heading, 16px supporting text in #656B61 at 1.7 line height, and one 48px-high #A33F24 upload button with white text and 8px corners. Use no fake score screenshot.
3. Build a 344px practice control rail on #FCFBF8, 16px padding, real YouTube content, 32px monospace timer, 48px mode controls and a lazy sync list. Keep the save action outside the video rectangle.
4. Build saved-song rows with a full-row click target, 16px title, 12px supporting artist and sync count, 16px padding and selection expressed by text and a Rust marker. Keep the existing list label and show an explicit empty/retry state.
5. Build a score toolbar at 56px with a 16px heading, visible document state and a secondary 48px upload action. Its PDF rectangle must start below the toolbar and end above feedback.
6. Build compact score/control tabs with 48px targets, 14px labels, selected semantics and Rust underline. Keep application state above both panes and update native media visibility from actual bounds.

### Iteration Guide

1. Keep the score as the largest content area.
2. Use only the seven semantic colors; native YouTube/PDF content is exempt.
3. Use 8px corners for controls and square structural panes.
4. Reuse Material 3 and the bundled Noto Sans KR font.
5. Korean body text is 16px with 1.7 line height; never compress actions into tiny labels.
6. Handle long titles and 320px layouts before adding detail.
7. Do not build or compile without an explicit user request.
8. Report browser mocks, source inspection and real compiled-app checks separately.
