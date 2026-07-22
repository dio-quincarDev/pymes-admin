<template>
  <q-page class="teams-page">
    <SkeletonLoader :is-loading="loading" layout="card">
      <div class="teams-header fade-in-up">
        <h1 class="teams-title">Teams</h1>
        <BaseButton
          v-if="canManage"
          variant="primary"
          icon-left="person_add"
          size="md"
          @click="openInvite"
        >
          Invite Member
        </BaseButton>
      </div>

      <BaseCard variant="default" class="teams-table-card fade-in-up">
        <q-table
          dark
          flat
          :rows="members"
          :columns="columns"
          row-key="user.id"
          :loading="loading"
          :rows-per-page-options="[10, 25, 50]"
          :pagination="{ rowsPerPage: 10 }"
          class="teams-table"
          hide-bottom
        >
          <template v-slot:body-cell-role="{ row }">
            <td>
              <span class="role-badge" :class="`role-${row.role.toLowerCase()}`">
                {{ row.role }}
              </span>
              <q-btn
                v-if="canManage && row.role !== 'OWNER'"
                flat
                dense
                round
                size="xs"
                icon="edit"
                class="role-edit-btn"
                @click="openRoleChange(row)"
              />
            </td>
          </template>

          <template v-slot:body-cell-joinedAt="{ row }">
            <td>
              <span class="date-cell">{{ formatDate(row.joinedAt) }}</span>
            </td>
          </template>

          <template v-slot:body-cell-actions="{ row }">
            <td class="text-right">
              <q-btn
                v-if="row.role !== 'OWNER' && userRole === 'OWNER'"
                flat
                dense
                round
                size="sm"
                icon="person_off"
                class="remove-btn"
                @click="confirmRemove(row)"
              />
            </td>
          </template>

          <template v-slot:no-data>
            <div class="empty-table">
              <q-icon name="group" size="2rem" class="text-accent" />
              <p>No members yet</p>
            </div>
          </template>
        </q-table>
      </BaseCard>
    </SkeletonLoader>

    <q-dialog v-model="inviteDialog" persistent>
      <BaseCard variant="elevated" class="invite-dialog">
        <div class="invite-dialog__header">
          <h2 class="invite-dialog__title">Invite Member</h2>
          <q-btn flat dense round icon="close" v-close-popup />
        </div>

        <q-form @submit="onInvite" ref="inviteFormRef">
          <q-input
            v-model="inviteForm.email"
            label="Email"
            type="email"
            dark
            dense
            filled
            :rules="[
              (val) => !!val || 'Email is required',
              (val) => /.+@.+\..+/.test(val) || 'Invalid email',
            ]"
            class="q-mb-md"
          />

          <q-select
            v-model="inviteForm.role"
            :options="availableRoles"
            label="Role"
            dark
            dense
            filled
            :rules="[(val) => !!val || 'Role is required']"
            class="q-mb-md"
          />

          <div class="invite-dialog__actions">
            <BaseButton variant="ghost" @click="inviteDialog = false">Cancel</BaseButton>
            <BaseButton variant="primary" type="submit" :loading="inviting">
              Send Invitation
            </BaseButton>
          </div>
        </q-form>
      </BaseCard>
    </q-dialog>

    <q-dialog v-model="roleDialog" persistent>
      <BaseCard variant="elevated" class="role-dialog">
        <div class="invite-dialog__header">
          <h2 class="invite-dialog__title">Change Role</h2>
          <q-btn flat dense round icon="close" v-close-popup />
        </div>

        <q-form @submit="onRoleChange">
          <p class="role-dialog__current">
            Current: <strong>{{ editingMember?.role }}</strong>
          </p>

          <q-select
            v-model="newRole"
            :options="availableRoles"
            label="New Role"
            dark
            dense
            filled
            :rules="[(val) => !!val || 'Select a role']"
            class="q-mb-md"
          />

          <div class="invite-dialog__actions">
            <BaseButton variant="ghost" @click="roleDialog = false">Cancel</BaseButton>
            <BaseButton variant="primary" type="submit" :loading="updatingRole">
              Update
            </BaseButton>
          </div>
        </q-form>
      </BaseCard>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useQuasar, useMeta } from 'quasar';
