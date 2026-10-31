<script setup lang="ts">
import { ref, shallowRef, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useTutorial } from 'src/composables/useTutorial';
import { formatCurrency, isUuidLike, toLocalISODate } from 'src/utils/format';
import { calcBreakdown } from '../utils/invoiceMath';
import {
  SUELTO,
  isItemComplete,
  scaledPriceOnPresentationChange,
  toPresentacionPayload,
} from '../utils/invoiceItemGuards';
import { facturaService } from '../services/factura.service';
import { productoService } from '../services/producto.service';
import { proveedorService } from '../services/proveedor.service';
import { costoService } from '../services/costo.service';
import type {
  Collaborador,
  Factura,
  FacturaRequest,
  GastoFijoRecurrente,
  SetupInfo,
  SetupCategory,
  ProductOption,
  Producto,
} from '../types';
import EmptyState from 'src/components/ui/EmptyState.vue';
import { api } from 'src/boot/axios';
import CategoryTabs from '../components/facturas/CategoryTabs.vue';
import InvoiceItemCard from '../components/facturas/InvoiceItemCard.vue';
import InvoiceDetailDialog from '../components/facturas/InvoiceDetailDialog.vue';
import ConfirmDialog from '../components/facturas/ConfirmDialog.vue';

useMeta({ title: 'Facturas — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;
const { showForCurrentRoute } = useTutorial();
// ponytail: delete solo OWNER; PAGADA->ANULADA conserva items, REGISTRADA borra
const isOwner = computed(() => authStore.user?.role === 'OWNER');

interface OptionItem {
  label: string;
  value: string;
  __isCreate?: boolean;
}

const rows = ref<Factura[]>([]);
const loading = shallowRef(false);
const filter = shallowRef('');
const editingId = shallowRef<string | null>(null);

const statusColor = (s: string) =>
  s === 'PAGADA' ? 'positive' : s === 'REGISTRADA' ? 'warning' : s === 'ANULADA' ? 'grey' : 'grey';

const statusLabel = (s: string) =>
  s === 'REGISTRADA' ? 'Pendiente' : s === 'PAGADA' ? 'Pagada' : s === 'ANULADA' ? 'Anulada' : s;

const filteredRows = computed(() => {
  if (!filter.value) return rows.value;
  const q = filter.value.toLowerCase();
  return rows.value.filter(
    (r) =>
      r.invoiceNumber.toLowerCase().includes(q) || (r.providerName ?? '').toLowerCase().includes(q),
  );
});

interface MonthGroup {
  label: string;
  items: Factura[];
}

const monthGroups = computed(() => {
  const groups = new Map<string, Factura[]>();
  for (const inv of filteredRows.value) {
    // ponytail: key por texto YYYY-MM — new Date() cambiaba de mes en UTC-5
    const key = inv.issueDate.slice(0, 7);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(inv);
  }
  const result: (MonthGroup & { key: string })[] = [];
  for (const [key, list] of groups.entries()) {
    const date = new Date(key + '-01T00:00:00');
    const label = date.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });
    result.push({ key, label, items: list });
  }
  // sort by key YYYY-MM desc (estable, no depende de locale)
  result.sort((a, b) => b.key.localeCompare(a.key));
  return result;
});

const allProducts = ref<ProductOption[]>([]);
const prodsData = ref<Producto[]>([]);
const productPresentationsMap = ref<Map<string, { label: string; value: string }[]>>(new Map());
const activeCategory = shallowRef('');
const providerOptions = ref<{ label: string; value: string }[]>([]);
const providerFilteredOptions = ref<OptionItem[]>([]);
const setupCategories = ref<SetupCategory[]>([]);
const setupUnits = ref<{ code: string; name: string }[]>([]);
const detailDialog = shallowRef(false);
const detailItem = shallowRef<Factura | null>(null);
const presentationNameMap = ref<Map<string, string>>(new Map());
const presentationConversionMap = ref<Map<string, number>>(new Map());
const colaboradores = ref<Collaborador[]>([]);
const gastosFijos = ref<GastoFijoRecurrente[]>([]);

function openDetail(f: Factura) {
  detailItem.value = f;
  detailDialog.value = true;
}

function collectCategoryCodes(cats: SetupCategory[], codes: Set<string>) {
  for (const c of cats) {
    codes.add(c.code);
    if (c.children) collectCategoryCodes(c.children, codes);
  }
}

function findCategoryInTree(cats: SetupCategory[], code: string): Set<string> {
  const codes = new Set<string>();
  function walk(nodes: SetupCategory[]): boolean {
    for (const c of nodes) {
      if (c.code === code) {
        codes.add(c.code);
        if (c.children) collectCategoryCodes(c.children, codes);
        return true;
      }
      if (c.children && walk(c.children)) return true;
    }
    return false;
  }
  walk(cats);
  return codes;
}

const unitNameMap = computed(() => {
  const map = new Map<string, string>();
  for (const u of setupUnits.value) map.set(u.code, u.name);
  return map;
});

// ponytail: deriva unidad base por producto para detail sin presentación (sin snapshot)
// UUID sin nombre en catálogo -> 'Standard' (evita filtrar IDs crudos)
const productBaseUnitMap = computed(() => {
  const map = new Map<string, string>();
  for (const p of prodsData.value) {
    const named = p.baseUnit ? unitNameMap.value.get(p.baseUnit) : undefined;
    map.set(p.id, named || (p.baseUnit && !isUuidLike(p.baseUnit) ? p.baseUnit : 'Standard'));
  }
  return map;
});

const filteredByProvider = computed(() => {
  const providerId = form.value.proveedorId;
  if (!providerId)
    return [...allProducts.value].sort((a, b) =>
      a.productName.localeCompare(b.productName, 'es', { sensitivity: 'base' }),
    );
  // ponytail: flexible + específico — genéricos (proveedorId=null) visibles en todos los items; sort O(n log n) para <500, BE pg_trgm si escala
  return allProducts.value
    .filter((p) => !p.proveedorId || p.proveedorId === providerId)
    .sort((a, b) => a.productName.localeCompare(b.productName, 'es', { sensitivity: 'base' }));
});

