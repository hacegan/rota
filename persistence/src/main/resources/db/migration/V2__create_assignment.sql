-- Filled seats of a roster, in the order the solver returned them (position).

create table assignment (
    roster_id   varchar(64) not null references roster (id) on delete cascade,
    position    integer     not null,
    seat_id     varchar(64) not null,
    start_at    timestamptz not null,
    end_at      timestamptz not null,
    skill       varchar(64),
    employee_id varchar(64) not null,
    primary key (roster_id, position),
    check (end_at > start_at)
);
