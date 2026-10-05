const { REGISTERED_IDS, readIndexHtml, extractIds, loadApp } = require('./setup/loadApp');

describe('test harness', () => {
  test('every element id in index.html is registered in setup/loadApp.js', () => {
    const ids = extractIds(readIndexHtml());
    expect(ids.length).toBeGreaterThan(0);
    for (const id of ids) {
      if (!REGISTERED_IDS.includes(id)) {
        throw new Error(
          `Unregistered element id "${id}". Add it to REGISTERED_IDS in src/test/javascript/setup/loadApp.js`
        );
      }
    }
  });

  test('every registered id exists in index.html', () => {
    const ids = extractIds(readIndexHtml());
    for (const id of REGISTERED_IDS) {
      expect(ids).toContain(id);
    }
  });

  test('index.html has no duplicate ids', () => {
    const ids = extractIds(readIndexHtml());
    expect(new Set(ids).size).toBe(ids.length);
  });

  test('loadApp mounts the page and every registered element is reachable', async () => {
    const { document } = await loadApp();
    for (const id of REGISTERED_IDS) {
      expect(document.getElementById(id)).not.toBeNull();
    }
  });
});
