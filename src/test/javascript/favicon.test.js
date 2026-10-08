const fs = require('fs');
const path = require('path');
const { readIndexHtml, HTML_PATH } = require('./setup/loadApp');

describe('favicon', () => {
  test('index.html links an SVG icon that exists next to it', () => {
    const doc = new DOMParser().parseFromString(readIndexHtml(), 'text/html');
    const link = doc.querySelector('head link[rel="icon"]');
    expect(link).not.toBeNull();
    expect(link.getAttribute('type')).toBe('image/svg+xml');
    const file = path.join(path.dirname(HTML_PATH), link.getAttribute('href'));
    expect(fs.existsSync(file)).toBe(true);
    expect(fs.readFileSync(file, 'utf8')).toMatch(/^<svg[^>]*xmlns="http:\/\/www\.w3\.org\/2000\/svg"/);
  });
});