const filteredByCategory = computed(() => {
  if (!activeCategory.value) return filteredByProvider.value;
  const codes = findCategoryInTree(setupCategories.value, activeCategory.value);
  return codes.size
    ? filteredByProvider.value.filter((p) => codes.has(p.category))
    : filteredByProvider.value;
});

function unitOptions(productId: string | null): { label: string; value: string }[] {
  if (!productId) return [];
  return productPresentationsMap.value.get(productId) || [];
}

let keyCounter = 0;

interface ItemForm {
  _key: number;
  productoId: string | null;
  presentacionId: string | null;
  fueSuelto: boolean;
  cantidad: number | null;
  valor: number | null;
  descuento: number;
  itbmsTasa: number;
}

const CATEGORIA_SALARIOS = 'SALARIOS';
const CATEGORIA_OTRO = 'OTRO';

const dialogOpen = shallowRef(false);
const saving = shallowRef(false);
const form = ref<{
  proveedorId: string | null;
  fecha: string;
  tipo: string;
  metodoPago: string | null;
  descuentoGlobal: number;
  items: ItemForm[];
  total: number | null;
  categoria: string | null;
  colaboradorId: string | null;
  fechaDesde: string | null;
  fechaHasta: string | null;
}>({
  proveedorId: null,
  fecha: toLocalISODate(new Date()),
  tipo: 'FACTURA',
  metodoPago: null,
  descuentoGlobal: 0,
  items: [],
  total: null,
  categoria: null,
  colaboradorId: null,
  fechaDesde: null,
  fechaHasta: null,
});

function addItem() {
  form.value.items.push({
    _key: ++keyCounter,
    productoId: null,
    presentacionId: null,
    fueSuelto: false,
    cantidad: null,
    valor: null,
    descuento: 0,
    itbmsTasa: 0,
  });
}

function onProductoChange(item: ItemForm, productoId: string | null) {
  item.productoId = productoId;
  item.presentacionId = null;
  item.fueSuelto = false;
  const prod = allProducts.value.find((p) => p.value === productoId);
  item.valor = prod?.lastUnitPrice ?? null;
}

function onPresentacionChange(item: ItemForm, presId: string | null) {
  // ponytail: el Valor sigue al autollenado por unidad base SOLO si no lo tocaste — agnostico al producto
  const prevConv =
    item.presentacionId && item.presentacionId !== SUELTO
      ? (presentationConversionMap.value.get(item.presentacionId) ?? null)
      : null;
  const newConv =
    presId && presId !== SUELTO ? (presentationConversionMap.value.get(presId) ?? null) : null;
  const base = allProducts.value.find((p) => p.value === item.productoId)?.lastUnitPrice ?? null;
  item.presentacionId = presId;
  item.fueSuelto = presId === SUELTO;
  item.valor = scaledPriceOnPresentationChange(item.valor, base, newConv, prevConv);
}

function removeItem(i: number) {
  form.value.items.splice(i, 1);
}

const computedTotal = computed(() => {
  if (form.value.tipo === 'GASTO_OPERATIVO') return form.value.total || 0;
  // ponytail: derived — single source form.items, no watcher-assigned ref (reactivity.md)
  return calcBreakdown(form.value.items).total;
});

const computedBreakdown = computed(() => calcBreakdown(form.value.items));

const totalStr = ref('');

function onTotalInput(val: string | number | null) {
  totalStr.value = String(val ?? '')
    .replace(/[^0-9.]/g, '')
    .replace(/(\..*)\./g, '$1');
  const n = parseFloat(totalStr.value);
  if (Number.isFinite(n)) form.value.total = n;
  else form.value.total = null;
}

function formatTotal() {
  const n = parseFloat(totalStr.value.replace(/,/g, ''));
  if (!isNaN(n) && totalStr.value) {
    totalStr.value = n.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    form.value.total = n;
  } else {
    form.value.total = null;
  }
}

function rawTotal(val: number | null) {
  return val
    ? val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}

const colaboradorOptions = computed(() =>
  colaboradores.value
    .filter((c) => c.activo)
    .map((c) => ({
      label: `${c.nombre} · ${c.tipoPago} $${c.monto.toLocaleString('en-US')}`,
      value: c.id,
    })),
);

const gastoFijoCategorias = computed(() => {
  const out: { label: string; value: string; monto: number; proveedorId: string | null }[] = [];
  for (const g of gastosFijos.value) {
    if (!g.activo) continue;
    const prov = g.proveedorName ? ` — ${g.proveedorName}` : '';
    out.push({
      label: `${g.categoria}${prov} · $${g.monto.toLocaleString('en-US')}`,
      value: g.id,
      monto: g.monto,
      proveedorId: g.proveedorId,
    });
  }
  return out;
});

const categoriaOptions = computed(() => [
  { label: 'Salarios', value: CATEGORIA_SALARIOS },
  ...gastoFijoCategorias.value,
  { label: 'Otro', value: CATEGORIA_OTRO },
]);

const categoriaMap = computed(() => new Map(gastosFijos.value.map((g) => [g.id, g.categoria])));

const selectedColaborador = computed(
  () => colaboradores.value.find((c) => c.id === form.value.colaboradorId) || null,
);

const diasEnRango = computed(() => {
  if (!form.value.fechaDesde || !form.value.fechaHasta) return 0;
  const ms = new Date(form.value.fechaHasta).getTime() - new Date(form.value.fechaDesde).getTime();
  // ponytail: cuenta días calendario (inclusive); días laborables vía config_laboral si hace falta
  return Math.max(1, Math.round(ms / 86400000) + 1);
});

