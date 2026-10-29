<script setup lang="ts">
import { ref, shallowRef, computed, watch, onMounted, onUnmounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { useTutorial } from 'src/composables/useTutorial';
import { proveedorService } from '../services/proveedor.service';
import type { Proveedor, ProveedorRequest } from '../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

useMeta({ title: 'Proveedores — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId;
const { showForCurrentRoute } = useTutorial();

const PAGE_SIZE = 9
const rows = ref<Proveedor[]>([]);
const loading = shallowRef(false);
const search = shallowRef('');
const page = shallowRef(1)

const filtrados = computed(() => {
  if (!search.value) return rows.value;
  const q = search.value.toLowerCase();
  return rows.value.filter(
    (r) =>
      r.name.toLowerCase().includes(q) ||
      r.contactName?.toLowerCase().includes(q) ||
      r.contactEmail?.toLowerCase().includes(q),
  );
});

// ponytail: A-Z client sort (stable) + slice, no BE pagination needed for <150 rows
const sortedFiltrados = computed(() =>
  [...filtrados.value].sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase())),
)
const totalPages = computed(() => Math.max(1, Math.ceil(sortedFiltrados.value.length / PAGE_SIZE)))
const paginated = computed(() =>
  sortedFiltrados.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE),
)

async function load() {
  if (!tenantId) return
  loading.value = true;
  try {
    const res = await proveedorService.getAll(tenantId);
    rows.value = res.data;
    // keep page in bounds after reload
    if (page.value > totalPages.value) page.value = totalPages.value
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar proveedores',
    });
  } finally {
    loading.value = false;
  }
}

watch(search, () => { page.value = 1 })

const dialogOpen = shallowRef(false);
const editingId = shallowRef<string | null>(null);
const saving = shallowRef(false);
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const form = ref<ProveedorRequest>({
  tenantId: tenantId as string,
  name: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
});

const deleteDialog = shallowRef(false);
const deletingItem = shallowRef<Proveedor | null>(null);
const deleting = shallowRef(false);

function openCreate() {
  editingId.value = null;
  form.value = { tenantId: tenantId as string, name: '', contactName: '', contactPhone: '', contactEmail: '' };
  dialogOpen.value = true;
}

function openEdit(p: Proveedor) {
  editingId.value = p.id;
  form.value = {
    tenantId: p.tenantId,
    name: p.name,
    contactName: p.contactName,
    contactPhone: p.contactPhone,
    contactEmail: p.contactEmail,
  };
  dialogOpen.value = true;
}

async function save() {
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    if (editingId.value) {
      const res = await proveedorService.update(editingId.value, form.value);
      const idx = rows.value.findIndex((r) => r.id === editingId.value);
      if (idx >= 0) rows.value[idx] = res.data;
    } else {
      const res = await proveedorService.create(form.value);
      rows.value.unshift(res.data);
    }
    dialogOpen.value = false;
    $q.notify({
      type: 'positive',
      message: `Proveedor ${editingId.value ? 'actualizado' : 'creado'}`,
    });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al guardar proveedor',
    });
  } finally {
    saving.value = false;
  }
}

function confirmDelete(p: Proveedor) {
  deletingItem.value = p;
  deleteDialog.value = true;
}

async function remove() {
  if (!deletingItem.value || !tenantId) return;
  deleting.value = true;
  try {
    await proveedorService.remove(deletingItem.value.id, tenantId);
    rows.value = rows.value.filter((r) => r.id !== deletingItem.value!.id);
    deleteDialog.value = false;
    $q.notify({ type: 'positive', message: 'Proveedor eliminado' });
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al eliminar proveedor',
    });
  } finally {
    deleting.value = false;
    deletingItem.value = null;
  }
}

onMounted(() => {
  if (!tenantId) {
    void showForCurrentRoute();
    return;
  }
  void load().then(() => showForCurrentRoute());
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => window.removeEventListener('keydown', handleKeydown));

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    openCreate();
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's' && dialogOpen.value) {
    e.preventDefault();
    void save();
  }
}
</script>

