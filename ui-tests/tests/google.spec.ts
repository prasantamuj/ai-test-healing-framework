import { test, expect } from '../src/fixtures/base';

test.describe('Google — environment baseline @smoke', () => {
  test('SauceDemo login page loads', async ({ page }) => {
    await page.goto('https://www.saucedemo.com');
    await expect(page.getByRole('textbox', { name: 'Username' })).toBeVisible();
    await expect(page.getByRole('textbox', { name: 'Password' })).toBeVisible();
  });
});