function applyCategoria() {
  if (form.value.tipo !== 'GASTO_OPERATIVO') return;
  form.value.colaboradorId = null;
  const gf = gastoFijoCategorias.value.find((c) => c.value === form.value.categoria);
  if (!gf) return;   // 'Otro' o categoría personalizada: respetar monto que ya hay escrito
  form.value.total = gf.monto;
  form.value.proveedorId = gf.proveedorId ?? null;
  providerFilteredOptions.value = [...providerOptions.value];
  totalStr.value = rawTotal(form.value.total);
}

function applySalario() {
  if (form.value.tipo !== 'GASTO_OPERATIVO' || form.value.categoria !== CATEGORIA_SALARIOS) return;
  const col = selectedColaborador.value;
  form.value.total = col
    ? col.tipoPago === 'DIARIO'
      ? col.monto * diasEnRango.value
      : col.monto
    : null;
  totalStr.value = rawTotal(form.value.total);
}

watch(() => form.value.categoria, applyCategoria);
watch(() => [form.value.colaboradorId, form.value.fechaDesde, form.value.fechaHasta], applySalario);

function providerFilter(val: string, update: (fn: () => void) => void) {
  update(() => {
    if (!val) {
      providerFilteredOptions.value = [...providerOptions.value];
      return;
    }
    const needle = val.toLowerCase();
    const filtered: OptionItem[] = providerOptions.value.filter((p) =>
      p.label.toLowerCase().includes(needle),
    );
    if (filtered.length === 0 && val.trim()) {
      filtered.push({
        label: `+ Crear "${val.trim()}"`,
        value: `__CREATE__${val.trim()}`,
        __isCreate: true,
      });
    }
    providerFilteredOptions.value = filtered;
  });
}

function onProviderSelected(val: string | null) {
  if (!val || !val.startsWith('__CREATE__')) return;
  const name = val.replace('__CREATE__', '');
  proveedorService
    .create({ tenantId: tenantId as string, name })
    .then((res) => {
      const newOpt = { label: res.data.name, value: res.data.id };
      providerOptions.value.push(newOpt);
      providerFilteredOptions.value = [...providerOptions.value];
      form.value.proveedorId = res.data.id;
      $q.notify({ type: 'positive', message: `Proveedor "${name}" creado` });
    })
    .catch((err: unknown) => {
      form.value.proveedorId = null;
      $q.notify({
        type: 'negative',
        message: err instanceof Error ? err.message : 'Error al crear proveedor',
      });
    });
}

async function openCreate(tipo: 'FACTURA' | 'GASTO_OPERATIVO' = 'FACTURA') {
  form.value = {
    proveedorId: null,
    fecha: toLocalISODate(new Date()),
    tipo,
    metodoPago: null,
    descuentoGlobal: 0,
    items: [],
    total: null,
    categoria: null,
    colaboradorId: null,
    fechaDesde: null,
    fechaHasta: null,
  };
  totalStr.value = '';
  editingId.value = null;
  keyCounter = 0;
  providerFilteredOptions.value = [...providerOptions.value];
  dialogOpen.value = true;
  await nextTick();
  addItem();
}

async function openEdit(factura: Factura) {
  if (!tenantId) return;
  editingId.value = factura.id;
  try {
    const res = await facturaService.getById(factura.id, tenantId);
    const f = res.data;
    const gastoOperativo = f.type === 'GASTO_OPERATIVO';
    form.value = {
      proveedorId: f.providerId,
      fecha: f.issueDate,
      tipo: f.type,
      metodoPago: f.paymentMethod || null,
      descuentoGlobal: Number(f.globalDiscount || 0),
      items: gastoOperativo
        ? []
        : f.items.map((item) => ({
            _key: ++keyCounter,
            productoId: item.productId,
            presentacionId: item.fueSuelto ? SUELTO : item.presentacionId,
            fueSuelto: item.fueSuelto ?? false,
            cantidad: item.cantidadPresentacion
              ? Number(item.cantidadPresentacion)
              : item.conversionFactor && item.conversionFactor > 1
                ? Number(item.quantity) / item.conversionFactor
                : Number(item.quantity),
            valor: item.valorPresentacion
              ? Number(item.valorPresentacion)
              : item.conversionFactor && item.conversionFactor > 1
                ? Number(item.unitPrice) * item.conversionFactor
                : Number(item.unitPrice),
            descuento:
              item.descuentoEsPorcentaje && item.descuentoInput
                ? Number(item.descuentoInput)
                : item.discount && item.quantity
                  ? (Number(item.discount) / Number(item.quantity)) * 100
                  : 0,
            itbmsTasa: item.itbmsTasa ?? 0,
          })),
      total: gastoOperativo ? Number(f.total || 0) : null,
      categoria: gastoOperativo ? CATEGORIA_OTRO : null,
      colaboradorId: null,
      fechaDesde: null,
      fechaHasta: null,
    };
    totalStr.value = gastoOperativo ? rawTotal(Number(f.total || 0)) : '';
    providerFilteredOptions.value = [...providerOptions.value];
    dialogOpen.value = true;
    await nextTick();
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar factura para editar',
    });
  }
}

watch(() => form.value.proveedorId, onProviderSelected);

function mapProductsToOptions(prods: Producto[]): ProductOption[] {
  const catMap = new Map<string, string>();
  function walkCats(cats: SetupCategory[]) {
    for (const c of cats) {
      catMap.set(c.code, c.name);
      if (c.children?.length) walkCats(c.children);
    }
  }
  walkCats(setupCategories.value);

  return prods.map((p) => ({
    label: `${p.name}${p.proveedorName ? ` · ${p.proveedorName}` : ''}`,
    value: p.id,
    productName: p.name,
    sku: p.sku,
    category: p.category,
    categoryName: catMap.get(p.category) || p.category,
    proveedorId: p.proveedorId,
    proveedorName: p.proveedorName,
    lastUnitPrice: p.lastUnitPrice,
  }));
}

