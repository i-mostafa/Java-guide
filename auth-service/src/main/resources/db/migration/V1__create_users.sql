-- Flyway migration: files are applied in version order exactly once and recorded in
-- flyway_schema_history. NEVER edit an applied migration - add V2__..., V3__... instead.
create table users
(
    id            uuid primary key,
    email         varchar(255) not null,
    password_hash varchar(255) not null,
    first_name    varchar(100) not null,
    last_name     varchar(100) not null,
    role          varchar(20)  not null,
    enabled       boolean      not null default true,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    version       bigint       not null default 0
);

create unique index ux_users_email on users (email);
