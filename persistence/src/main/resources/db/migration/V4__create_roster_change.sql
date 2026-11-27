-- Repair change list against the published roster (empty for a solve), in order (position).
-- before/after employee null = empty seat.

create table roster_change (
    roster_id          varchar(64) not null references roster (id) on delete cascade,
    position           integer     not null,
    seat_id            varchar(64) not null,
    before_employee_id varchar(64),
    after_employee_id  varchar(64),
    kind               varchar(16) not null check (kind in ('REASSIGNED', 'LEFT_EMPTY', 'FILLED')),
    primary key (roster_id, position)
);