async function loadDependencies() {
  if (!tenantId) return;
  try {
    const [provs, setupRes, prodsRes, colRes, gfRes] = await Promise.all([
      proveedorService.getAll(tenantId),
      api.get<SetupInfo>(`/core/setup/${tenantId}`),
      // ponytail: cache-first getAll (productos @Cacheable) — full list, no 100 truncation, category search stays client
      productoService.getAll(tenantId),
      costoService.getAllCollaboradores(tenantId),
      costoService.getAllGastosFijos(tenantId),
    ]);
    colaboradores.value = colRes.data;
    gastosFijos.value = gfRes.data;
    setupCategories.value = setupRes.data.categories || [];
    setupUnits.value = setupRes.data.units || [];
    const provOpts = provs.data.map((p) => ({ label: p.name, value: p.id }));
    providerOptions.value = provOpts;
    providerFilteredOptions.value = [...provOpts];
    const prods: Producto[] = Array.isArray(prodsRes.data)
      ? prodsRes.data
      : (prodsRes.data as unknown as { content: Producto[] }).content;
    prodsData.value = prods;
    allProducts.value = mapProductsToOptions(prods);
    const presMap = new Map<string, { label: string; value: string }[]>();
    const presNameMap = new Map<string, string>();
    const convMap = new Map<string, number>();
    const unitByCode = new Map(setupUnits.value.map((u) => [u.code, u.name]));
    for (const p of prods) {
      // ponytail: suelto muestra la unidad base real (Kg, Lb...), 'Standard' solo si no hay nombre
      const named = p.baseUnit ? unitByCode.get(p.baseUnit) : undefined;
      const baseLabel = named || (p.baseUnit && !isUuidLike(p.baseUnit) ? p.baseUnit : 'Standard');
      const unitOpts: { label: string; value: string }[] = [{ label: baseLabel, value: SUELTO }];
      for (const pres of p.presentaciones || []) {
        unitOpts.push({ label: pres.name, value: pres.id });
        presNameMap.set(pres.id, pres.name);
        convMap.set(pres.id, pres.conversion);
      }
      presMap.set(p.id, unitOpts);
    }
    presentationNameMap.value = presNameMap;
    presentationConversionMap.value = convMap;
    productPresentationsMap.value = presMap;
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Error al cargar datos del formulario';
    $q.notify({ type: 'negative', message: msg });
  }
}

async function save() {
  if (!tenantId) return;
  const gastoOperativo = form.value.tipo === 'GASTO_OPERATIVO';
  if (gastoOperativo && !(form.value.total && form.value.total > 0)) {
    $q.notify({ type: 'negative', message: 'Ingresa un monto total mayor a cero' });
    return;
  }
  saving.value = true;
  try {
    // ponytail: pre-flight en palabras del usuario — el 400 del backend es red, no UX
    if (!gastoOperativo) {
      const incompleto =
        !form.value.items.length || form.value.items.some((i) => !isItemComplete(i));
      if (incompleto) {
        $q.notify({
          type: 'warning',
          message: 'Cada item necesita producto, unidad, cantidad y valor',
        });
        saving.value = false;
        return;
      }
    }
    const resolvedCategoria = gastoOperativo
      ? form.value.categoria
        ? (categoriaMap.value.get(form.value.categoria) ?? form.value.categoria)
        : null
      : null;
    const payload: FacturaRequest = {
      tenantId,
      proveedorId: form.value.proveedorId,
      colaboradorId: gastoOperativo ? form.value.colaboradorId : null,
      fecha: form.value.fecha,
      tipo: form.value.tipo,
      metodoPago: form.value.metodoPago,
      category: resolvedCategoria,
      descuentoGlobal: form.value.descuentoGlobal || 0,
      total: gastoOperativo ? form.value.total : null,
      items: gastoOperativo
        ? []
        : form.value.items.map((item) => {
            const conv =
              !item.fueSuelto && item.presentacionId
                ? presentationConversionMap.value.get(item.presentacionId) || 1
                : 1;
            const val = item.valor || 0;
            const pres = toPresentacionPayload(item);
            return {
              productoId: item.productoId!,
              presentacionId: pres.presentacionId,
              fueSuelto: pres.fueSuelto,
              cantidadPresentacion: item.cantidad || 0,
              valorPresentacion: val,
              precioUnitario: conv > 0 ? val / conv : val,
              descuento: (item.cantidad || 0) * val * ((item.descuento || 0) / 100),
              descuentoInput: item.descuento || 0,
              descuentoEsPorcentaje: true,
              itbmsTasa: item.itbmsTasa ?? 0,
            };
          }),
    };
    let res;
    if (editingId.value) {
      res = await facturaService.update(editingId.value, payload);
      const idx = rows.value.findIndex((r) => r.id === editingId.value);
      if (idx >= 0) rows.value[idx] = res.data;
      $q.notify({ type: 'positive', message: 'Factura actualizada: ' + res.data.invoiceNumber });
    } else {
      res = await facturaService.create(payload);
      rows.value.unshift(res.data);
      $q.notify({ type: 'positive', message: 'Factura creada: ' + res.data.invoiceNumber });
    }
    dialogOpen.value = false;
    editingId.value = null;
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Error al guardar factura';
    $q.notify({ type: 'negative', message: msg });
  } finally {
    saving.value = false;
  }
}

const payDialog = shallowRef(false);
const payingItem = shallowRef<Factura | null>(null);
const paying = shallowRef(false);

function confirmPay(f: Factura) {
  payingItem.value = f;
  payDialog.value = true;
}

