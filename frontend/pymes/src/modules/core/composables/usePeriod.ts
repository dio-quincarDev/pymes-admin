import { ref, watch } from 'vue';

const STORAGE_KEY = 'pymeq_analytics_period';

function getCurrentPeriod() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export function usePeriod() {
  const period = ref(localStorage.getItem(STORAGE_KEY) || getCurrentPeriod());

  watch(period, (val) => localStorage.setItem(STORAGE_KEY, val));

  function setPeriod(val: string) {
    period.value = val;
  }

  return { period, setPeriod };
}
