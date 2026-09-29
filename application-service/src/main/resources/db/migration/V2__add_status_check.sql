-- Example of an incremental migration: add a DB-level guard for valid statuses.
alter table finance_applications
    add constraint ck_fin_app_status
        check (status in ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED'));
