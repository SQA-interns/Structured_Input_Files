-- Registration storage (contract docs/02_contracts/database.md, BR-07, AR-06)
create table registration (
    id                 uuid primary key,
    type               varchar(16)   not null check (type in ('EXTERNAL', 'STUDENT')),
    submitted_at       timestamptz   not null,
    first_name         varchar(200)  not null,
    last_name          varchar(200)  not null,
    email              varchar(254)  not null,
    email_normalized   varchar(254)  not null,
    organization       varchar(200),
    study_institution  varchar(200),
    study_programme    varchar(200),
    student_id         varchar(200),
    consent_id         varchar(64)   not null,
    consent_text       varchar(1000) not null,
    consent_given_at   timestamptz   not null,
    constraint uk_registration_email_normalized unique (email_normalized)
);

create table registration_option (
    registration_id  uuid         not null references registration (id) on delete cascade,
    position         integer      not null,
    option_id        varchar(64)  not null,
    option_name      varchar(200) not null,
    category         varchar(16)  not null check (category in ('WORKSHOP', 'EVENT', 'MEAL', 'OTHER')),
    primary key (registration_id, position),
    constraint uk_registration_option unique (registration_id, option_id)
);

create index ix_registration_submitted_at on registration (submitted_at);
