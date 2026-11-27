-- Empty seats of a roster with the per-employee reasons (FR8), in solver order (position).

create table unassigned_seat (
    roster_id varchar(64) not null references roster (id) on delete cascade,
    position  integer     not null,
    seat_id   varchar(64) not null,
    start_at  timestamptz not null,
    end_at    timestamptz not null,
    skill     varchar(64),
    -- Reasons in order, newline-separated (each reason is one line).
    reasons   text        not null,
    primary key (roster_id, position),
    check (end_at > start_at)
);
