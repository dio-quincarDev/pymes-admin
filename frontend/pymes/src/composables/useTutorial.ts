import { nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';

// ponytail: single tour across routes via per-page highlight, not one drive() call — avoids missing DOM across routes
export const TOUR_STEPS = [
  {
    route: '/dashboard/patrimonio',
    element: '[data-tour="inversion"]',
    title: '1/7 — Tu inversión',
    description:
      'Aquí ponés con cuánta plata arrancaste. Sin esto no puedo decirte cuándo la recuperás ni si estás ganando de verdad.',
  },
  {
    route: '/dashboard/costos',
    element: '[data-tour="costos"]',
    title: '2/7 — Gastos y salarios',
    description:
      'Aquí va lo que pagás sí o sí cada mes: alquiler, luz, internet y el equipo. Aunque no vendas, esto se paga.',
  },
  {
    route: '/dashboard/proveedores',
    element: '[data-tour="proveedores"]',
    title: '3/7 — Proveedores',
    description:
      'Guardá a quién le comprás. Después te aviso si dependés mucho de uno solo y te conviene diversificar.',
  },
  {
    route: '/dashboard/productos',
    element: '[data-tour="productos"]',
    title: '4/7 — Productos',
    description:
      'Te dejé lo típico de tu rubro. Borrá lo que no vendés y creá lo tuyo. Sin producto no hay factura.',
  },
  {
    route: '/dashboard/facturas',
    element: '[data-tour="facturas"]',
    title: '5/7 — Facturas',
    description:
      'Registrá lo que vendés. Arriba ves el total sin impuesto y abajo con ITBMS (0/7/10%). Así no te enredás.',
  },
  {
    route: '/dashboard',
    element: '[data-tour="dashboard"]',
    title: '6/7 — Dashboard',
    description:
      'Tu foto rápida: nota de 0 a 100, plata por recuperar, margen del mes y gráfico de 7 días. Si está en rojo, hay que actuar.',
  },
  {
    route: '/dashboard/analisis-gastos',
    element: '[data-tour="analisis"]',
    title: '7/7 — Análisis',
    description:
      'Aquí te digo dónde se te va la plata con 9 alertas criollas: “te comiste el margen”, “dependés de un proveedor”, etc.',
  },
] as const;

const SEEN_KEY = 'pymeq_tour_seen';
const ACTIVE_KEY = 'pymeq_tour_active';
const STEP_KEY = 'pymeq_tour_step';

let driverInstance: ReturnType<typeof driver> | null = null;

function getStepIndex(): number {
  const raw = localStorage.getItem(STEP_KEY);
  const n = raw ? parseInt(raw, 10) : 0;
  return Number.isNaN(n) ? 0 : Math.max(0, Math.min(n, TOUR_STEPS.length - 1));
}

function isActive(): boolean {
  return localStorage.getItem(ACTIVE_KEY) === 'true';
}

function hasSeen(): boolean {
  return localStorage.getItem(SEEN_KEY) === 'true';
}

function destroyDriver() {
  try {
    driverInstance?.destroy();
  } catch {
    // ignore
  }
  driverInstance = null;
}

export function useTutorial() {
  const router = useRouter();
  const route = useRoute();

  function completeTour() {
    destroyDriver();
    localStorage.setItem(SEEN_KEY, 'true');
    localStorage.removeItem(ACTIVE_KEY);
    localStorage.removeItem(STEP_KEY);
  }

  function goNext() {
    const idx = getStepIndex();
    const nextIdx = idx + 1;
    destroyDriver();
    if (nextIdx >= TOUR_STEPS.length) {
      completeTour();
      return;
    }
    localStorage.setItem(STEP_KEY, String(nextIdx));
    const next = TOUR_STEPS[nextIdx]!;
    if (route.path !== next.route) {
      void router.push(next.route);
    } else {
      void showForCurrentRoute();
    }
  }

  function goPrev() {
    const idx = getStepIndex();
    if (idx === 0) return;
    const prevIdx = idx - 1;
    destroyDriver();
    localStorage.setItem(STEP_KEY, String(prevIdx));
    const prev = TOUR_STEPS[prevIdx]!;
    if (route.path !== prev.route) {
      void router.push(prev.route);
    } else {
      void showForCurrentRoute();
    }
  }

  function createDriver() {
    destroyDriver();
    const idx = getStepIndex();
    const isFirst = idx === 0;
    const isLast = idx === TOUR_STEPS.length - 1;

    driverInstance = driver({
      showProgress: false,
      animate: true,
      smoothScroll: true,
      allowClose: true,
      overlayColor: 'rgba(10, 18, 16, 0.85)',
      stagePadding: 8,
      popoverClass: 'pymeq-tour-popover',
      nextBtnText: isLast ? 'Listo' : 'Siguiente →',
      prevBtnText: '← Atrás',
      doneBtnText: 'Listo',
      showButtons: ['next', 'previous', 'close'],
      onCloseClick: () => {
        completeTour();
      },
      onDestroyStarted: () => {
        // driver calls this before destroy — keep seen flag if user closed early
        destroyDriver();
      },
      // ponytail: override default next/prev to handle cross-route navigation
      onNextClick: () => goNext(),
      onPrevClick: () => goPrev(),
      // hide prev on first step via later DOM tweak
      onHighlighted: () => {
        // hide "Atrás" on first step
        if (isFirst) {
          // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
          const prevBtn = document.querySelector('.driver-popover-prev-btn') as HTMLElement | null;
          if (prevBtn) prevBtn.style.display = 'none';
        }
      },
    });
    return driverInstance;
  }

  async function showForCurrentRoute() {
    if (!isActive()) return;
    const idx = getStepIndex();
    const step = TOUR_STEPS[idx];
    if (!step) return;
    const current = route.path;
    const matches = current === step.route || current.startsWith(step.route);
    if (!matches) return;

    await nextTick();
    // ponytail: poll for element (page may render skeleton then data) — avoids 350ms one-shot miss
    let el: HTMLElement | null = null;
    for (let attempt = 0; attempt < 10; attempt++) {
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
      el = document.querySelector(step.element) as HTMLElement | null;
      if (el) break;
      await new Promise<void>((resolve) => setTimeout(resolve, 250));
    }
    if (!el) return;

    const d = createDriver();
    // ponytail: use drive() with single step so next/prev buttons render; highlight() alone hides them
    d.setSteps([
      {
        element: step.element,
        popover: {
          title: step.title,
          description: step.description,
          side: 'bottom' as const,
          align: 'start' as const,
        },
      },
    ]);
    d.drive();
  }

  function startTour(force = false) {
    if (!force && hasSeen()) return;
    localStorage.setItem(ACTIVE_KEY, 'true');
    localStorage.setItem(STEP_KEY, '0');
    localStorage.removeItem(SEEN_KEY);
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    const first = TOUR_STEPS[0]!;
    if (route.path !== first.route) {
      void router.push(first.route).then(() => showForCurrentRoute());
    } else {
      void showForCurrentRoute();
    }
  }

  function resetTour() {
    localStorage.removeItem(SEEN_KEY);
    localStorage.removeItem(ACTIVE_KEY);
    localStorage.removeItem(STEP_KEY);
    destroyDriver();
  }

  return {
    TOUR_STEPS,
    startTour,
    resetTour,
    completeTour,
    showForCurrentRoute,
    hasSeen,
    isActive,
    getStepIndex,
  };
}