async function pay() {
  if (!payingItem.value || !tenantId) return;
  paying.value = true;
  try {
    const res = await facturaService.pay(payingItem.value.id, tenantId);
    const idx = rows.value.findIndex((r) => r.id === payingItem.value!.id);
    if (idx >= 0) rows.value[idx] = res.data;
    payDialog.value = false;
    $q.notify({ type: 'positive', message: 'Factura pagada' });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al pagar factura',
    });
  } finally {
    paying.value = false;
    payingItem.value = null;
  }
}

const deleteDialog = shallowRef(false);
const deletingItem = shallowRef<Factura | null>(null);
const deleting = shallowRef(false);

function confirmDelete(f: Factura) {
  deletingItem.value = f;
  deleteDialog.value = true;
}

async function remove() {
  if (!deletingItem.value || !tenantId) return;
  deleting.value = true;
  try {
    await facturaService.remove(deletingItem.value.id, tenantId);
    rows.value = rows.value.filter((r) => r.id !== deletingItem.value!.id);
    deleteDialog.value = false;
    $q.notify({ type: 'positive', message: 'Factura eliminada' });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al eliminar factura',
    });
  } finally {
    deleting.value = false;
    deletingItem.value = null;
  }
}

async function load() {
  if (!tenantId) return;
  loading.value = true;
  try {
    const res = await facturaService.getAll(tenantId);
    rows.value = res.data;
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar facturas',
    });
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  if (!tenantId) {
    void showForCurrentRoute();
    return;
  }
  await Promise.all([load(), loadDependencies()]);
  void showForCurrentRoute();
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});

watch(
  () => dialogOpen.value,
  (open) => {
    if (!open) editingId.value = null;
  },
);

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    void openCreate('FACTURA');
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's' && dialogOpen.value) {
    e.preventDefault();
    void save();
  }
}
</script>

