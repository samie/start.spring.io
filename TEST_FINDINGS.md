# Vaadin UI — UX findings

Findings from the Playwright validation suite (`start-site/e2e/`) and browser-driven
exploration against the server-side Vaadin UI (`io.spring.start.site.ui`).

Updated on **2026-07-16** against the app running in dev mode
(`cd start-site && ../mvnw spring-boot:run`) at `http://localhost:8080/ui/` on
**Vaadin 25.2.3**. Playwright / Chromium, single worker.

**All seven findings previously recorded here have been fixed** as part of the Vaadin
25.2.3 upgrade work, and each is now guarded by a Playwright test. Suite result:
**15/15 passing**, with no `test.fail()` annotations remaining.

---

## Open findings

None. See "Resolved" below.

---

## Resolved

### 1. Dependency search: no-results state showed a stale row — was High
When a query matched nothing, the `VirtualList` did not clear — the last matching row
stayed on screen next to a non-matching query.
- Fix: `DependencyPickerDialog.refresh(...)` now hides the list and shows an explicit
  "No matching dependencies" placeholder `Span` when the filtered set is empty.
- Guard: `dependencies.spec.js` → *"search with no matches shows an empty-state
  placeholder"* (the former `test.fail()` guard was converted to assert correct
  behavior).

### 2. Picker reset the filter after each toggle — was Medium
Clicking a row's Add/Remove button re-rendered the full, unfiltered catalog while the
search box still showed the old query.
- Fix: the Add/Remove handler now calls `refresh(this.search.getValue())` (the `search`
  field was promoted to an instance field) instead of `refresh("")`, so the current
  query stays in effect.
- Guard: `dependencies.spec.js` → *"toggling a dependency keeps the current search
  filter"*.

### 3. Icon-only buttons had no accessible name — was Medium (a11y)
The navbar theme toggle and the dependency chip remove buttons were icon-only with no
accessible name.
- Fix: `MainView` sets `setAriaLabel` + `setTooltipText` on the theme toggle (kept in
  sync as the icon flips in `setDarkMode`); `DependenciesSection` sets
  `"Remove <dependency>"` on each chip remove button.
- Guard: `a11y.spec.js` → *"theme toggle has an accessible name"* and *"chip remove
  button has an accessible name"*.

### 4. Generate gave no in-app feedback — was Low
Generate silently triggered a download with no confirmation.
- Fix: `ActionsBar.generate()` now shows a `LUMO_SUCCESS` `Notification`
  ("Downloading <artifact>.zip…") on the success path; the error path is unchanged.
- Guard: `generate.spec.js` asserts the confirmation notification appears.

### 5. Actions were far from the content — was Low (Observed)
The action bar floated at the top-right of the right pane, above a large empty area.
- Fix: `MainView.buildContent()` sets `flexGrow(1)` on the dependencies section and
  `flexGrow(0)` on the action bar, so the actions sit at the bottom of the pane next to
  the work area.

### 6. Explore content pane had no placeholder — was Low (Observed)
The read-only pane was blank on open and whenever a directory node was selected.
- Fix: `ExploreDialog` initialises the pane with "Select a file to view its contents"
  and restores that hint when a directory (no content) is selected.
- Guard: `explore.spec.js` → *"shows a placeholder before a file is selected"*.
- Note: syntax highlighting remains intentionally out of scope (stock-components
  constraint).

### 7. "…" actions menu had low discoverability — was Low (Observed)
Share/Bookmark lived behind a bare ellipsis `MenuBar`.
- Fix: `ActionsBar` labels the menu trigger "More".
- Guard: `a11y.spec.js` → *"actions menu is labelled \"More\""*.

### Earlier resolved (previous iteration; still holding)
- Picker rows had two competing toggle controls (row click + button) — rows are now
  button-only.
- Add-dependencies search now focuses on open (`addOpenedChangeListener`).
- Explore tree width no longer stretches to half the dialog (`autoWidth`/`fit-content`).
- Explore duplicate-node `TreeData` crash fixed (`Node` carries a unique `path`).

## Test coverage summary

`start-site/e2e/` (Playwright, Chromium) — **15 tests, all passing**:

| Spec | Covers |
| --- | --- |
| `dependencies.spec.js` | empty state, add→chip, positive search filter, remove chip, no-match placeholder (#1), filter retained after toggle (#2) |
| `a11y.spec.js` | theme toggle + chip remove accessible names (#3), "More" menu label (#7) |
| `generate.spec.js` | Generate downloads a non-empty zip; success notification (#4); Generate after adding a dependency |
| `explore.spec.js` | tree opens without the `Explore failed` regression, placeholder before selection (#6), file content loads, dialog closes |
