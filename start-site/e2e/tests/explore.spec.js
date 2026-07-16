// @ts-check
const { test, expect } = require('@playwright/test');
const { gotoApp, dialog } = require('./helpers');

test.describe('Exploring a project', () => {
  test.beforeEach(async ({ page }) => {
    await gotoApp(page);
  });

  test('Explore opens the file tree without failing', async ({ page }) => {
    await page.getByRole('button', { name: 'Explore', exact: true }).click();
    const dlg = dialog(page);
    const grid = dlg.locator('vaadin-grid');
    await expect(grid).toBeVisible();

    // The regression we are guarding against: a duplicate-node TreeData error
    // surfaced as an "Explore failed: ..." notification.
    await expect(page.getByText(/Explore failed/)).toHaveCount(0);

    // The tree renders with the project root folder.
    await expect(grid.getByText('demo', { exact: true }).first()).toBeVisible();
  });

  test('shows a placeholder before a file is selected', async ({ page }) => {
    await page.getByRole('button', { name: 'Explore', exact: true }).click();
    const dlg = dialog(page);
    await expect(dlg.locator('vaadin-grid')).toBeVisible();

    // Fixed (TEST_FINDINGS.md #6): the content pane hints at what to do instead of
    // being blank on open.
    await expect(dlg.locator('pre')).toContainText('Select a file to view its contents');
  });

  test('selecting a file shows its content', async ({ page }) => {
    await page.getByRole('button', { name: 'Explore', exact: true }).click();
    const dlg = dialog(page);
    const grid = dlg.locator('vaadin-grid');
    await expect(grid).toBeVisible();

    // HELP.md sits at the project root and is always generated; clicking it
    // should populate the read-only content pane.
    await grid.getByText('HELP.md', { exact: true }).first().click();
    await expect(dlg.locator('pre')).toContainText(/\S/);
  });

  test('Explore can be closed', async ({ page }) => {
    await page.getByRole('button', { name: 'Explore', exact: true }).click();
    const dlg = dialog(page);
    await expect(dlg.locator('vaadin-grid')).toBeVisible();
    await dlg.getByRole('button', { name: 'Close' }).click();
    await expect(dialog(page)).toHaveCount(0);
  });
});
