import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.beforeEach(async ({ page }) => { await mockApi(page); });

test('shows Logging tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Logging' })).toBeVisible();
});

test('shows Kafka tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Kafka' })).toBeVisible();
});

test('shows IBM MQ tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'IBM MQ' })).toBeVisible();
});

test('shows Ping tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Ping' })).toBeVisible();
});

test('Kubernetes tab absent when no namespaces', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Kubernetes' })).not.toBeVisible();
});
