create table finance_applications
(
    id                  uuid primary key,
    customer_user_id    uuid           not null,
    customer_id         uuid           not null,
    property_reference  varchar(50)    not null,
    city                varchar(100)   not null,
    property_type       varchar(20)    not null,
    declared_value      numeric(14, 2) not null,
    valuation_amount    numeric(14, 2) not null,
    valuation_reference varchar(64),
    finance_amount      numeric(14, 2) not null,
    tenure_months       integer        not null,
    profit_rate         numeric(6, 4)  not null,
    monthly_installment numeric(14, 2) not null,
    status              varchar(20)    not null,
    status_reason       varchar(500),
    created_at          timestamptz    not null,
    updated_at          timestamptz    not null,
    version             bigint         not null default 0
);

create index ix_fin_app_customer on finance_applications (customer_user_id, created_at desc);
create index ix_fin_app_status on finance_applications (status);
