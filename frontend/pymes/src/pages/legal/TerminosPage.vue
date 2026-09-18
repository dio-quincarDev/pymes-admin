<script setup lang="ts">
import { useMeta } from 'quasar';
// ponytail: texto beta MVP simple, contacto outlook temporal, migrar a empresarial cuando exista
// ponytail: beta disclaimer visible — MVP v0.1 no es asesoría fiscal/legal
useMeta({
  title: 'Términos BETA — PYMEQ',
  meta: {
    description: { name: 'description', content: 'Términos BETA v0.1 MVP PYMEQ por QCore System Panamá. Versión beta en pruebas.' },
    robots: { name: 'robots', content: 'index, follow' },
  },
});
</script>

<template>
  <q-page class="legal-page">
    <div class="container-narrow mx-auto q-pa-md q-pa-lg-xl">
      <q-banner class="beta-banner q-mb-lg" role="alert">
        <template #avatar><q-icon name="science" color="accent" /></template>
        Versión BETA v0.1 — MVP en pruebas. Funciones, precios y textos pueden cambiar sin aviso. Última actualización: 17 sep 2026
      </q-banner>

      <nav class="q-mb-lg"><q-btn flat size="sm" color="accent" icon="sym_r_arrow_back" to="/" label="Volver al inicio" /></nav>

      <h1 class="legal-title">Términos y Condiciones — BETA</h1>
      <p class="legal-updated">PYMEQ por QCore System · Panamá · Contacto: <a href="mailto:dio-quincar@outlook.com">dio-quincar@outlook.com</a> · v0.1 BETA 17 sep 2026</p>
      <q-separator class="q-my-lg" style="background: var(--pq-border)" />

      <article class="legal-content">
        <h2>1. Quién es PYMEQ</h2>
        <p>Somos QCore System, la empresa detrás de PYMEQ en Panamá. Al crear tu cuenta y marcar “Acepto Términos” aceptás estas reglas. Si no estás de acuerdo, no uses la app.</p>

        <h2>2. Para qué sirve (y para qué no) — BETA</h2>
        <p>PYMEQ está en <strong>beta</strong>: te ayuda a llevar tu negocio — productos, proveedores, facturas, gastos y préstamos. Te muestra totales sin y con ITBMS (0%, 7%, 10%) para que entiendas tu plata, pero <strong>no es contador ni factura oficial de la DGI</strong>. Los cálculos y el puntaje 0–100 son ayuda, no verdad definitiva. Vos seguís siendo responsable de tus impuestos. Usalo como apoyo y consultá a tu contador para lo oficial.</p>

        <h2>3. Tu cuenta y tu equipo</h2>
        <p>Tu negocio es solo tuyo — nadie más ve tus datos. Vos sos el dueño principal y decidís a quién invitás y qué puede hacer (ver, cargar datos o administrar). Cada persona cuida su contraseña. Si alguien ya no debe entrar, quítale el acceso.</p>

        <h2>4. Cuánto cuesta</h2>
        <p>Ahora es <strong>gratis para empezar</strong> (un negocio, un usuario) por ser beta. Cuando haya planes pagos te avisamos el precio en dólares 30 días antes. Si no pagás cuando toque, te avisamos y luego pausamos el acceso. No hay letra chica escondida.</p>

        <h2>5. De quién es cada cosa</h2>
        <p>El código de la app es abierto, pero la marca PYMEQ y el servicio son nuestros. Lo que vos cargás — tus facturas, productos y clientes — es tuyo. No lo vendemos ni lo compartimos.</p>

        <h2>6. Qué hacemos con tus datos</h2>
        <p>Solo guardamos lo necesario para que funcione: tu nombre, correo y lo de tu negocio. Están guardados con clave y solo personal autorizado puede verlos para ayudarte. Podés pedir verlos, corregirlos o borrarlos escribiendo a <a href="mailto:dio-quincar@outlook.com">dio-quincar@outlook.com</a>.</p>

        <h2>7. Usar la app con respeto</h2>
        <p>No vale hackear, saturar el sistema, compartir tu clave o intentar ver datos de otro negocio. Si alguien hace eso, podemos pausarle la cuenta para proteger a todos.</p>

        <h2>8. Cómo funciona el servicio — BETA</h2>
        <p>Te damos la app tal cual, lo mejor que podemos, sin prometer 100% de disponibilidad. Como es beta, puede haber mantenimiento sin aviso. Si algo crítico falla lo comunicamos por correo. Soporte en 1–2 días hábiles.</p>

        <h2>9. Si te querés ir</h2>
        <p>Te vas cuando quieras. Te llevás tu información y si nos pedís borrarla la borramos en unos 30 días, salvo lo que por ley debemos guardar un tiempo más. Si no cumplís las reglas o no pagás, nosotros también podemos pausarte o cerrarte la cuenta avisándote antes.</p>

        <h2>10. Reglas y cambios</h2>
        <p>Estas reglas son bajo leyes de Panamá y cualquier tema se ve en Panamá. Si cambiamos algo importante te avisamos 15 días antes. Si seguís usando la app después de ese aviso, estás de acuerdo. Abajo siempre verás la versión y la fecha.</p>

        <q-separator class="q-my-xl" style="background: var(--pq-border)" />
        <p class="legal-updated">¿Dudas? Escribinos a <a href="mailto:dio-quincar@outlook.com">dio-quincar@outlook.com</a> · v0.1 BETA — texto referencial MVP, no es asesoría legal.</p>
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
