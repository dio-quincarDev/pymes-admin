<template>
  <div class="teams-page">
    <!-- Header editorial con firma cobre -->
    <div class="page-header" data-tour="teams">
      <div class="page-header__main">
        <h1 class="page-title">Equipo</h1>
        <p class="page-subtitle">Gestiona los miembros de tu empresa</p>
      </div>
      <div class="page-header__signature" aria-hidden="true">
        <span class="signature-dot" />
        <span class="signature-line" />
      </div>
    </div>

    <div class="rule" />

    <!-- Toolbar -->
    <div class="teams-toolbar">
      <q-space />
      <q-btn
        v-if="isOwner && members.length"
        color="primary" label="Invitar" no-caps
        icon="sym_r_person_add"
        @click="showInviteDialog = true"
        class="toolbar-new"
      />
    </div>

    <BaseCard class="teams-card">
      <q-table
        :rows="members"
        :columns="columns"
        row-key="user.id"
        :loading="loading"
        flat
        hide-bottom
        :pagination="{ rowsPerPage: 0 }"
      >
        <template v-slot:body-cell-role="props">
          <q-td :props="props">
            <q-chip
              :color="roleColor(props.row.role)"
              text-color="white"
              dense
              size="sm"
            >
              {{ props.row.role }}
            </q-chip>
          </q-td>
        </template>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <div class="row no-wrap q-gutter-xs">
              <q-btn
                v-if="isOwner && props.row.user.id !== currentUserId"
                flat
                round
                dense
                icon="sym_r_edit"
                size="sm"
                @click="openRoleDialog(props.row)"
              >
                <q-tooltip>Cambiar rol</q-tooltip>
              </q-btn>
              <q-btn
                v-if="isOwner && props.row.user.id !== currentUserId"
                flat
                round
                dense
                icon="sym_r_person_remove"
                size="sm"
                color="negative"
                @click="confirmRemove(props.row)"
              >
                <q-tooltip>Remover del equipo</q-tooltip>
              </q-btn>
            </div>
          </q-td>
        </template>
      </q-table>

      <div v-if="!loading && !members.length" class="q-mt-lg">
        <EmptyState icon="sym_r_groups" title="Sin miembros" message="Invita a tu equipo para empezar.">
          <q-btn
            v-if="isOwner"
            color="primary" label="Invitar Miembro" no-caps
            @click="showInviteDialog = true"
            class="q-mt-sm"
          />
        </EmptyState>
      </div>
    </BaseCard>

    <!-- Invite Dialog -->
    <q-dialog v-model="showInviteDialog" persistent>
      <q-card class="dialog-surface" style="width: 90vw; max-width: 420px">
        <q-card-section><div class="dialog-title">Invitar Miembro</div></q-card-section>
        <q-separator dark />
        <q-card-section class="q-gutter-md">
          <q-input
            v-model="inviteForm.email"
            label="Email"
            dark
            filled
            color="primary"
            label-color="accent"
            dense
            :rules="[val => !!val || 'Requerido', val => /.+@.+/.test(val) || 'Email inválido']"
          />
          <q-select
            v-model="inviteForm.role"
            :options="roleOptions"
            label="Rol"
            dark
            filled
            color="primary"
            label-color="accent"
            dense
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn
            color="primary"
            label="Enviar Invitación"
            :loading="sendingInvite"
            @click="sendInvite"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Role Change Dialog -->
    <q-dialog v-model="showRoleDialog" persistent>
      <q-card class="dialog-surface" style="width: 90vw; max-width: 360px">
        <q-card-section><div class="dialog-title">Cambiar Rol</div></q-card-section>
        <q-separator dark />
        <q-card-section>
          <div class="text-body2 q-mb-sm" style="color: var(--pq-text-muted);">
            {{ selectedMember?.user.name }}
          </div>
          <q-select
            v-model="newRole"
            :options="roleOptions"
            label="Nuevo rol"
            dark
            filled
            color="primary"
            label-color="accent"
            dense
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="accent" v-close-popup />
          <q-btn
            color="primary"
            label="Guardar"
            :loading="changingRole"
            @click="changeRole"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useMeta, useQuasar } from 'quasar';
import { useAuthStore } from '../store';
import { memberService } from '../services/member.service';
import { invitationService } from '../services/invitation.service';
import BaseCard from 'src/components/base/BaseCard.vue';
import EmptyState from 'src/components/ui/EmptyState.vue';
import type { ApiResponse, MemberResponse } from '../types';

useMeta({ title: 'Equipo — PYMEQ' });

const $q = useQuasar();
const authStore = useAuthStore();

const members = ref<MemberResponse[]>([]);
const loading = ref(true);
const showInviteDialog = ref(false);
const showRoleDialog = ref(false);
const sendingInvite = ref(false);
const changingRole = ref(false);
const selectedMember = ref<MemberResponse | null>(null);
const newRole = ref('');

const inviteForm = ref({ email: '', role: 'CONTABLE' });