<template>
  <q-page class="core-page facturas-page">
    <div class="page-header fade-in-up" data-tour="facturas">
      <div class="page-header__main">
        <h1 class="page-title">Facturas</h1>
        <p class="page-subtitle">Registro de facturas de proveedores</p>
      </div>
      <div class="page-header__signature" aria-hidden="true">
        <span class="signature-dot" />
        <span class="signature-line" />
      </div>
    </div>

    <div class="facturas-toolbar">
      <q-input
        dark
        dense
        filled
        v-model="filter"
        placeholder="Buscar por número o proveedor..."
        class="toolbar-search"
      >
        <template v-slot:prepend><q-icon name="search" /></template>
      </q-input>
      <q-space class="toolbar-spacer" />
      <q-btn
        outline
        color="accent"
        icon="sym_r_payments"
        label="Gasto rápido"
        @click="openCreate('GASTO_OPERATIVO')"
        no-caps
        class="toolbar-secondary"
      />
      <q-btn
        icon="sym_r_add"
        label="Factura"
        @click="openCreate('FACTURA')"
        no-caps
        class="toolbar-new"
      />
    </div>

    <div v-if="!loading && !filteredRows.length" class="q-my-lg">
      <EmptyState
        icon="sym_r_receipt_long"
        title="Sin facturas"
        message="Registra tu primera factura de proveedor para comenzar."
      />
    </div>

    <div v-if="loading" class="q-gutter-y-md">
      <div v-for="n in 3" :key="n">
        <q-skeleton type="text" dark animation="pulse" class="q-mb-sm" width="30%" height="16px" />
        <q-skeleton type="rect" dark animation="pulse" class="q-mb-xs" height="40px" />
        <q-skeleton type="rect" dark animation="pulse" class="q-mb-xs" height="40px" />
      </div>
    </div>

    <div v-for="group in monthGroups" :key="group.label" class="q-mb-lg">
      <div class="month-group-header">
        <span class="month-group-label">{{ group.label }}</span>
        <span class="month-group-count"
          >{{ group.items.length }} factura{{ group.items.length !== 1 ? 's' : '' }}</span
        >
      </div>

      <div v-for="inv in group.items" :key="inv.id" class="invoice-row">
        <div class="invoice-row__info">
          <div class="invoice-row__number">{{ inv.invoiceNumber }}</div>
          <div class="invoice-row__provider">{{ inv.providerName || '—' }}</div>
        </div>
        <div class="invoice-row__date">{{ inv.issueDate }}</div>
        <div class="invoice-row__total">{{ formatCurrency(inv.total) }}</div>
        <div class="invoice-row__status">
          <q-badge :color="statusColor(inv.status)" class="q-px-sm q-py-xs">{{
            statusLabel(inv.status)
          }}</q-badge>
        </div>
        <div class="invoice-row__actions">
          <q-btn
            flat
            dense
            round
            icon="sym_r_visibility"
            color="accent"
            size="sm"
            @click="openDetail(inv)"
            aria-label="Ver detalles"
          />
          <q-btn
            v-if="inv.status === 'REGISTRADA'"
            flat
            dense
            round
            icon="sym_r_edit"
            color="primary"
            size="sm"
            @click="openEdit(inv)"
            aria-label="Editar"
          />
          <q-btn
            v-if="inv.status === 'REGISTRADA'"
            flat
            dense
            round
            icon="sym_r_paid"
            color="positive"
            size="sm"
            @click="confirmPay(inv)"
            aria-label="Marcar como pagada"
          />
          <q-btn
            v-if="isOwner && (inv.status === 'REGISTRADA' || inv.status === 'PAGADA')"
            flat
            dense
            round
            icon="sym_r_delete"
            color="negative"
            size="sm"
            @click="confirmDelete(inv)"
            :aria-label="inv.status === 'PAGADA' ? 'Anular factura pagada' : 'Eliminar'"
          />
        </div>
      </div>
    </div>

    <!-- Create Invoice Dialog -->
    <q-dialog
      v-model="dialogOpen"
      dark
      transition-show="scale"
      transition-hide="fade"
    >
      <q-card
        dark
        class="bg-surface-pine invoice-dialog"
        style="width: 96vw; max-width: 980px; max-height: 92vh"
      >
        <q-form @submit.prevent="save" class="column no-wrap invoice-dialog__form">
          <!-- Header -->
          <div class="invoice-dialog__header">
            <div class="invoice-dialog__title">
              <q-icon name="receipt_long" size="1.2rem" class="text-accent" />
              <span class="invoice-dialog__title-text">{{
                editingId
                  ? 'Editar'
                  : form.tipo === 'GASTO_OPERATIVO'
                    ? 'Gasto Rápido'
                    : 'Nueva Factura'
              }}</span>
            </div>
            <q-btn flat round icon="sym_r_close" color="accent" v-close-popup size="sm" />
          </div>

          <q-separator dark class="opacity-20" />

          <!-- Scrollable body -->
          <q-card-section class="invoice-dialog__body col">
            <div class="invoice-dialog__layout row q-col-gutter-md">
              <!-- Left: form fields -->
              <div class="col-12 col-lg-5 invoice-dialog__col">
                <div class="invoice-dialog__section">
                  <div class="invoice-dialog__section-title">
                    <q-icon name="receipt" size="0.85rem" class="text-accent" />
                    <span>Datos de la factura</span>
                  </div>
                  <div class="row q-col-gutter-x-sm q-col-gutter-y-sm">
                    <div class="col-12">
                      <q-select
                        dark
                        dense
                        v-model="form.proveedorId"
                        :options="providerFilteredOptions"
                        label="Proveedor"
                        label-color="accent"
                        :rules="form.tipo !== 'GASTO_OPERATIVO' ? [(v) => !!v || 'Requerido'] : []"
                        map-options
                        emit-value
                        use-input
                        @filter="providerFilter"
                        popup-content-class="product-dropdown"
                      >
                        <template v-slot:no-option
                          ><q-item
                            ><q-item-section class="text-accent text-caption"
                              >Escribe el nombre para crearlo</q-item-section
                            ></q-item
                          ></template
                        >
                      </q-select>
                    </div>
                    <div class="col-12 col-sm-6">
                      <q-input
                        dark
                        dense
                        v-model="form.fecha"
                        label="Fecha"
                        label-color="accent"
                        type="date"
                        :rules="[(v) => !!v || 'Requerido']"
                      />
                    </div>
                    <div class="col-12 col-sm-6">
                      <q-select
                        dark
                        dense
                        v-model="form.metodoPago"
                        :options="['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'CHEQUE']"
                        label="Método de pago"
                        label-color="accent"
                        clearable
                      />
                    </div>
                  </div>
                </div>

                <!-- Gasto operativo -->
                <div v-if="form.tipo === 'GASTO_OPERATIVO'" class="invoice-dialog__section q-mt-md">
                  <div class="invoice-dialog__section-title">
                    <q-icon name="payments" size="0.85rem" class="text-accent" />
                    <span>Categoría</span>
                  </div>
                  <div class="q-gutter-y-sm">
                    <q-select
                      dark
                      dense
                      v-model="form.categoria"
                      :options="categoriaOptions"
                      label="Categoría"
                      label-color="accent"
                      map-options
                      emit-value
                      :rules="[(v) => !!v || 'Requerido']"
                    />
                    <q-select
                      v-if="form.categoria === 'SALARIOS'"
                      dark
                      dense
                      v-model="form.colaboradorId"
                      :options="colaboradorOptions"
                      label="Colaborador"
                      label-color="accent"
                      map-options
                      emit-value
                      clearable
                    />
                    <div
                      v-if="
                        form.categoria === 'SALARIOS' && selectedColaborador?.tipoPago === 'DIARIO'
                      "
                      class="row q-col-gutter-x-sm"
                    >
                      <div class="col-6">
                        <q-input dark dense v-model="form.fechaDesde" label="Desde" type="date" />
                      </div>
                      <div class="col-6">
                        <q-input dark dense v-model="form.fechaHasta" label="Hasta" type="date" />
                      </div>
                    </div>
                    <div
                      v-if="
                        form.categoria === 'SALARIOS' && selectedColaborador?.tipoPago === 'DIARIO'
                      "
                      class="text-caption text-accent q-mt-xs"
                    >
                      {{ diasEnRango }} días × ${{
                        selectedColaborador.monto.toLocaleString('en-US')
                      }}
                      = {{ formatCurrency(diasEnRango * selectedColaborador.monto) }}
                    </div>
                    <q-input
                      dark
                      dense
                      filled
                      :model-value="totalStr"
                      label="Total / Monto"
                      label-color="accent"
                      placeholder="0.00"
                      prefix="$"
                      :rules="[() => !!form.total || 'Requerido']"
                      @update:model-value="onTotalInput"
                      @blur="formatTotal"
                    />
                  </div>
                </div>
              </div>

              <!-- Right: items + totals -->
              <div
                v-if="form.tipo !== 'GASTO_OPERATIVO'"
                class="col-12 col-lg-7 invoice-dialog__col"
              >
                <div class="invoice-dialog__section">
                  <div class="invoice-dialog__items-header">
                    <div class="invoice-dialog__items-title">
                      <q-icon name="list" size="0.85rem" class="text-accent" />
                      <span>Items</span>
                      <q-badge
                        v-if="form.items.length"
                        :label="form.items.length"
                        color="accent"
                        text-color="dark"
                        class="q-ml-xs"
                      />
                    </div>
                    <span v-if="form.items.length" class="invoice-dialog__items-total">{{
                      formatCurrency(computedTotal)
                    }}</span>
                  </div>

                  <CategoryTabs
                    v-if="allProducts.length"
                    v-model="activeCategory"
                    :categories="setupCategories"
                  />

                  <div class="invoice-dialog__items">
                    <InvoiceItemCard
                      v-for="(item, i) in form.items"
                      :key="item._key"
                      :item="item"
                      :index="i"
                      :product-options="filteredByCategory"
                      :all-product-options="allProducts"
                      :unit-options="unitOptions(item.productoId)"
                      :presentation-conversion-map="presentationConversionMap"
                      @update:productoId="onProductoChange(item, $event)"
                      @update:presentacionId="onPresentacionChange(item, $event)"
                      @update:cantidad="item.cantidad = $event"
                      @update:valor="item.valor = $event"
                      @update:descuento="item.descuento = $event"
                      @update:itbmsTasa="item.itbmsTasa = $event"
                      @remove="removeItem(i)"
                    />
                    <div v-if="!form.items.length" class="invoice-dialog__empty">
                      <q-icon
                        name="add_shopping_cart"
                        size="1.6rem"
                        class="text-accent"
                        style="opacity: 0.5"
                      />
                      <span>Sin items — agrega el primero</span>
                    </div>
                  </div>

                  <div class="invoice-dialog__add">
                    <q-btn
                      outline
                      color="accent"
                      icon="sym_r_add"
                      label="Agregar item"
                      @click="addItem"
                      no-caps
                      size="sm"
                    />
                  </div>
                </div>
              </div>
            </div>
          </q-card-section>

          <!-- Sticky totals bar -->
          <div
            v-if="form.items.length || form.tipo === 'GASTO_OPERATIVO'"
            class="invoice-dialog__totals-bar"
          >
            <div v-if="form.items.length" class="invoice-dialog__breakdown">
              <span>Sin ITBMS {{ formatCurrency(computedBreakdown.exento) }}</span>
              <span class="sep">·</span>
              <span>Con ITBMS {{ formatCurrency(computedBreakdown.gravado) }}</span>
              <span class="sep">·</span>
              <span class="itbms">ITBMS {{ formatCurrency(computedBreakdown.itbms) }}</span>
            </div>
            <div class="invoice-dialog__total">
              <span class="invoice-dialog__total-label">Total</span>
              <span class="invoice-dialog__total-val">{{ formatCurrency(computedTotal) }}</span>
            </div>
          </div>

          <q-separator dark class="opacity-20" />

          <!-- Sticky footer -->
          <div class="invoice-dialog__footer">
            <q-btn flat label="Cancelar" color="accent" v-close-popup no-caps />
            <q-btn
              type="submit"
              label="Guardar"
              color="primary"
              :loading="saving"
              no-caps
              icon="sym_r_save"
            />
          </div>
        </q-form>
      </q-card>
    </q-dialog>

    <ConfirmDialog
      v-model="payDialog"
      icon="sym_r_paid"
      icon-color="positive"
      confirm-label="Confirmar Pago"
      confirm-color="positive"
      :loading="paying"
      @confirm="pay"
    >
      Marcar como pagada la factura <strong>{{ payingItem?.invoiceNumber }}</strong
      >?
    </ConfirmDialog>

    <ConfirmDialog
      v-model="deleteDialog"
      icon="sym_r_warning"
      icon-color="negative"
      :confirm-label="deletingItem?.status === 'PAGADA' ? 'Anular' : 'Eliminar'"
      confirm-color="negative"
      :loading="deleting"
      @confirm="remove"
    >
      <template v-if="deletingItem?.status === 'PAGADA'">
        ¿Anular factura pagada <strong>{{ deletingItem?.invoiceNumber }}</strong
        >? Se revertirá el stock.
      </template>
      <template v-else>
        ¿Eliminar factura <strong>{{ deletingItem?.invoiceNumber }}</strong
        >?
      </template>
    </ConfirmDialog>

    <InvoiceDetailDialog
      :factura="detailItem"
      v-model="detailDialog"
      :presentation-name-map="presentationNameMap"
      :categoria-map="categoriaMap"
      :product-base-unit-map="productBaseUnitMap"
    />
  </q-page>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 8px;
}

