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
  return /^(:root|\[data-theme="(light|dark)"\])(\s*,\s*(:root|\[data-theme="(light|dark)"\]))*$/.test(selector);
}

function declaredVariables(body) {
  return (body.match(/--[\w-]+(?=\s*:)/g) || []).sort();
}

beforeEach(() => {
  window.localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
});

afterEach(() => {
  jest.restoreAllMocks();
});

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

  test('clicking switches light to dark and back', async () => {
    const { document } = await loadApp();
    expect(theme(document)).toBe('light');
    toggle(document).click();
    expect(theme(document)).toBe('dark');
    toggle(document).click();
    expect(theme(document)).toBe('light');
  });

  test('the label names the theme you get when you click', async () => {
    const { document } = await loadApp();
    expect(toggle(document).textContent).toBe('Dark theme');
    expect(toggle(document).getAttribute('aria-label')).toBe('Switch to the dark theme');
    toggle(document).click();
    expect(toggle(document).textContent).toBe('Light theme');
    expect(toggle(document).getAttribute('aria-label')).toBe('Switch to the light theme');
  });

  test('clicking does not call the API', async () => {
    const { document, api } = await loadApp();
    const before = api.calls.length;
    toggle(document).click();
    expect(api.calls.length).toBe(before);
  });

  test('the app state follows the theme', async () => {
    const { document, app } = await loadApp();
    expect(app.state.theme).toBe('light');
    toggle(document).click();
    expect(app.state.theme).toBe('dark');
  });
});

describe('AC-2: data-theme attribute and CSS variables', () => {
  test('light is the default and is set on <html> as data-theme', async () => {
    const { document } = await loadApp();
    expect(theme(document)).toBe('light');
  });

  test('app.js contains no colour', () => {
    expect(fs.readFileSync(APP_PATH, 'utf8')).not.toMatch(COLOUR_LITERAL);
  });

  test('style.css has a light and a dark theme block declaring the same variables', () => {
    const rules = cssRules();
    const light = rules.find((r) => r.selector.split(',').map((s) => s.trim()).includes(':root'));
    const dark = rules.find((r) => r.selector === '[data-theme="dark"]');
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
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('dark');
    toggle(document).click();
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('light');
  });

  test('a stored dark theme is restored on load', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'dark');
    const { document } = await loadApp();
    expect(theme(document)).toBe('dark');
    expect(toggle(document).textContent).toBe('Light theme');
  });

  test('a refresh keeps the theme picked before it', async () => {
    let { document } = await loadApp();
    toggle(document).click();
    document.documentElement.removeAttribute('data-theme');
    ({ document } = await loadApp());
    expect(theme(document)).toBe('dark');
  });

  test('an unknown stored value falls back to the default', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'purple');
    const { document } = await loadApp();
    expect(theme(document)).toBe('light');
  });

  test('the toggle still works when localStorage throws', async () => {
    jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    const { document } = await loadApp();
    expect(theme(document)).toBe('light');
    toggle(document).click();
    expect(theme(document)).toBe('dark');
  });
});

describe('theme without the API', () => {
  test('the theme is applied even when the health check fails', async () => {
    window.localStorage.setItem(STORAGE_KEY, 'dark');
    const { document } = await loadApp({ failing: ['/api/health'] });
    expect(theme(document)).toBe('dark');
  });
});
