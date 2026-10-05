const { loadApp } = require('./setup/loadApp');

function bars(document, selector) {
  return Array.from(document.querySelectorAll(selector));
}

describe('header and KPI tiles', () => {
  test('shows the app title and subtitle', async () => {
    const { document } = await loadApp();
    expect(document.getElementById('app-title').textContent).toBe('Marlowe & Finch Operations');
    expect(document.getElementById('app-subtitle').textContent).toContain('Deliveries');
  });

  test('renders the on-time rate tile as a percentage', async () => {
    const { document } = await loadApp();
    expect(document.querySelector('#kpi-on-time .kpi-value').textContent).toBe('93.7%');
  });

  test('renders the open tickets and orders tiles as plain counts', async () => {
    const { document } = await loadApp();
    expect(document.querySelector('#kpi-open-tickets .kpi-value').textContent).toBe('114');
    expect(document.querySelector('#kpi-orders .kpi-value').textContent).toBe('624');
  });

  test('renders the revenue tile as money', async () => {
    const { document } = await loadApp();
    expect(document.querySelector('#kpi-revenue .kpi-value').textContent).toBe('£360,095.50');
  });
});

describe('on-time chart', () => {
  test('draws one bar row per carrier, in API order', async () => {
    const { document } = await loadApp();
    const rows = bars(document, '#chart-on-time .bar-row');
    expect(rows.map((g) => g.getAttribute('data-carrier'))).toEqual([
      'Harbour Express', 'Kessler Logistics', 'Northwind Freight', 'Redwood Couriers'
    ]);
  });

  test('bar widths are proportional to the rate on a 0 to 100% scale', async () => {
    const { document } = await loadApp();
    const widths = bars(document, '#chart-on-time rect.bar').map((r) => Number(r.getAttribute('width')));
    expect(widths[0]).toBeGreaterThan(widths[1]);
    expect(widths[0] / widths[1]).toBeCloseTo(0.96 / 0.8981, 3);
  });

  test('labels each bar with the formatted rate', async () => {
    const { document } = await loadApp();
    const values = bars(document, '#chart-on-time .bar-value').map((t) => t.textContent);
    expect(values).toEqual(['96.0%', '89.8%', '96.4%', '96.8%']);
  });

  test('marks a carrier under 95% with the warn class', async () => {
    const { document } = await loadApp();
    const kessler = document.querySelector('#chart-on-time [data-carrier="Kessler Logistics"] rect.bar');
    const harbour = document.querySelector('#chart-on-time [data-carrier="Harbour Express"] rect.bar');
    expect(kessler.classList.contains('warn')).toBe(true);
    expect(harbour.classList.contains('warn')).toBe(false);
  });
});

describe('tickets chart', () => {
  test('draws one column per category with its label', async () => {
    const { document } = await loadApp();
    const cols = bars(document, '#chart-tickets .bar-col');
    expect(cols).toHaveLength(5);
    expect(cols[0].querySelector('.bar-label').textContent).toBe('Delivery delay');
  });

  test('the tallest column is the category with most tickets', async () => {
    const { document } = await loadApp();
    const heights = bars(document, '#chart-tickets rect.bar').map((r) => Number(r.getAttribute('height')));
    expect(Math.max(...heights)).toBe(heights[0]);
    expect(heights[4]).toBeLessThan(heights[0]);
  });
});

describe('late deliveries table', () => {
  test('renders one row per late delivery with the order reference', async () => {
    const { document } = await loadApp();
    const rows = bars(document, '#late-body tr');
    expect(rows).toHaveLength(3);
    expect(rows.map((r) => r.getAttribute('data-order'))).toEqual(['MF-01801', 'MF-01755', 'MF-01790']);
  });

  test('shows an empty message when nothing was late', async () => {
    const { document } = await loadApp({ late: [] });
    const rows = bars(document, '#late-body tr');
    expect(rows).toHaveLength(1);
    expect(rows[0].classList.contains('empty')).toBe(true);
    expect(rows[0].textContent).toBe('No late deliveries in this range');
  });
});

describe('vendors panel', () => {
  test('lists every vendor with its name', async () => {
    const { document } = await loadApp();
    const items = bars(document, '#vendors-list .vendor');
    expect(items).toHaveLength(5);
    expect(items[0].querySelector('.vendor-name').textContent).toBe('Volta Parts GmbH');
  });

  test('highlights renewals inside their notice window', async () => {
    const { document } = await loadApp();
    const due = bars(document, '#vendors-list .vendor.renewal-due').map((li) => li.getAttribute('data-vendor'));
    expect(due).toEqual(['Volta Parts GmbH', 'Lumen Creative', 'Kessler Logistics']);
  });

  test('does not highlight a vendor whose notice window has not opened', async () => {
    const { document } = await loadApp();
    const helpSpark = document.querySelector('#vendors-list [data-vendor="HelpSpark"]');
    expect(helpSpark.classList.contains('renewal-due')).toBe(false);
    expect(helpSpark.querySelector('.vendor-end').textContent).toBe('Ends 2026-12-31 (in 101 days), 30 days notice');
  });
});