<template>
  <q-page class="core-page proveedores-page">
    <!-- Header con firma cobre -->
    <div class="page-header" data-tour="proveedores">
      <div class="page-header__main">
        <h1 class="page-title">Proveedores</h1>
        <p class="page-subtitle">Gestión de proveedores</p>
      </div>
      <div class="page-header__signature" aria-hidden="true">
        <span class="signature-dot" />
        <span class="signature-line" />
      </div>
    </div>

    <!-- Stats row -->
    <div v-if="!loading && sortedFiltrados.length" class="stats-row">
      <span class="stat">{{ sortedFiltrados.length }} {{ sortedFiltrados.length === 1 ? 'proveedor' : 'proveedores' }}</span>
      <template v-if="totalPages > 1">
        <span class="stat-sep" aria-hidden="true">·</span>
        <span class="stat">página {{ page }} de {{ totalPages }}</span>
      </template>
    </div>
    <div class="rule" />

    <!-- Toolbar -->
    <div class="proveedores-toolbar">
      <q-input dark dense filled v-model="search" label="Buscar" class="toolbar-search" clearable>
        <template v-slot:prepend><q-icon name="search" /></template>
      </q-input>
      <q-space class="toolbar-spacer" />
      <q-btn
        v-if="rows.length"
        color="primary" label="Nuevo" no-caps
        @click="openCreate" class="toolbar-new"
      />
    </div>

    <div v-if="!loading && !sortedFiltrados.length" class="q-mt-lg">
      <EmptyState
        icon="sym_r_people"
        title="Sin proveedores"
        message="Agrega tu primer proveedor para asociarlo a productos y facturas."
      >
        <q-btn
          color="primary"
          label="Nuevo Proveedor"
          no-caps
          @click="openCreate"
          class="q-mt-sm"
        />
      </EmptyState>
    </div>

    <div v-if="loading" class="card-grid">
      <div v-for="n in 6" :key="n">
        <q-skeleton type="rect" dark animation="pulse" height="100px" />
      </div>
    </div>

    <div v-if="!loading && paginated.length" class="card-grid">
      <q-card v-for="p in paginated" :key="p.id" dark class="hover-lift q-pa-md">
        <div class="text-weight-bold q-mb-xs">{{ p.name }}</div>
        <div v-if="p.contactName" class="text-caption text-accent q-mb-sm">
          {{ p.contactName }}
        </div>
        <div v-if="p.contactPhone" class="text-caption">
          <q-icon name="phone" size="0.8rem" class="text-accent q-mr-xs" />
          {{ p.contactPhone }}
        </div>
        <div v-if="p.contactEmail" class="text-caption">
          <q-icon name="mail" size="0.8rem" class="text-accent q-mr-xs" />
          {{ p.contactEmail }}
        </div>
        <q-separator dark class="q-mt-sm q-mb-xs" />
        <div class="row justify-end q-gutter-x-xs">
          <q-btn
            flat
            dense
            round
            icon="sym_r_edit"
            color="primary"
            size="sm"
            @click="openEdit(p)"
            aria-label="Editar"
          />
          <q-btn
            flat
            dense
            round
            icon="sym_r_delete"
            color="negative"
            size="sm"
            @click="confirmDelete(p)"
            aria-label="Eliminar"
          />
        </div>
      </q-card>
    </div>

    <div class="q-mt-md flex justify-center" v-if="!loading && totalPages > 1">
      <q-pagination
        v-model="page"
        :max="totalPages"
        :max-pages="5"
        boundary-numbers
        direction-links
        color="primary"
        text-color="accent"
        active-color="primary"
      />
    </div>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="width: 90vw; max-width: 480px">
        <q-card-section>
          <div class="text-h6 text-primary">{{ editingId ? 'Editar' : 'Nuevo' }} Proveedor</div>
        </q-card-section>
        <q-separator dark />
        <q-card-section>
          <q-form ref="formRef" @submit.prevent="save" class="q-gutter-y-md">
            <q-input
              dark
              filled
              v-model="form.name"
              label="Nombre"
              :rules="[(v) => !!v || 'Requerido']"
            />
            <q-input dark filled v-model="form.contactName" label="Nombre de contacto" />
            <q-input dark filled v-model="form.contactPhone" label="Telefono" type="tel" />
            <q-input dark filled v-model="form.contactEmail" label="Email" type="email" />
            <div class="row justify-end q-gutter-x-sm">
              <q-btn flat label="Cancelar" color="accent" v-close-popup />
              <q-btn type="submit" label="Guardar" color="primary" :loading="saving" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="deleteDialog" dark>
      <q-card dark class="bg-surface-pine">
        <q-card-section class="row items-center q-gutter-x-md">
          <q-icon name="warning" color="negative" size="md" />
          <span
            >Eliminar proveedor <strong>{{ deletingItem?.name }}</strong
            >?</span
          >
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn label="Eliminar" color="negative" :loading="deleting" @click="remove" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<style scoped>
.proveedores-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 64px);
}

/* Header con firma cobre (sin glow — brief Mayor) */
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

.stats-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 0;
}

.stat {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--pq-text-subtle);
  text-transform: uppercase;
}

.stat-sep {
  color: var(--pq-text-muted);
}

.rule {
  height: 1px;
  background: var(--pq-border);
  margin: 24px 0;
  opacity: 0.4;
}

.proveedores-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

/* Buscador — mismo patrón que ProductosPage */
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
  /* desktop: búsqueda crece (max 560) + q-space empuja el botón a la derecha */
  .toolbar-search {
    flex: 1 1 320px;
    min-width: 280px;
    max-width: 560px;
  }
}

/* Nuevo button — filled cobre (mismo patrón que Productos); 40px = alto input dense filled */
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

/* tablet táctil ≥640px: escal táctil 44px */
@media (pointer: coarse) {
  .toolbar-new {
    min-height: 44px;
  }
}

@media (max-width: 639px) {
  .proveedores-toolbar {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .toolbar-search,
  .toolbar-new {
    width: 100%;
    min-width: 0;
    max-width: none;
  }

  .toolbar-new {
    min-height: 44px;
  }

  /* touch target 44px */
  .toolbar-new {
    min-height: 44px;
  }

}
</style>
