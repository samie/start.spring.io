// @ts-check
const { test, expect } = require('@playwright/test');
const { gotoApp, addDependency } = require('./helpers');
const fs = require('fs');

test.describe('Generating a project', () => {
  test.beforeEach(async ({ page }) => {
    await gotoApp(page);
  });

  test('Generate downloads a non-empty project zip named after the artifact', async ({ page }) => {
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Generate', exact: true }).click();
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toBe('demo.zip');

    const path = await download.path();
    expect(path).toBeTruthy();
    const size = fs.statSync(path).size;
    expect(size).toBeGreaterThan(0);

    // Fixed (TEST_FINDINGS.md #4): Generate now shows an in-app confirmation.
    await expect(page.getByText(/Downloading .*\.zip/)).toBeVisible();
  });

  test('Generate works after adding a dependency', async ({ page }) => {
    await addDependency(page, 'Spring Web');

    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Generate', exact: true }).click();
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toMatch(/\.zip$/);
    const size = fs.statSync(await download.path()).size;
    expect(size).toBeGreaterThan(0);
  });
});
