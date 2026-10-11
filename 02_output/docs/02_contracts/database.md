# Contract: registration storage (backend ↔ PostgreSQL)

> Written in: phase 2 · Source: specification section 5, BR-07, AR-06 · Agent: writes

Implemented only by Flyway migrations in `backend/src/main/resources/db/migration` (AR-06, ES-08); Hibernate validates, never generates.

```sql
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
    registered_at     timestamptz  NOT NULL,
    CHECK ((type = 'EXTERNAL' AND organization IS NOT NULL
            AND study_institution IS NULL AND study_programme IS NULL AND student_id IS NULL)
        OR (type = 'STUDENT' AND organization IS NULL
            AND study_institution IS NOT NULL AND study_programme IS NOT NULL AND student_id IS NOT NULL))
);

CREATE TABLE registration_option (
    registration_id uuid         NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    option_id       varchar(64)  NOT NULL,
    option_name     varchar(200) NOT NULL,
    category        varchar(16)  NOT NULL CHECK (category IN ('WORKSHOP', 'EVENT', 'MEAL', 'OTHER')),
    PRIMARY KEY (registration_id, option_id)
);

CREATE TABLE registration_consent (
    registration_id uuid          NOT NULL REFERENCES registration (id) ON DELETE CASCADE,
    consent_id      varchar(64)   NOT NULL,
    consent_text    varchar(1000) NOT NULL,
    given_at        timestamptz   NOT NULL,
    PRIMARY KEY (registration_id, consent_id)
);

CREATE INDEX registration_registered_at_idx ON registration (registered_at);
```

- Encoding: the database uses UTF-8 (PostgreSQL image default), so č, š, ž are stored unchanged (NFR-01).
- Option name, category and consent text are copies taken at registration time.
- Deleting a registration (operator task, D-10) cascades to its options and consents.
