-- Registration storage (docs/02_contracts/database.sql, AR-06)

CREATE TABLE registration (
    id                UUID         PRIMARY KEY,
    type              VARCHAR(16)  NOT NULL CHECK (type IN ('EXTERNAL', 'STUDENT')),
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    organization      VARCHAR(200),
    study_institution VARCHAR(200),
    study_programme   VARCHAR(200),
    student_id        VARCHAR(200),
    registered_at     TIMESTAMPTZ  NOT NULL
);

CREATE TABLE registration_option (
    registration_id UUID         NOT NULL REFERENCES registration (id),
    option_id       VARCHAR(50)  NOT NULL,
    PRIMARY KEY (registration_id, option_id)
);

CREATE TABLE registration_consent (
    registration_id UUID        NOT NULL REFERENCES registration (id),
    consent_id      VARCHAR(50) NOT NULL,
    given_at        TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);
