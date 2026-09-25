import { ref, shallowRef, computed, readonly } from 'vue';
import { productoService } from '../services/producto.service';
import { api } from 'src/boot/axios';
import type { Producto, SetupInfo, SetupCategory } from '../types';

interface CategoryGroup {
  name: string;
  total: number;
  pct: number;
  count: number;
}

export function useAnalisisGastos(tenantId: string | undefined) {
  const products = ref<Producto[]>([]);
  const setupCategories = ref<SetupCategory[]>([]);
  const loading = shallowRef(false);

  const totalInvestment = computed(() =>
    products.value.reduce((sum, p) => sum + (p.totalInvestment ?? 0), 0),
  );
  const productCount = computed(() => products.value.length);

  const categoryNameMap = computed(() => {
    const map = new Map<string, string>();
    function walk(cats: SetupCategory[]) {
      for (const c of cats) {
        map.set(c.code, c.name);
        if (c.children?.length) walk(c.children);
      }
    }
    walk(setupCategories.value);
    return map;
  });

  const byCategory = computed<CategoryGroup[]>(() => {
    const map = new Map<string, { total: number; count: number }>();
    let grandTotal = 0;
    for (const p of products.value) {
      const cat = categoryNameMap.value.get(p.category) || p.category || 'Sin categoría';
      const inv = p.totalInvestment ?? 0;
      const g = map.get(cat) ?? { total: 0, count: 0 };
      g.total += inv;
      g.count++;
      map.set(cat, g);
      grandTotal += inv;
    }
    return Array.from(map.entries())
      .map(([name, g]) => ({
        name,
        total: g.total,
        pct: grandTotal > 0 ? +(g.total / grandTotal * 100).toFixed(1) : 0,
        count: g.count,
      }))
      .sort((a, b) => b.total - a.total);
  });

  const categoryChartItems = computed(() =>
    byCategory.value.map((c) => ({
      category: c.name,
      currentAmount: c.total,
      percentage: c.pct,
    })),
  );

  async function load() {
    if (!tenantId) return;
    loading.value = true;
    try {
      const [prodRes, setupRes] = await Promise.all([
        productoService.getAll(tenantId),
        api.get<SetupInfo>(`/core/setup/${tenantId}`),
      ]);
      products.value = prodRes.data;
      setupCategories.value = setupRes.data.categories || [];
    } finally {
      loading.value = false;
    }
  }

  return {
    products: readonly(products),
    setupCategories: readonly(setupCategories),
    loading: readonly(loading),
    totalInvestment,
    productCount,
    byCategory,
    categoryChartItems,
    load,
  };
}