.page-title {
  font-family: var(--pq-font-display);
  font-weight: 700;
  font-size: clamp(22px, 4vw, 28px);
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--pq-text);
  margin: 0;
}

.page-subtitle {
  font-family: var(--pq-font-body);
  font-weight: 500;
  font-size: 13px;
  line-height: 1.5;
  color: var(--pq-text-muted);
  margin: 4px 0 0;
}

.page-header__signature {
  display: none;
  align-items: center;
  gap: 8px;
}

@media (min-width: 1024px) {
  .page-header__signature {
    display: flex;
  }
}

.signature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pq-accent);
}

.signature-line {
  width: 40px;
  height: 1px;
  background: var(--pq-accent);
  opacity: 0.4;
}

.invoice-dialog {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.invoice-dialog__form {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.invoice-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  flex: 0 0 auto;
}

.invoice-dialog__form > .q-separator {
  flex: 0 0 auto;
}

.invoice-dialog__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--pq-font-display);
  color: var(--pq-text);
}

.invoice-dialog__title-text {
  font-family: var(--pq-font-display);
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--pq-text);
}

.invoice-dialog__body {
  flex: 1 1 auto;
  overflow-y: auto;
  min-height: 0;
  padding: 12px 16px;
}

.invoice-dialog__layout {
  align-items: flex-start;
}

.invoice-dialog__col {
  min-width: 0;
}

.invoice-dialog__section {
  background: color-mix(in srgb, var(--pq-surface) 60%, transparent);
  border: 1px solid color-mix(in srgb, var(--pq-border) 12%, transparent);
  border-radius: var(--pq-radius-md);
  padding: 10px 12px;
}

.invoice-dialog__section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--pq-font-display);
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--pq-text-muted);
  margin-bottom: 8px;
}

.invoice-dialog__items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0 4px;
}

.invoice-dialog__items-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--pq-text-muted);
}

.invoice-dialog__items-total {
  font-size: 0.85rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--pq-accent);
}

.invoice-dialog__items {
  min-height: 0;
}

.invoice-dialog__add {
  padding: 8px 0 4px;
}

