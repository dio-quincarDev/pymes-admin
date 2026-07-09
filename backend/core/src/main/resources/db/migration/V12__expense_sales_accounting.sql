CREATE TABLE core.operating_expenses (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    category      VARCHAR(50) NOT NULL,
    description   VARCHAR(255),
    amount        DECIMAL(12,2) NOT NULL,
    expense_date  DATE NOT NULL,
    payment_method VARCHAR(50),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_operating_expenses_tenant_date ON core.operating_expenses(tenant_id, expense_date);

CREATE TABLE core.loans (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    name               VARCHAR(150) NOT NULL,
    lender             VARCHAR(150),
    amount             DECIMAL(15,2) NOT NULL,
    interest_rate      DECIMAL(6,4),
    term_months        INTEGER,
    start_date         DATE NOT NULL,
    remaining_balance  DECIMAL(15,2) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    notes              TEXT,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_loans_tenant ON core.loans(tenant_id);

CREATE TABLE core.loan_payments (
    id               UUID PRIMARY KEY,
    loan_id          UUID NOT NULL REFERENCES core.loans(id),
    amount           DECIMAL(15,2) NOT NULL,
    interest_paid    DECIMAL(15,2) NOT NULL DEFAULT 0,
    principal_paid   DECIMAL(15,2) NOT NULL DEFAULT 0,
    payment_date     DATE NOT NULL,
    payment_method   VARCHAR(50),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_loan_payments_loan ON core.loan_payments(loan_id);

CREATE TABLE core.patrimony (
    tenant_id        UUID PRIMARY KEY,
    initial_capital  DECIMAL(15,2) NOT NULL DEFAULT 0,
    start_date       DATE,
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE core.daily_sales (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    sale_date     DATE NOT NULL,
    gross_amount  DECIMAL(12,2) NOT NULL,
    description   VARCHAR(255),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_daily_sales_tenant_date ON core.daily_sales(tenant_id, sale_date);

CREATE TABLE core.tenant_financial_metrics (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    period             VARCHAR(7) NOT NULL,
    total_income       DECIMAL(15,2) DEFAULT 0,
    cost_of_goods      DECIMAL(15,2) DEFAULT 0,
    operating_expenses DECIMAL(15,2) DEFAULT 0,
    loan_payments      DECIMAL(15,2) DEFAULT 0,
    total_expenses     DECIMAL(15,2) DEFAULT 0,
    gross_margin       DECIMAL(15,2) DEFAULT 0,
    gross_margin_pct   DECIMAL(10,4) DEFAULT 0,
    operating_margin   DECIMAL(15,2) DEFAULT 0,
    operating_margin_pct DECIMAL(10,4) DEFAULT 0,
    net_margin         DECIMAL(15,2) DEFAULT 0,
    net_margin_pct     DECIMAL(10,4) DEFAULT 0,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_metrics_tenant_period UNIQUE (tenant_id, period)
);
