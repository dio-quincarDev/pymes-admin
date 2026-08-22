<template>
  <div class="teams-page">
    <div class="page-header q-mb-lg">
      <div>
        <h1 class="text-h5 text-weight-bold">Equipo</h1>
        <p class="text-body2" style="color: var(--pq-text-muted);">Gestiona los miembros de tu empresa</p>
      </div>
      <q-btn
        label="INVITAR MIEMBRO"
        icon="sym_r_person_add"
        @click="showInviteDialog = true"
      />
    </div>

    <BaseCard>
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
                v-if="canManage && props.row.user.id !== currentUserId"
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
    </BaseCard>

    <!-- Invite Dialog -->
    <q-dialog v-model="showInviteDialog" persistent>
      <q-card style="min-width: 380px; background: var(--pq-surface); border: 1px solid var(--pq-border);">
        <q-card-section>
          <div class="text-h6">Invitar Miembro</div>
        </q-card-section>
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
          <q-btn flat label="Cancelar" v-close-popup />
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
      <q-card style="min-width: 320px; background: var(--pq-surface); border: 1px solid var(--pq-border);">
        <q-card-section>
          <div class="text-h6">Cambiar Rol</div>
          <div class="text-body2" style="color: var(--pq-text-muted);">
            {{ selectedMember?.user.name }}
          </div>
        </q-card-section>
        <q-card-section>
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
          <q-btn flat label="Cancelar" v-close-popup />
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
const canManage = computed(() => userRole.value === 'OWNER' || userRole.value === 'ADMIN');

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
  width: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
