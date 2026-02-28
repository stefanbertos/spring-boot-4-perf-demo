import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.beforeEach(async ({ page }) => { await mockApi(page); });

test('sidebar shows all navigation links', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Run Test')).toBeVisible();
  await expect(page.getByText('Test Runs')).toBeVisible();
  await expect(page.getByText('Dashboards')).toBeVisible();
  await expect(page.getByText('Admin')).toBeVisible();
});

test('sidebar brand shows Perf Tester', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Perf Tester')).toBeVisible();
});

test('root path redirects to /dashboards', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL('/dashboards');
});

test('clicking Run Test navigates to /send', async ({ page }) => {
  await page.goto('/dashboards');
  await page.getByRole('button', { name: 'Run Test' }).click();
  await expect(page).toHaveURL('/send');
});

test('clicking Test Runs navigates to /test-runs', async ({ page }) => {
  await page.goto('/dashboards');
  await page.getByRole('button', { name: 'Test Runs' }).click();
  await expect(page).toHaveURL('/test-runs');
});

test('clicking Admin navigates to /admin', async ({ page }) => {
  await page.goto('/dashboards');
  await page.getByRole('button', { name: 'Admin' }).click();
  await expect(page).toHaveURL('/admin');
});
