create table customers
(
    id             uuid primary key,
    user_id        uuid         not null,
    email          varchar(255) not null,
    first_name     varchar(100) not null,
    last_name      varchar(100) not null,
    phone_number   varchar(20),
    date_of_birth  date,
    national_id    varchar(20),
    kyc_status     varchar(20)  not null,
    kyc_reference  varchar(64),
    kyc_checked_at timestamptz,
    created_at     timestamptz  not null,
    updated_at     timestamptz  not null,
    version        bigint       not null default 0
);

create unique index ux_customers_user_id on customers (user_id);
create index ix_customers_kyc_status on customers (kyc_status);