import { useAuthStore } from 'src/modules/auth/store';
import { memberService } from 'src/modules/auth/services/member.service';
import { invitationService } from 'src/modules/auth/services/invitation.service';
import BaseCard from 'src/components/base/BaseCard.vue';
import BaseButton from 'src/components/base/BaseButton.vue';
import SkeletonLoader from 'src/components/ui/SkeletonLoader.vue';

useMeta({ title: 'Teams — PYMEQ' });

interface MemberRow {
  user: { id: string; email: string; name: string; pictureUrl?: string };
  role: string;
  accepted: boolean;
  joinedAt: string;
}

const $q = useQuasar();
const authStore = useAuthStore();
const tenantId = authStore.user?.tenantId || '';
const userRole = authStore.user?.role || '';

const loading = ref(true);
const members = ref<MemberRow[]>([]);

const canManage = computed(() => userRole === 'OWNER');
const availableRoles = computed(() => {
  if (userRole !== 'OWNER') return [];
  return ['ADMIN', 'CONTABLE', 'VIEWER'];
});

const columns = [
  {
    name: 'name',
    label: 'Name',
    field: (row: MemberRow) => row.user.name,
    align: 'left' as const,
    sortable: true,
  },
  {
    name: 'email',
    label: 'Email',
    field: (row: MemberRow) => row.user.email,
    align: 'left' as const,
    sortable: true,
  },
  {
    name: 'role',
    label: 'Role',
    field: (row: MemberRow) => row.role,
    align: 'left' as const,
    sortable: true,
  },
  {
    name: 'joinedAt',
    label: 'Joined',
    field: (row: MemberRow) => row.joinedAt,
    align: 'left' as const,
    sortable: true,
  },
  { name: 'actions', label: '', field: '', align: 'right' as const },
];

async function loadMembers() {
  if (!tenantId) return;
  loading.value = true;
  try {
    const res = await memberService.getMembers(tenantId);
    const data = res.data?.data || res.data;
    members.value = data.content || data;
  } catch (err) {
    $q.notify({ type: 'negative', message: getErrorMessage(err, 'Error loading members') });
  } finally {
    loading.value = false;
  }
}

function getErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const axiosErr = err as { response?: { data?: { mensaje?: string; message?: string } } };
    return axiosErr.response?.data?.mensaje || axiosErr.response?.data?.message || fallback;
  }
  return err instanceof Error ? err.message : fallback;
}

onMounted(loadMembers);

const inviteDialog = ref(false);
const inviteFormRef = ref<{ validate: () => Promise<boolean> } | null>(null);
const inviteForm = ref({ email: '', role: '' });
const inviting = ref(false);

function openInvite() {
  inviteForm.value = { email: '', role: availableRoles.value[0] || '' };
  inviteDialog.value = true;
}

async function onInvite() {
  if (!(await inviteFormRef.value?.validate())) return;
  inviting.value = true;
  try {
    await invitationService.createInvitation({
      tenantId,
      email: inviteForm.value.email,
      role: inviteForm.value.role,
    });
    $q.notify({ type: 'positive', message: 'Invitation sent' });
    inviteDialog.value = false;
  } catch (err) {
    $q.notify({ type: 'negative', message: getErrorMessage(err, 'Error sending invitation') });
  } finally {
    inviting.value = false;
  }
}

const roleDialog = ref(false);
const editingMember = ref<MemberRow | null>(null);
const newRole = ref('');
const updatingRole = ref(false);

function openRoleChange(row: MemberRow) {
  editingMember.value = row;
  newRole.value = '';
  roleDialog.value = true;
}

