// @ts-check
const { test, expect } = require('@playwright/test');
const { gotoApp, addDependency } = require('./helpers');

test.describe('Accessibility and discoverability', () => {
  test.beforeEach(async ({ page }) => {
    await gotoApp(page);
  });

  // Fixed (TEST_FINDINGS.md #3): the icon-only theme toggle now has an accessible name.
  test('theme toggle has an accessible name', async ({ page }) => {
    const name = await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('vaadin-button'));
      const t = btns.find((b) => b.querySelector('vaadin-icon[icon*="moon"], vaadin-icon[icon*="sun"]'));
      return t ? (t.getAttribute('aria-label') || '') : 'NOT-FOUND';
    });
    expect(name).toMatch(/mode/i);
  });

  // Fixed (TEST_FINDINGS.md #3): each chip remove button names the dependency it removes.
  test('chip remove button has an accessible name', async ({ page }) => {
    await addDependency(page, 'Spring Web');
    await expect(page.getByRole('button', { name: 'Remove Spring Web' })).toBeVisible();
  });

  // Fixed (TEST_FINDINGS.md #7): Share/Bookmark live behind a labelled "More" menu.
  test('actions menu is labelled "More"', async ({ page }) => {
    await expect(page.getByText('More', { exact: true })).toBeVisible();
  });
});