.invoice-dialog__add :deep(.q-btn) {
  width: 100%;
  border-style: dashed;
  opacity: 0.5;
}

.invoice-dialog__add :deep(.q-btn:hover) {
  opacity: 1;
}

.invoice-dialog__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px 8px;
  color: var(--pq-text-muted);
  font-size: 0.82rem;
  border: 1px dashed color-mix(in srgb, var(--pq-border) 20%, transparent);
  border-radius: var(--pq-radius-md);
  margin: 4px 0;
}

.invoice-dialog__totals-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px;
  background: var(--pq-surface);
  flex: 0 0 auto;
  flex-wrap: wrap;
}

.invoice-dialog__totals-bar .invoice-dialog__breakdown {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--pq-font-utility);
  font-variant-numeric: tabular-nums;
  font-size: 0.75rem;
  color: var(--pq-text-muted);
  flex-wrap: wrap;
}

.invoice-dialog__totals-bar .invoice-dialog__breakdown .sep {
  opacity: 0.4;
}

.invoice-dialog__totals-bar .invoice-dialog__breakdown .itbms {
  font-weight: 600;
  color: var(--pq-accent);
}

.invoice-dialog__totals-bar .invoice-dialog__total {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.invoice-dialog__totals-bar .invoice-dialog__total-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--pq-accent-muted);
}

.invoice-dialog__totals-bar .invoice-dialog__total-val {
  font-family: var(--pq-font-utility);
  font-size: 1.15rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--pq-accent);
}

.invoice-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 12px;
  background: var(--pq-surface);
  flex: 0 0 auto;
}

@media (max-width: 599px) {
  .invoice-dialog {
    width: 100vw !important;
    max-width: 100vw !important;
    max-height: 100vh !important;
    max-height: 100dvh !important;
    border-radius: 0 !important;
  }
}

/* tablet y abajo: botones full-width apilados para que siempre quepan */
@media (max-width: 767px) {
  .invoice-dialog__footer {
    flex-direction: column-reverse;
  }
  .invoice-dialog__footer .q-btn {
    width: 100%;
    min-height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .invoice-dialog__add :deep(.q-btn),
  .item-card {
    transition: none !important;
  }
}

.product-dropdown {
  font-size: 0.82rem;
}

.facturas-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.toolbar-search {
  flex: 1 1 220px;
  min-width: 200px;
}

.toolbar-spacer {
  display: none;
}

@media (min-width: 640px) {
  .toolbar-spacer {
    display: block;
    flex: 1;
  }
  .toolbar-search {
    flex: 1 1 320px;
    min-width: 280px;
    max-width: 560px;
  }
}

.toolbar-new {
  min-height: 40px;
  background: var(--pq-accent) !important;
  color: var(--pq-background) !important;
  font-weight: 600;
  border-radius: var(--pq-radius-sm);
  box-shadow: 0 2px 8px rgba(163, 120, 94, 0.25);
  transition: background-color 0.16s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.16s ease, transform 0.1s ease;
}

.toolbar-new:hover {
  background: var(--pq-accent-hover) !important;
  box-shadow: 0 4px 16px rgba(163, 120, 94, 0.35);
  transform: translateY(-1px);
}

.toolbar-new:active {
  transform: scale(0.985) translateY(0);
}

.toolbar-new:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

.toolbar-secondary {
  min-height: 40px;
  border-radius: var(--pq-radius-sm);
  transition: background-color 0.16s ease, transform 0.1s ease;
}

.toolbar-secondary:hover {
  transform: translateY(-1px);
}

@media (pointer: coarse) {
  .toolbar-new,
  .toolbar-secondary {
    min-height: 44px;
  }
}

@media (max-width: 639px) {
  .facturas-toolbar {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .toolbar-search,
  .toolbar-new,
  .toolbar-secondary {
    width: 100%;
    min-width: 0;
    max-width: none;
  }

  .toolbar-new,
  .toolbar-secondary {
    min-height: 44px;
  }

  .facturas-toolbar :deep(.q-space) {
    display: none;
  }
}

.month-group-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid color-mix(in srgb, var(--pq-border) 15%, transparent);
  margin-bottom: 8px;
}

.month-group-label {
  font-weight: 700;
  text-transform: capitalize;
  color: var(--pq-text);
  font-size: 0.95rem;
}

.month-group-count {
  color: var(--pq-text-muted);
  font-size: 0.8rem;
}

.invoice-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  row-gap: 4px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background var(--pq-motion-fast);
  border-bottom: 1px solid color-mix(in srgb, var(--pq-border) 6%, transparent);
}

.invoice-row:hover {
  background: color-mix(in srgb, var(--pq-surface) 30%, transparent);
}

.invoice-row__info {
  flex: 1;
  min-width: 0;
}

.invoice-row__number {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invoice-row__provider {
  font-size: 0.8rem;
  color: var(--pq-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invoice-row__date {
  font-size: 0.8rem;
  color: var(--pq-text-muted);
  margin-right: 16px;
  white-space: nowrap;
}

.invoice-row__total {
  font-weight: 700;
  margin-right: 16px;
  font-family: var(--pq-font-utility);
  white-space: nowrap;
  color: var(--pq-accent);
}

.invoice-row__status {
  margin-right: 16px;
}

.invoice-row__actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

@media (max-width: 599px) {
  .invoice-row {
    flex-wrap: wrap;
    gap: 4px;
    padding: 8px 10px;
  }

  .invoice-row__date {
    display: none;
  }

  .invoice-row__total {
    margin-right: 8px;
  }

  .invoice-row__status {
    margin-right: 0;
    margin-left: auto;
  }

  .invoice-row__actions {
    width: 100%;
    justify-content: flex-end;
    padding-top: 4px;
    border-top: 1px solid color-mix(in srgb, var(--pq-border) 6%, transparent);
  }
}
</style>
