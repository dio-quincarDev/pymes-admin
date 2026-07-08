ALTER TABLE core.expense_analysis
    ADD COLUMN IF NOT EXISTS supplier_comparison   JSONB,
    ADD COLUMN IF NOT EXISTS supplier_recommendations JSONB,
    ADD COLUMN IF NOT EXISTS price_prediction      JSONB;
