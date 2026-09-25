<script setup lang="ts">
import { useMeta } from 'quasar';
// ponytail: beta MVP privacidad simple, contacto outlook temporal
useMeta({
  title: 'Privacidad BETA — PYMEQ',
  meta: {
    description: { name: 'description', content: 'Privacidad BETA v0.1 PYMEQ — cómo cuidamos tus datos. Contacto dio-quincar@outlook.com' },
    robots: { name: 'robots', content: 'index, follow' },
  },
});
</script>

<template>
  <q-page class="legal-page">
    <div class="container-narrow mx-auto q-pa-md q-pa-lg-xl">
      <q-banner class="beta-banner q-mb-lg" role="alert">
        <template #avatar><q-icon name="science" color="accent" /></template>
        Versión BETA v0.1 — MVP en pruebas. Esta política puede cambiar. Última actualización: 17 sep 2026
      </q-banner>

      <nav class="q-mb-lg"><q-btn flat size="sm" color="accent" icon="sym_r_arrow_back" to="/" label="Volver al inicio" /></nav>

      <h1 class="legal-title">Política de Privacidad — BETA</h1>
      <p class="legal-updated">Responsable: QCore System · Panamá · Contacto: <a href="mailto:dio-quincar@outlook.com">dio-quincar@outlook.com</a> · v0.1 BETA 17 sep 2026</p>
      <q-separator class="q-my-lg" style="background: var(--pq-border)" />

      <article class="legal-content">
        <h2>1. Qué datos guardamos</h2>
        <p>Tu nombre, correo, contraseña con clave, nombre de tu negocio y lo que cargás: productos, proveedores, facturas, gastos y préstamos. También guardamos datos técnicos mínimos (IP, registros de acceso) para seguridad.</p>

        <h2>2. Para qué los usamos</h2>
        <p>Solo para darte el servicio, facturar cuando haya planes pagos y ayudarte por soporte. No vendemos tus datos ni los usamos para publicidad.</p>

        <h2>3. Dónde están</h2>
        <p>En servidores seguros (PostgreSQL + Redis) con acceso limitado y conexión cifrada. Por ser beta, pueden estar en nube internacional — lo informaremos cuando cambie.</p>

        <h2>4. Cuánto tiempo</h2>
        <p>Mientras tu cuenta esté activa y unos meses después por si volvés o por obligación legal. Si pedís borrar todo, lo anonimizamos en unos 30 días.</p>

        <h2>5. Tus derechos</h2>
        <p>Podés ver, corregir, pedir copia o borrar tus datos cuando quieras escribiendo a <a href="mailto:dio-quincar@outlook.com">dio-quincar@outlook.com</a>. Respondemos en unos 30 días. Si no estás conforme, podés reclamar ante la autoridad de Panamá (ANTAI).</p>

        <h2>6. Cookies y terceros</h2>
        <p>Solo usamos cookies necesarias para que la app funcione y recuerde tu sesión. Si entrás con Google, aplica también la política de Google. No usamos rastreadores de publicidad en beta.</p>

        <h2>7. Seguridad</h2>
        <p>Guardamos claves con cifrado, sesión con token y aíslamos cada negocio (nadie ve datos de otro). Hacemos lo razonable para proteger, pero ningún sistema es 100% infalible.</p>

        <h2>8. Cambios</h2>
        <p>Si cambiamos algo importante te avisamos 15 días antes por correo o aviso en la app. Seguir usando = estar de acuerdo. Versión siempre visible abajo.</p>

        <q-separator class="q-my-xl" style="background: var(--pq-border)" />
        <p class="legal-updated">¿Dudas de privacidad? <a href="mailto:dio-quincar@outlook.com">dio-quincar@outlook.com</a> · v0.1 BETA — texto referencial MVP, valida con abogado antes de datos sensibles.</p>
      </article>
    </div>
  </q-page>
</template>

<style scoped lang="scss">
.legal-page { background: var(--pq-background); color: var(--pq-text); }
.beta-banner { background: rgba(200,150,62,0.12); border: 1px solid var(--pq-accent); border-radius: 6px; color: var(--pq-text); font-size: 12.5px; }
.legal-title { font-family: var(--pq-font-display); color: var(--pq-accent); font-size: 28px; font-weight: 800; line-height: 1.1; }
.legal-updated { color: var(--pq-text-subtle); font-size: 12px; font-family: var(--pq-font-utility); a { color: var(--pq-accent); } }
.legal-content { font-family: var(--pq-font-body); line-height: 1.7; color: var(--pq-text-muted);
  h2 { color: var(--pq-text); font-family: var(--pq-font-display); font-size: 18px; margin: 32px 0 8px; }
  p { margin: 0 0 12px; }
  a { color: var(--pq-accent); }
}
</style>
