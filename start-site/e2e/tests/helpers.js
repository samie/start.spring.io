// @ts-check
const { expect } = require('@playwright/test');

/** Path the Vaadin UI is mounted at (see vaadin.url-mapping = /ui/*). */
const UI_PATH = '/ui/';

/**
 * Navigate to the Vaadin UI and wait until it has bootstrapped (the primary
 * "Add dependencies..." button is the readiness signal).
 * @param {import('@playwright/test').Page} page
 */
async function gotoApp(page) {
  await page.goto(UI_PATH, { waitUntil: 'networkidle' });
  await expect(page.getByRole('button', { name: /Add dependencies/ })).toBeVisible();
}

/**
 * Scope for the currently-open Vaadin dialog. The `vaadin-dialog` host itself is
 * zero-size (content is teleported), so we target the visible content element and
 * use it only as a container for inner role/text queries (which resolve uniquely).
 * @param {import('@playwright/test').Page} page
 */
function dialog(page) {
  return page.locator('vaadin-dialog-content:visible');
}

/**
 * Open the dependency picker, search for a dependency by display name, toggle it
 * on by clicking its row, and close the picker.
 * @param {import('@playwright/test').Page} page
 * @param {string} name exact dependency display name (e.g. "Spring Web")
 */
async function addDependency(page, name) {
  await page.getByRole('button', { name: /Add dependencies/ }).click();
  const dlg = dialog(page);
  const search = dlg.getByRole('textbox');
  await expect(search).toBeVisible();
  await search.fill(name);
  // Rows are button-only: toggle via the row's Add/Remove button, not the row body.
  const nameSpan = dlg.getByText(name, { exact: true }).first();
  await expect(nameSpan).toBeVisible();
  const row = nameSpan.locator('xpath=ancestor::vaadin-vertical-layout[1]');
  await row.getByRole('button', { name: /Add|Remove/ }).click();
  await dlg.getByRole('button', { name: 'Close' }).click();
  await expect(dialog(page)).toHaveCount(0);
}

module.exports = { UI_PATH, gotoApp, dialog, addDependency };
