-- V1: registration storage (docs/02_contracts/database.sql, AR-06, ES-08).

CREATE TABLE registration (
    id                uuid         PRIMARY KEY,
    type              varchar(16)  NOT NULL CHECK (type IN ('EXTERNAL', 'STUDENT')),
    first_name        varchar(100) NOT NULL,
    last_name         varchar(100) NOT NULL,
    email             varchar(254) NOT NULL,
    organization      varchar(200),
    study_institution varchar(200),
    study_programme   varchar(200),
    student_id        varchar(50),
    registered_at     timestamptz  NOT NULL
);

CREATE TABLE registration_option (
    registration_id uuid         NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    option_id       varchar(64)  NOT NULL,
    option_name     varchar(200) NOT NULL,
    category        varchar(16)  NOT NULL,
    position        integer      NOT NULL,
    PRIMARY KEY (registration_id, option_id)
);

CREATE TABLE registration_consent (
    registration_id uuid        NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id      varchar(64) NOT NULL,
    consent_text    text        NOT NULL,
    given_at        timestamptz NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);

CREATE INDEX registration_registered_at_idx ON registration (registered_at);
