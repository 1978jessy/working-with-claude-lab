const fs = require('fs');
const path = require('path');
const { loadApp, APP_PATH } = require('./setup/loadApp');

const CSS_PATH = path.join(path.dirname(APP_PATH), 'style.css');
const STORAGE_KEY = 'ops-dashboard.theme';
const COLOUR_LITERAL = /#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(/;

function theme(document) {
  return document.documentElement.getAttribute('data-theme');
}

function toggle(document) {
  return document.getElementById('theme-toggle');
}

/** The CSS rule blocks of style.css as { selector, body } pairs (comments removed). */
function cssRules() {
  const css = fs.readFileSync(CSS_PATH, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '');
  const rules = [];
  const re = /([^{}]+)\{([^}]*)\}/g;
  let match;
  while ((match = re.exec(css)) !== null) {
    rules.push({ selector: match[1].trim(), body: match[2] });
  }
  return rules;
}

function isThemeBlock(selector) {
  return /^(:root|(:root)?\[data-theme="(light|dark)"\])(\s*,\s*(:root|(:root)?\[data-theme="(light|dark)"\]))*$/.test(selector);
}

function declaredVariables(body) {
  return (body.match(/--[\w-]+(?=\s*:)/g) || []).sort();
}

beforeEach(() => {
  window.localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
});

const originalMatchMedia = window.matchMedia;

afterEach(() => {
  jest.restoreAllMocks();
  window.matchMedia = originalMatchMedia;
});

/** jsdom has no matchMedia; stand in for an OS that answers every query as given. */
function mockOsPrefers(scheme) {
  window.matchMedia = jest.fn((query) => ({
    matches: query.includes('prefers-color-scheme: ' + scheme),
    media: query,
    addEventListener() {},
    removeEventListener() {},
    addListener() {},
    removeListener() {}
  }));
}

describe('AC-1: theme toggle in the header', () => {
  test('there is a #theme-toggle button inside the header that does not submit the form', async () => {
    const { document } = await loadApp();
    const button = toggle(document);
    expect(button).not.toBeNull();
    expect(button.tagName).toBe('BUTTON');
    expect(button.getAttribute('type')).toBe('button');
    expect(document.getElementById('app-header').contains(button)).toBe(true);
    expect(document.getElementById('range-form').contains(button)).toBe(false);
  });

  test('clicking switches dark to light and back', async () => {
    const { document } = await loadApp();
    expect(theme(document)).toBe('dark');
    toggle(document).click();
    expect(theme(document)).toBe('light');
    toggle(document).click();
    expect(theme(document)).toBe('dark');
  });

  test('the label names the theme you get when you click', async () => {
    const { document } = await loadApp();
    expect(toggle(document).textContent).toBe('Light theme');
    expect(toggle(document).getAttribute('aria-label')).toBe('Switch to the light theme');
    toggle(document).click();
    expect(toggle(document).textContent).toBe('Dark theme');
    expect(toggle(document).getAttribute('aria-label')).toBe('Switch to the dark theme');
  });

  test('clicking does not call the API', async () => {
    const { document, api } = await loadApp();
    const before = api.calls.length;
    toggle(document).click();
    expect(api.calls.length).toBe(before);
  });

  test('the app state follows the theme', async () => {
    const { document, app } = await loadApp();
    expect(app.state.theme).toBe('dark');
    toggle(document).click();
    expect(app.state.theme).toBe('light');
  });
});

describe('AC-2: data-theme attribute and CSS variables', () => {
  test('app.js contains no colour', () => {
    expect(fs.readFileSync(APP_PATH, 'utf8')).not.toMatch(COLOUR_LITERAL);
  });

  test('style.css has a light and a dark theme block declaring the same variables', () => {
    const rules = cssRules();
    const dark = rules.find((r) => r.selector.split(',').map((s) => s.trim()).includes(':root'));
    // :root[data-theme="light"] outranks the bare :root of the dark block, so the order of the blocks does not matter.
    const light = rules.find((r) => r.selector === ':root[data-theme="light"]');
    expect(light).toBeDefined();
    expect(dark).toBeDefined();
    expect(declaredVariables(light.body).length).toBeGreaterThan(0);
    expect(declaredVariables(dark.body)).toEqual(declaredVariables(light.body));
  });

  test('outside the theme blocks, style.css uses no colour literal', () => {
    const offenders = cssRules()
      .filter((r) => !isThemeBlock(r.selector))
      .filter((r) => COLOUR_LITERAL.test(r.body))
      .map((r) => r.selector);
    expect(offenders).toEqual([]);
  });

  test.each(['.chart-svg .bar', '.chart-svg .bar.warn', '.chart-svg .bar-label', '.chart-svg .bar-value'])(
    'chart rule %s takes its fill from a CSS variable',
    (selector) => {
      const rule = cssRules().find((r) => r.selector === selector);
      expect(rule).toBeDefined();
      expect(rule.body).toMatch(/fill:\s*var\(--[\w-]+\)/);
    }
  );
});

describe('AC-3: the choice is persisted', () => {
  test('clicking stores the theme in localStorage', async () => {
    const { document } = await loadApp();
    toggle(document).click();
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('light');
    toggle(document).click();
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('dark');
  });

  test('a stored light theme is restored on load', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'light');
    const { document } = await loadApp();
    expect(theme(document)).toBe('light');
    expect(toggle(document).textContent).toBe('Dark theme');
  });

  test('a refresh keeps the theme picked before it', async () => {
    let { document } = await loadApp();
    toggle(document).click();
    document.documentElement.removeAttribute('data-theme');
    ({ document } = await loadApp());
    expect(theme(document)).toBe('light');
  });

  test('an unknown stored value falls back to the default', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'purple');
    const { document } = await loadApp();
    expect(theme(document)).toBe('dark');
  });

  test('the toggle still works when localStorage throws', async () => {
    jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    const { document } = await loadApp();
    expect(theme(document)).toBe('dark');
    toggle(document).click();
    expect(theme(document)).toBe('light');
  });
});

describe('theme without the API', () => {
  test('the theme is applied even when the health check fails', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'light');
    const { document } = await loadApp({ failing: ['/api/health'] });
    expect(theme(document)).toBe('light');
  });
});

describe('AC-4: dark by default, the OS setting is ignored', () => {
  test('with nothing stored the dashboard opens in the dark theme', async () => {
    const { document, app } = await loadApp();
    expect(theme(document)).toBe('dark');
    expect(app.state.theme).toBe('dark');
    expect(toggle(document).textContent).toBe('Light theme');
  });

  test('an OS that prefers light still gets the dark theme', async () => {
    mockOsPrefers('light');
    const { document } = await loadApp();
    expect(theme(document)).toBe('dark');
  });

  test('a stored choice still wins over the default', async () => {
    mockOsPrefers('dark');
    window.localStorage.setItem(STORAGE_KEY, 'light');
    const { document } = await loadApp();
    expect(theme(document)).toBe('light');
  });

  test('neither style.css nor app.js looks at the OS colour scheme', () => {
    expect(fs.readFileSync(CSS_PATH, 'utf8')).not.toMatch(/prefers-color-scheme/);
    expect(fs.readFileSync(APP_PATH, 'utf8')).not.toMatch(/matchMedia|prefers-color-scheme/);
  });

  test('index.html already says dark before app.js runs', () => {
    const html = fs.readFileSync(path.join(path.dirname(APP_PATH), 'index.html'), 'utf8');
    expect(html).toMatch(/<html[^>]*\sdata-theme="dark"/);
    expect(html).toMatch(/<button id="theme-toggle"[^>]*>Light theme<\/button>/);
  });
});