async function onRoleChange() {
  if (!editingMember.value || !newRole.value) return;
  updatingRole.value = true;
  try {
    await memberService.updateRole(tenantId, editingMember.value.user.id, newRole.value);
    $q.notify({ type: 'positive', message: 'Role updated' });
    roleDialog.value = false;
    await loadMembers();
  } catch (err) {
    $q.notify({ type: 'negative', message: getErrorMessage(err, 'Error updating role') });
  } finally {
    updatingRole.value = false;
  }
}

function confirmRemove(row: MemberRow) {
  $q.dialog({
    title: 'Desactivar miembro',
    message: `¿Desactivar a ${row.user.name}? Podés reactivarlo después.`,
    dark: true,
    ok: { label: 'Desactivar', color: 'negative', flat: true },
    cancel: { label: 'Cancelar', flat: true },
  }).onOk(() => {
    memberService
      .removeMember(tenantId, row.user.id)
      .then(() => {
        $q.notify({ type: 'positive', message: 'Miembro desactivado' });
        return loadMembers();
      })
      .catch((err) => {
        $q.notify({ type: 'negative', message: getErrorMessage(err, 'Error al desactivar miembro') });
      });
  });
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleDateString('es-AR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}
</script>

<style lang="scss" scoped>
.teams-page {
  width: 100%;
}

.teams-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 12px;
  flex-wrap: wrap;
}

.teams-title {
  font-family: 'Geist', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--pq-text);
  margin: 0;
}

.teams-table-card {
  overflow: clip;
}

.teams-table {
  :deep(.q-table) {
    background: transparent;
    color: var(--pq-text);
  }

  :deep(.q-table thead th) {
    font-family: 'Satoshi', sans-serif;
    font-size: 11px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--pq-text-subtle);
    border-bottom: 1px solid var(--pq-border);
    padding: 12px 16px;
  }

  :deep(.q-table tbody td) {
    font-family: 'Satoshi', sans-serif;
    font-size: 14px;
    color: var(--pq-text);
    border-bottom: 1px solid rgba(53, 57, 69, 0.3);
    padding: 12px 16px;
  }

  :deep(.q-table tbody tr:hover) {
    background: rgba(200, 150, 62, 0.04);
  }

  :deep(.q-table__card) {
    background: transparent;
    box-shadow: none;
  }
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 9999px;
  font-family: 'Satoshi', sans-serif;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  gap: 4px;

  &.role-owner {
    background: rgba(212, 160, 74, 0.15);
    color: var(--pq-accent);
  }

  &.role-admin {
    background: rgba(110, 139, 184, 0.15);
    color: var(--pq-info);
  }

  &.role-viewer {
    background: rgba(107, 104, 99, 0.15);
    color: var(--pq-text-muted);
  }

  &.role-contable {
    background: rgba(72, 199, 142, 0.15);
    color: #48c78e;
  }
}

.role-edit-btn {
  color: var(--pq-text-subtle);
  margin-left: 4px;

  &:hover {
    color: var(--pq-accent);
  }
}

.date-cell {
  font-family: 'Satoshi', sans-serif;
  font-size: 13px;
  color: var(--pq-text-muted);
}

.remove-btn {
  color: var(--pq-text-subtle);

  &:hover {
    color: var(--pq-danger);
  }
}

.empty-table {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: var(--pq-text-muted);
  font-family: 'Satoshi', sans-serif;
  font-size: 14px;
}

.invite-dialog,
.role-dialog {
  width: 400px;
  max-width: 90vw;
  padding: 24px;
}

.invite-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.invite-dialog__title {
  font-family: 'Geist', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--pq-text);
  margin: 0;
}

.invite-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.role-dialog__current {
  font-family: 'Satoshi', sans-serif;
  font-size: 14px;
  color: var(--pq-text-muted);
  margin-bottom: 16px;
}
</style>
