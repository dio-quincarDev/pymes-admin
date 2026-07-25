<script setup lang="ts">
import { ref, shallowRef, computed, onMounted, onUnmounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { proveedorService } from '../services/proveedor.service';
import type { Proveedor, ProveedorRequest } from '../types';
import EmptyState from 'src/components/ui/EmptyState.vue';

useMeta({ title: 'Proveedores — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId || '';

const rows = ref<Proveedor[]>([]);
const loading = shallowRef(false);
const search = shallowRef('');

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

async function load() {
  loading.value = true;
  try {
    const res = await proveedorService.getAll(tenantId);
    rows.value = res.data;
  } catch (err) {
    $q.notify({
      type: 'negative',
      message: err instanceof Error ? err.message : 'Error al cargar proveedores',
    });
  } finally {
    loading.value = false;
  }
}

const dialogOpen = shallowRef(false);
const editingId = shallowRef<string | null>(null);
const saving = shallowRef(false);
const formRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const form = ref<ProveedorRequest>({
  tenantId,
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
  form.value = { tenantId, name: '', contactName: '', contactPhone: '', contactEmail: '' };
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
  if (!deletingItem.value) return;
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
  void load();
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
  <q-page class="core-page">
    <div class="q-mb-md">
      <h1 class="text-h4 text-primary font-bold q-ma-none">Proveedores</h1>
      <p class="text-subtitle1 text-accent q-mt-xs">Gestion de proveedores</p>
    </div>

    <div class="toolbar">
      <q-input dark dense filled v-model="search" placeholder="Buscar..." class="toolbar__search">
        <template v-slot:prepend><q-icon name="search" /></template>
      </q-input>
      <q-space />
      <q-btn color="primary" icon="add" label="Nuevo" @click="openCreate" />
    </div>

    <div v-if="!loading && !filtrados.length" class="q-mt-lg">
      <EmptyState
        icon="people"
        title="Sin proveedores"
        message="Agrega tu primer proveedor para asociarlo a productos y facturas."
      >
        <q-btn
          color="primary"
          icon="add"
          label="Nuevo Proveedor"
          @click="openCreate"
          class="q-mt-sm"
        />
      </EmptyState>
    </div>

    <div v-if="loading" class="row q-col-gutter-x-sm q-col-gutter-y-sm">
      <div v-for="n in 6" :key="n" class="col-12 col-sm-6 col-md-4">
        <q-skeleton type="rect" dark animation="pulse" height="100px" />
      </div>
    </div>

    <div v-if="!loading && filtrados.length" class="row q-col-gutter-x-sm q-col-gutter-y-sm">
      <div v-for="p in filtrados" :key="p.id" class="col-12 col-sm-6 col-md-4">
        <q-card dark class="glass hover-lift q-pa-md">
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
              icon="edit"
              color="primary"
              size="sm"
              @click="openEdit(p)"
              aria-label="Editar"
            />
            <q-btn
              flat
              dense
              round
              icon="delete"
              color="negative"
              size="sm"
              @click="confirmDelete(p)"
              aria-label="Eliminar"
            />
          </div>
        </q-card>
      </div>
    </div>

    <q-dialog v-model="dialogOpen" dark>
      <q-card dark class="bg-surface-pine" style="min-width: 400px">
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
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar__search {
  max-width: 250px;
}
</style>
