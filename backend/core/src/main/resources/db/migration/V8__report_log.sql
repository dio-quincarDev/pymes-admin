-- V8: log de reportes mensuales a OWNERs (idempotencia por PK + ON CONFLICT DO NOTHING).
-- Un row por (tenant, periodo, formato): PDF este sprint, XLSX el siguiente sin migracion nueva.
CREATE TABLE IF NOT EXISTS core.report_log (
    tenant_id UUID        NOT NULL,
    period    VARCHAR(7)  NOT NULL,   -- '2026-09'
    format    VARCHAR(10) NOT NULL,   -- 'PDF' | 'XLSX'
    status    VARCHAR(10) NOT NULL DEFAULT 'SENDING', -- SENDING|SENT|FAILED|SKIPPED
    sent_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    error_msg TEXT,
    PRIMARY KEY (tenant_id, period, format),
    CONSTRAINT report_log_status_check CHECK (status IN ('SENDING', 'SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_report_log_failed
  ON core.report_log (tenant_id, period) WHERE status = 'FAILED';
