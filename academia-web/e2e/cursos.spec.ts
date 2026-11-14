import { expect, test } from '@playwright/test';

const claveAdmin = process.env['ADMIN_PASSWORD'];
if (!claveAdmin) throw new Error('Defina ADMIN_PASSWORD con la clave del entorno de pruebas.');

test.describe('Academia UPN · flujo principal', () => {
  test('sin sesión redirige al login', async ({ page }) => {
    await page.goto('/cursos');
    await expect(page).toHaveURL(/\/login(?:\?|$)/);
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });

  test('credenciales incorrectas muestran un error', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Usuario', { exact: true }).fill('admin');
    await page.getByLabel('Contraseña', { exact: true }).fill('clave-incorrecta');
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
    await expect(page.getByRole('alert')).toHaveText('Usuario o contraseña incorrectos');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('el administrador crea un curso y lo encuentra en el listado', async ({ page }, testInfo) => {
    // El reintento usa otro código para no chocar con un alta anterior.
    const codigo = `QAE-${401 + testInfo.retry}`;
    await page.goto('/login');
    await page.getByLabel('Usuario', { exact: true }).fill('admin');
    await page.getByLabel('Contraseña', { exact: true }).fill(claveAdmin);
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Cursos', exact: true })).toBeVisible();

    await page.getByRole('link', { name: '+ Nuevo curso', exact: true }).click();
    await page.getByLabel('Código', { exact: true }).fill(codigo);
    await page.getByLabel('Nombre', { exact: true }).fill('Curso creado por Playwright');
    await page.getByLabel('Docente', { exact: true }).fill('Prueba E2E');
    await page.getByLabel('Créditos', { exact: true }).fill('3');
    await page.getByLabel('Cupos', { exact: true }).fill('20');
    await page.getByRole('button', { name: 'Guardar', exact: true }).click();

    await expect(page).toHaveURL(/\/cursos$/);
    await page.getByLabel('Buscar por docente', { exact: true }).fill('Prueba E2E');
    const fila = page.getByRole('row').filter({ has: page.getByRole('cell', { name: codigo, exact: true }) });
    await expect(fila.getByRole('cell', { name: 'Curso creado por Playwright', exact: true })).toBeVisible();
    const captura = testInfo.outputPath('curso-creado.png');
    await page.screenshot({ path: captura, fullPage: true });
    await testInfo.attach('Curso creado en Angular', { path: captura, contentType: 'image/png' });
  });
});
