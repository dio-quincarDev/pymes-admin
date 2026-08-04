ALTER TABLE core.invoices
  ADD COLUMN colaborador_id UUID;

ALTER TABLE core.invoices
  ADD CONSTRAINT fk_invoices_colaborador
  FOREIGN KEY (colaborador_id) REFERENCES core.collaboradores(id)
  ON DELETE SET NULL;

CREATE INDEX idx_invoices_colaborador_id ON core.invoices(colaborador_id);
