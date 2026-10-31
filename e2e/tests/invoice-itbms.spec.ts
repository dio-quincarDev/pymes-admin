import { test, expect, type Page, type Locator } from '@playwright/test';
import { TENANT_ID, mockCore, seedSession, type InvoicePayload } from './support/mocks';

test.use({ serviceWorkers: 'block' });

async function selectOption(page: Page, text: string) {
  await page.locator('.q-menu').last().getByText(text, { exact: true }).click();
}

async function fillItem(page: Page, index: number, qty: number, valor: number, itbms: 0 | 7 | 10) {
  const card = page.locator('.item-card').nth(index);

  await card.locator('.item-card__product').click();
  await selectOption(page, 'Producto E2E');

  await card.locator('.item-card__field--unit').click();
  await selectOption(page, 'Unidad');

  await card.locator('.item-card__field--qty input').fill(String(qty));
  await card.locator('.item-card__field--valor input').fill(String(valor));

  if (itbms !== 0) {
    await card.locator('.item-card__field--itbms').click();
    await selectOption(page, `${itbms}%`);
  }
}

test('invoice dialog applies ITBMS 0/7/10 per item and sends each rate upstream', async ({ page }) => {
  let saved: InvoicePayload | undefined;
  await seedSession(page);
  await mockCore(page, (payload) => { saved = payload; });

  await page.goto('/#/dashboard/facturas');
  await expect(page.getByRole('heading', { name: 'Facturas' })).toBeVisible();
  await page.getByRole('button', { name: 'Factura', exact: true }).click();

  const dialog = page.locator('.q-dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('.item-card')).toHaveCount(1);

  await dialog.locator('.q-select').first().click();
  await selectOption(page, 'Proveedor E2E');

  await fillItem(page, 0, 2, 50, 0);
  await page.getByRole('button', { name: 'Agregar item' }).click();
  await fillItem(page, 1, 1, 100, 7);
  await page.getByRole('button', { name: 'Agregar item' }).click();
  await fillItem(page, 2, 1, 100, 10);

  const rows: Locator[] = await page.locator('.invoice-dialog__breakdown-row').all();
  await expect(rows[0]).toContainText('$100.00'); // Sin ITBMS: 2×50 exento
  await expect(rows[1]).toContainText('$200.00'); // Con ITBMS: 100 + 100
  await expect(rows[2]).toContainText('$17.00'); // ITBMS: 7 + 10
  await expect(page.locator('.invoice-dialog__total-val')).toContainText('$317.00');

  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(dialog).toBeHidden();

  expect(saved?.tenantId).toBe(TENANT_ID);
  expect(saved?.items?.map((item) => item.itbmsTasa)).toEqual([0, 7, 10]);
  expect(saved?.items?.map((item) => item.cantidadPresentacion)).toEqual([2, 1, 1]);
  expect(saved?.items?.map((item) => item.valorPresentacion)).toEqual([50, 100, 100]);
});
