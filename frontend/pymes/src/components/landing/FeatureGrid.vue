<script setup lang="ts">
import { useScrollReveal } from 'src/composables/useScrollReveal';

const { target, isVisible } = useScrollReveal();
</script>

<template>
  <section id="features" ref="target" class="features-section" :class="{ 'reveal-visible': isVisible }">
    <div class="container">
      <div class="section-header reveal-up">
        <div class="section-label">INTELIGENCIA FINANCIERA</div>
        <h2 class="section-title">Control total de forma sencilla.</h2>
      </div>

      <div class="features-grid">
        <div class="feature-dominant reveal-up">
          <div class="feature-index">01</div>
          <div class="feature-content">
            <q-icon name="receipt_long" size="2rem" class="feature-icon" aria-hidden="true" />
            <h3 class="feature-title">Tu negocio y tu inversión, en un correo al mes.</h3>
            <p class="feature-copy">
              Reportes automáticos con ingresos, gastos y plan de retorno de inversión.
              Sin abrir la app.
            </p>
            <div class="feature-preview">
              <span class="preview-line">Ingresos $8,500  ·  Neto $4,600</span>
              <span class="preview-line">Aparta $1,666.67  ·  recuperás tu inversión</span>
            </div>
          </div>
        </div>

        <div class="feature-secondary reveal-up">
          <div class="feature-row">
            <div class="feature-index">02</div>
            <div class="feature-row-content">
              <div class="feature-row-header">
                <q-icon name="analytics" size="1.25rem" class="feature-icon-sm" aria-hidden="true" />
                <span class="feature-tag">ABC DE GASTOS</span>
              </div>
              <h4 class="feature-row-title">Analiza tus gastos</h4>
              <p class="feature-row-copy">
                Clasificación automática A/B/C de tus productos. Sabé qué insumos
                consumen el 80% de tu presupuesto.
              </p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-index">03</div>
            <div class="feature-row-content">
              <div class="feature-row-header">
                <q-icon name="cloud_off" size="1.25rem" class="feature-icon-sm" aria-hidden="true" />
                <span class="feature-tag">PWA OFFLINE</span>
              </div>
              <h4 class="feature-row-title">Tu oficina móvil</h4>
              <p class="feature-row-copy">
                Registrá operaciones desde cualquier lugar. Sin conexión incluido.
              </p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-index">04</div>
            <div class="feature-row-content">
              <div class="feature-row-header">
                <q-icon name="trending_up" size="1.25rem" class="feature-icon-sm" aria-hidden="true" />
                <span class="feature-tag">PROVEEDORES</span>
              </div>
              <h4 class="feature-row-title">Comparativa de proveedores</h4>
              <p class="feature-row-copy">
                Sabé si tu proveedor te está vendiendo más caro que el promedio del mercado.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 clamp(16px, 4vw, 24px);
  width: 100%;
}

.features-section {
  padding: clamp(48px, 8vw, 96px) 0;
}

.section-header {
  margin-bottom: 48px;
}

.section-label {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-accent);
  margin-bottom: 8px;
}

.section-title {
  font-family: var(--pq-font-display);
  font-size: clamp(2rem, 3.5vw, 3rem);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -0.025em;
  color: var(--pq-text);
  margin: 0;
  text-wrap: balance;
}

.features-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 40px;

  @media (min-width: 1024px) {
    grid-template-columns: 7fr 5fr;
    gap: 24px;
  }
}

.feature-dominant {
  border: 1px solid var(--pq-border);
  border-radius: var(--pq-radius-md);
  padding: 32px;
  background: var(--pq-surface);
  display: flex;
  gap: 24px;
  align-items: flex-start;
  transition: border-color 0.2s cubic-bezier(0.22, 1, 0.36, 1),
              transform 0.2s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    border-color: var(--pq-accent);
    transform: translateY(-2px);
  }
}

.feature-index {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  font-weight: 500;
  color: var(--pq-text-subtle);
  letter-spacing: 0.08em;
  flex-shrink: 0;
  padding-top: 4px;
}

.feature-icon {
  font-family: 'Material Symbols Rounded' !important;
  font-variation-settings: 'FILL' 0, 'wght' 300, 'opsz' 24;
  color: var(--pq-accent);
  font-size: 2rem;
  display: block;
  margin-bottom: 16px;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
              color 0.2s ease,
              font-variation-settings 0.2s ease;

  .feature-dominant:hover & {
    transform: rotate(8deg) scale(1.05);
    color: var(--pq-accent-hover);
    font-variation-settings: 'FILL' 1, 'wght' 400, 'opsz' 24;
  }
}

.feature-icon-sm {
  font-family: 'Material Symbols Rounded' !important;
  font-variation-settings: 'FILL' 0, 'wght' 300, 'opsz' 20;
  color: var(--pq-accent);
  font-size: 1.25rem;
  display: inline-block;
  vertical-align: middle;
  margin-left: 4px;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
              color 0.2s ease,
              font-variation-settings 0.2s ease;

  .feature-row:hover & {
    transform: translateX(4px);
    color: var(--pq-accent-hover);
    font-variation-settings: 'FILL' 1, 'wght' 400, 'opsz' 20;
  }
}

.feature-title {
  font-family: var(--pq-font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--pq-text);
  margin: 0 0 12px;
}

.feature-copy {
  font-family: var(--pq-font-body);
  font-size: 14px;
  line-height: 1.55;
  color: var(--pq-text-muted);
  margin: 0;
  text-wrap: pretty;
}

.feature-preview {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--pq-border);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-line {
  font-family: var(--pq-font-utility);
  font-size: 12px;
  font-weight: 500;
  color: var(--pq-accent);
  letter-spacing: 0.04em;
  font-variant-numeric: tabular-nums;
}

.feature-secondary {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.feature-row {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  padding: 24px 0;
  border-bottom: 1px solid var(--pq-border);
  transition: border-color 0.2s cubic-bezier(0.22, 1, 0.36, 1),
              padding-left 0.2s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    border-bottom-color: var(--pq-accent);
    padding-left: 8px;
  }

  &:first-child {
    padding-top: 0;
  }

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.feature-row-content {
  flex: 1;
}

.feature-row-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.feature-tag {
  font-family: var(--pq-font-utility);
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--pq-accent);
  display: block;
  margin-bottom: 8px;
}

.feature-row-title {
  font-family: var(--pq-font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--pq-text);
  margin: 0 0 8px;
}

.feature-row-copy {
  font-family: var(--pq-font-body);
  font-size: 14px;
  line-height: 1.5;
  color: var(--pq-text-muted);
  margin: 0;
  text-wrap: pretty;
}

.reveal-up {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.32s cubic-bezier(0.22, 1, 0.36, 1),
              transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.reveal-visible .reveal-up {
  opacity: 1;
  transform: translateY(0);
}
</style>
