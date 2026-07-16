// @ts-check
const { test, expect } = require('@playwright/test');
const { gotoApp, dialog, addDependency } = require('./helpers');

test.describe('Adding dependencies', () => {
  test.beforeEach(async ({ page }) => {
    await gotoApp(page);
  });

  test('starts with the empty state', async ({ page }) => {
    await expect(page.getByText('No dependency selected.')).toBeVisible();
  });

  test('add a dependency from the picker and see it as a chip', async ({ page }) => {
    await addDependency(page, 'Spring Web');

    // Empty state is replaced by a chip carrying the dependency name.
    await expect(page.getByText('No dependency selected.')).toBeHidden();
    await expect(page.getByText('Spring Web', { exact: true })).toBeVisible();
  });

  test('search filters the dependency list to matching entries', async ({ page }) => {
    await page.getByRole('button', { name: /Add dependencies/ }).click();
    const dlg = dialog(page);
    const search = dlg.getByRole('textbox');
    await expect(search).toBeVisible();

    await search.fill('Lombok');
    await expect(dlg.getByText('Lombok', { exact: true })).toBeVisible();
    // A clearly unrelated dependency should be filtered out.
    await expect(dlg.getByText('Spring Web', { exact: true })).toHaveCount(0);
  });

  // Fixed (TEST_FINDINGS.md #1): when the query matches nothing the picker now clears
  // the list and shows an explicit "No matching dependencies" placeholder.
  test('search with no matches shows an empty-state placeholder', async ({ page }) => {
    await page.getByRole('button', { name: /Add dependencies/ }).click();
    const dlg = dialog(page);
    const search = dlg.getByRole('textbox');
    await expect(search).toBeVisible();

    await search.fill('Lombok');
    await expect(dlg.getByText('Lombok', { exact: true })).toBeVisible();
    await search.fill('zzzznotadependency');
    // The placeholder shows and the previously-matching row is no longer visible.
    await expect(dlg.getByText('No matching dependencies')).toBeVisible();
    await expect(dlg.getByText('Lombok', { exact: true })).toBeHidden();
  });

  // Fixed (TEST_FINDINGS.md #2): toggling a dependency keeps the current search filter
  // in effect instead of resetting the list to the full catalog.
  test('toggling a dependency keeps the current search filter', async ({ page }) => {
    await page.getByRole('button', { name: /Add dependencies/ }).click();
    const dlg = dialog(page);
    const search = dlg.getByRole('textbox');
    await search.fill('Lombok');
    await expect(dlg.getByText('Lombok', { exact: true })).toBeVisible();

    const row = dlg.getByText('Lombok', { exact: true }).first()
      .locator('xpath=ancestor::vaadin-vertical-layout[1]');
    await row.getByRole('button', { name: /Add|Remove/ }).click();

    // The query text is retained AND the list stays filtered: an unrelated dependency
    // (which the "Lombok" filter excludes) must not reappear.
    await expect(search).toHaveValue('Lombok');
    await expect(dlg.getByText('Spring Web', { exact: true })).toHaveCount(0);
  });

  test('a selected dependency can be removed again', async ({ page }) => {
    await addDependency(page, 'Spring Web');
    await expect(page.getByText('Spring Web', { exact: true })).toBeVisible();

    // The chip is a horizontal layout with a single (remove) button.
    const chip = page.locator('vaadin-horizontal-layout').filter({ hasText: 'Spring Web' }).first();
    await chip.getByRole('button').click();
    await expect(page.getByText('No dependency selected.')).toBeVisible();
  });
});
