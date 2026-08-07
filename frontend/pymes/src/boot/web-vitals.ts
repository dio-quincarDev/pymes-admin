import { defineBoot } from '#q-app/wrappers';

interface LayoutShiftEntry extends PerformanceEntry { value: number }
interface FirstInputEntry extends PerformanceEntry { processingStart: number }

// ponytail: native PerformanceObserver, no deps. Reports CLS, LCP, FID.
export default defineBoot(() => {
  if (!('PerformanceObserver' in window)) return;

  const log = (metric: string, value: number) => {
    console.debug(`[Web Vitals] ${metric}:`, value);
    // ponytail: send to analytics here if needed later
  };

  try {
    const cls = new PerformanceObserver((list) => {
      const value = (list.getEntries() as LayoutShiftEntry[]).reduce((acc, entry) => acc + entry.value, 0);
      log('CLS', value);
    });
    cls.observe({ type: 'layout-shift', buffered: true });

    const lcp = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      const last = entries[entries.length - 1];
      if (last) log('LCP', last.startTime);
    });
    lcp.observe({ type: 'largest-contentful-paint', buffered: true });

    const fid = new PerformanceObserver((list) => {
      const entry = (list.getEntries() as FirstInputEntry[])[0];
      if (entry) log('FID', entry.processingStart - entry.startTime);
    });
    fid.observe({ type: 'first-input', buffered: true });
  } catch { /* métricas no soportadas */ }
});
