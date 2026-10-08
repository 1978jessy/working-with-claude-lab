# TODO-231: Light/dark theme toggle in the header

**Type:** Feature
**Area:** Frontend
**Priority:** Medium

## Story

As a member of the Marlowe & Finch operations team, I want to switch the dashboard
between a light and a dark theme, so that it is readable on the wall screen in the
warehouse office at night without lighting up the room, and still crisp in the
daytime.

The dashboard stays open all day on that screen and on people's second monitors.
Today it is light only, with colours hardcoded in `style.css`, including the fills
and label colours of the two SVG charts.

## Acceptance criteria

- **AC-1** There is a toggle button in the header with the id `theme-toggle`.
  Clicking it switches between the light and the dark theme. The button label
  (or icon) makes it clear which theme you will get when you click.
- **AC-2** The theme is applied through a `data-theme` attribute on the `<html>`
  element (`data-theme="light"` or `data-theme="dark"`) and CSS variables. No
  colour is duplicated in JavaScript. Both SVG charts (`#chart-on-time` and
  `#chart-tickets`) follow the theme: bars, labels and values must stay readable
  in the dark theme.
- **AC-3** The choice is persisted in `localStorage` and restored on load, so a
  refresh keeps the theme the user picked.
- **AC-4** With no stored choice, the dashboard opens in the dark theme. The OS
  setting (`prefers-color-scheme`) is ignored. A stored choice still wins (AC-3).

Fences:

- Both test suites stay green (`./mvnw test` and `npm test`).
- Frontend only: no Java changes.
- No new dependencies (no npm packages, no Maven dependencies).

## Open questions

- ~~Default theme when nothing is stored: light, dark, or follow the OS setting?~~
  Decided: dark, OS setting ignored. See AC-4.

## Definition of done

- Run `./mvnw test` and `npm test` and report both counts.
- New element ids are registered in `src/test/javascript/setup/loadApp.js`.
- Restart the app so the reviewer can click it.
