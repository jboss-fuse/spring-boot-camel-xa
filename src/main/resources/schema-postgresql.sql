create table if not exists audit_log (
  audit_id serial primary key,
  message varchar(255) not null
);
