-- One stored roster: a solve or a sick-call repair of a fixture (application Roster + RosterPlan).
-- Ids are "r" + roster_id_seq (PostgresRosterRepository.nextId).

create sequence roster_id_seq start with 1 increment by 1;

create table roster (
    id              varchar(64) primary key,
    fixture         varchar(64) not null,
    operation       varchar(16) not null check (operation in ('SOLVE', 'REPAIR')),
    zone            varchar(64) not null,
    hard_score      bigint      not null,
    medium_score    bigint      not null,
    soft_score      bigint      not null,
    -- Pinned seat ids in order, newline-separated; empty for a solve.
    pinned_seat_ids text        not null default '',
    created_at      timestamptz not null default now()
);