const roleOptions = ['OWNER', 'ADMIN', 'CONTABLE', 'VIEWER'];
const currentUserId = computed(() => authStore.user?.id);
const userRole = computed(() => authStore.user?.role || '');
const isOwner = computed(() => userRole.value === 'OWNER');

const columns = [
  { name: 'name', label: 'Nombre', field: (row: MemberResponse) => row.user.name, align: 'left' as const },
  { name: 'email', label: 'Email', field: (row: MemberResponse) => row.user.email, align: 'left' as const },
  { name: 'role', label: 'Rol', field: 'role', align: 'center' as const },
  { name: 'actions', label: '', field: 'actions', align: 'right' as const },
];

function roleColor(role: string) {
  const map: Record<string, string> = { OWNER: 'amber', ADMIN: 'blue', CONTABLE: 'teal', VIEWER: 'grey' };
  return map[role] || 'grey';
}

async function loadMembers() {
  const tenantId = authStore.user?.tenantId;
  if (!tenantId) return;
  loading.value = true;
  try {
    const response = await memberService.getMembers(tenantId);
    const apiResponse = response.data as ApiResponse<{ content: MemberResponse[] }>;
    members.value = apiResponse.data?.content || [];
  } catch {
    $q.notify({ type: 'negative', message: 'No se pudieron cargar los miembros.' });
  } finally {
    loading.value = false;
  }
}

async function sendInvite() {
  const tenantId = authStore.user?.tenantId;
  if (!tenantId || !inviteForm.value.email) return;
  sendingInvite.value = true;
  try {
    await invitationService.createInvitation({
      tenantId,
      email: inviteForm.value.email,
      role: inviteForm.value.role
    });
    $q.notify({ type: 'positive', message: 'Invitación enviada.' });
    showInviteDialog.value = false;
    inviteForm.value = { email: '', role: 'CONTABLE' };
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({ type: 'negative', message: msg || 'Error al enviar invitación.' });
  } finally {
    sendingInvite.value = false;
  }
}

function openRoleDialog(member: MemberResponse) {
  selectedMember.value = member;
  newRole.value = member.role;
  showRoleDialog.value = true;
}

async function changeRole() {
  const tenantId = authStore.user?.tenantId;
  if (!tenantId || !selectedMember.value) return;
  changingRole.value = true;
  try {
    await memberService.updateRole(tenantId, selectedMember.value.user.id, newRole.value);
    $q.notify({ type: 'positive', message: 'Rol actualizado.' });
    showRoleDialog.value = false;
    await loadMembers();
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({ type: 'negative', message: msg || 'Error al cambiar rol.' });
  } finally {
    changingRole.value = false;
  }
}

function confirmRemove(member: MemberResponse) {
  $q.dialog({
    title: 'Remover miembro',
    message: `¿Remover a ${member.user.name} del equipo?`,
    cancel: 'Cancelar',
    ok: { label: 'Remover', color: 'negative' },
    persistent: true,
  }).onOk(() => {
    void doRemove(member);
  });
}

async function doRemove(member: MemberResponse) {
  const tenantId = authStore.user?.tenantId;
  if (!tenantId) return;
  try {
    await memberService.removeMember(tenantId, member.user.id);
    $q.notify({ type: 'positive', message: 'Miembro removido.' });
    await loadMembers();
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { mensaje?: string } } })?.response?.data?.mensaje;
    $q.notify({ type: 'negative', message: msg || 'Error al remover miembro.' });
  }
}

onMounted(loadMembers);
</script>

<style lang="scss" scoped>
.teams-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: clamp(24px, 4vw, 64px);
}

.page-header {
  margin-bottom: 8px;
}

.page-header__main {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.page-header__main h1 { margin: 0; }

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

.page-title {
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 700;
  color: var(--pq-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  color: var(--pq-text-muted);
  margin: 4px 0 0;
  font-size: 0.9rem;
}

.page-header__signature {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.signature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--pq-accent);
}

.signature-line {
  width: 40px;
  height: 1px;
  background: var(--pq-accent);
  opacity: 0.6;
}

.rule {
  height: 1px;
  background: var(--pq-border);
  margin: 16px 0 24px;
}

.teams-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 639px) {
  .teams-toolbar {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .teams-toolbar .toolbar-new {
    width: 100%;
  }
}

/* toolbar-new (mismo patrón que Productos) */
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

.toolbar-new:active { transform: scale(0.985) translateY(0); }

.toolbar-new:focus-visible {
  outline: 2px solid var(--pq-accent);
  outline-offset: 2px;
}

@media (pointer: coarse) {
  .toolbar-new { min-height: 44px; }
}

.teams-card {
  padding: 0;
  overflow: hidden;
}

.teams-card :deep(.q-table__top),
.teams-card :deep(thead tr th) {
  color: var(--pq-text-subtle);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.teams-card :deep(tbody tr) {
  border-bottom: 1px solid var(--pq-border);
}

.teams-card :deep(tbody tr:hover) {
  background: rgba(27, 44, 38, 0.3);
}

.dialog-surface {
  background: var(--pq-surface);
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-md);
}

.dialog-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--pq-text);
}
</style>