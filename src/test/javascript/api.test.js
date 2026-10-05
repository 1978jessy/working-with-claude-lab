const { loadApp } = require('./setup/loadApp');

describe('API calls', () => {
  test('asks the API for today first, then the four range endpoints and the vendors', async () => {
    const { api } = await loadApp();
    const paths = api.calls.map((c) => c.path);
    expect(paths[0]).toBe('/api/health');
    expect(paths.slice(1).sort()).toEqual([
      '/api/deliveries/late',
      '/api/deliveries/on-time',
      '/api/kpis',
      '/api/tickets/by-category',
      '/api/vendors'
    ]);
  });

  test('asks for at most 20 late deliveries', async () => {
    const { api } = await loadApp();
    const late = api.calls.find((c) => c.path === '/api/deliveries/late');
    expect(late.params.limit).toBe('20');
  });

  test('the status line is empty after a successful load', async () => {
    const { document } = await loadApp();
    expect(document.getElementById('status-line').textContent).toBe('');
    expect(document.getElementById('status-line').classList.contains('error')).toBe(false);
  });

  test('shows an error in the status line when a range endpoint fails', async () => {
    const { document, app } = await loadApp({ failing: ['/api/kpis'] });
    const status = document.getElementById('status-line');
    expect(status.classList.contains('error')).toBe(true);
    expect(status.textContent).toBe('Could not load the dashboard: Request failed: 500 /api/kpis?from=2026-08-22&to=2026-09-21');
    expect(app.state.error).not.toBeNull();
  });

  test('shows an error when the API is unreachable for the health check', async () => {
    const { document } = await loadApp({ failing: ['/api/health'] });
    const status = document.getElementById('status-line');
    expect(status.classList.contains('error')).toBe(true);
    expect(status.textContent).toBe('Could not reach the API: Request failed: 500 /api/health');
  });

  test('a vendors failure does not blank the KPI tiles', async () => {
    const { document } = await loadApp({ failing: ['/api/vendors'] });
    expect(document.querySelector('#kpi-orders .kpi-value').textContent).toBe('624');
    expect(document.getElementById('status-line').textContent).toBe('Could not load vendors: Request failed: 500 /api/vendors');
  });
});
