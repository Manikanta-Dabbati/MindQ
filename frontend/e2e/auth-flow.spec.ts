import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {
  test('landing page loads and shows correct content', async ({ page }) => {
    await page.goto('/');
    
    // Should show MindQ branding
    await expect(page.locator('text=MindQ').first()).toBeVisible();
    
    // Should show hero content
    await expect(page.locator('text=Start Learning Free')).toBeVisible();
    await expect(page.locator('text=AI-Powered Learning Platform')).toBeVisible();
    
    // Should show navigation
    await expect(page.locator('text=Sign in')).toBeVisible();
    await expect(page.locator('text=Get Started')).toBeVisible();
  });

  test('can navigate to register page', async ({ page }) => {
    await page.goto('/');
    
    // Click Get Started button
    await page.locator('text=Get Started').first().click();
    
    // Should navigate to register page
    await expect(page).toHaveURL('/register');
    await expect(page.locator('text=Create your account')).toBeVisible();
  });

  test('can navigate to login page', async ({ page }) => {
    await page.goto('/');
    
    // Click Sign in link
    await page.locator('text=Sign in').first().click();
    
    // Should navigate to login page
    await expect(page).toHaveURL('/login');
    await expect(page.locator('text=Sign in to MindQ')).toBeVisible();
  });

  test('login page shows error for empty fields', async ({ page }) => {
    await page.goto('/login');
    
    // Click sign in without filling fields
    await page.locator('button:has-text("Sign in")').click();
    
    // Should show validation errors
    await expect(page.locator('text=Email is required')).toBeVisible();
    await expect(page.locator('text=Password is required')).toBeVisible();
  });

  test('register page shows error for empty fields', async ({ page }) => {
    await page.goto('/register');
    
    // Click create account without filling fields
    await page.locator('button:has-text("Create account")').click();
    
    // Should show validation errors
    await expect(page.locator('text=Full name is required')).toBeVisible();
    await expect(page.locator('text=Email is required')).toBeVisible();
    await expect(page.locator('text=Password is required')).toBeVisible();
  });

  test('protected routes redirect to login', async ({ page }) => {
    // Try to access dashboard without auth
    await page.goto('/dashboard');
    
    // Should redirect to login
    await expect(page).toHaveURL('/login');
  });

  test('404 page shows for unknown routes', async ({ page }) => {
    await page.goto('/unknown-page');
    
    // Should show 404 page
    await expect(page.locator('text=404')).toBeVisible();
    await expect(page.locator('text=Page Not Found')).toBeVisible();
  });
});
