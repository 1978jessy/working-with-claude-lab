const { loadApp } = require('./setup/loadApp');

function rangeCalls(api, path) {
  return api.calls.filter((c) => c.path === path).map((c) => c.params);
}

function click(document, id) {
  document.getElementById(id).dispatchEvent(new window.Event('click', { bubbles: true }));
}

async function flush() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}

describe('date range', () => {
  test('defaults to the last 30 days ending on the API today', async () => {
    const { document } = await loadApp();
    expect(document.getElementById('range-from').value).toBe('2026-08-22');
    expect(document.getElementById('range-to').value).toBe('2026-09-21');
  });

  test('the 30-day preset is active on load', async () => {
    const { document } = await loadApp();
    expect(document.getElementById('preset-30').classList.contains('active')).toBe(true);
    expect(document.getElementById('preset-7').classList.contains('active')).toBe(false);
  });

  test('the 7-day preset updates the inputs and refetches with the new range', async () => {
    const { document, api } = await loadApp();
    click(document, 'preset-7');
    await flush();
    expect(document.getElementById('range-from').value).toBe('2026-09-14');
    expect(document.getElementById('range-to').value).toBe('2026-09-21');
    const kpiCalls = rangeCalls(api, '/api/kpis');
    expect(kpiCalls[kpiCalls.length - 1]).toEqual({ from: '2026-09-14', to: '2026-09-21' });
    expect(document.getElementById('preset-7').classList.contains('active')).toBe(true);
    expect(document.getElementById('preset-30').classList.contains('active')).toBe(false);
  });

  test('the 90-day preset reaches back to June', async () => {
    const { document, api } = await loadApp();
    click(document, 'preset-90');
    await flush();
    expect(document.getElementById('range-from').value).toBe('2026-06-23');
    const lateCalls = rangeCalls(api, '/api/deliveries/late');
    expect(lateCalls[lateCalls.length - 1].from).toBe('2026-06-23');
  });

  test('submitting custom dates queries every range endpoint with them', async () => {
    const { document, api } = await loadApp();
    document.getElementById('range-from').value = '2026-07-01';
    document.getElementById('range-to').value = '2026-07-31';
    document.getElementById('range-form').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
    await flush();
    for (const path of ['/api/kpis', '/api/deliveries/on-time', '/api/deliveries/late', '/api/tickets/by-category']) {
      const calls = rangeCalls(api, path);
      expect(calls[calls.length - 1].from).toBe('2026-07-01');
      expect(calls[calls.length - 1].to).toBe('2026-07-31');
    }
  });

  test('submitting custom dates clears the active preset', async () => {
    const { document } = await loadApp();
    document.getElementById('range-from').value = '2026-07-01';
    document.getElementById('range-to').value = '2026-07-31';
    document.getElementById('range-form').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
    await flush();
    expect(document.querySelectorAll('.preset.active')).toHaveLength(0);
  });
});
