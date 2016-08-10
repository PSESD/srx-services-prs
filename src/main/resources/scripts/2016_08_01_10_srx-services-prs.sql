CREATE SCHEMA IF NOT EXISTS srx_services_admin;

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_admin' AND sequence_name = 'message_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_admin.message_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;


CREATE TABLE IF NOT EXISTS srx_services_admin.message
(
  id integer NOT NULL DEFAULT nextval('srx_services_admin.message_id_seq'::regclass),
  message_id uuid NOT NULL,
  message_time timestamp with time zone NOT NULL,
  component text NOT NULL,
  component_version text NOT NULL,
  resource text NOT NULL,
  method text NOT NULL,
  status text NOT NULL,
  generator_id text NOT NULL,
  request_id text NOT NULL,
  zone_id text NOT NULL,
  context_id text NOT NULL,
  student_id text NOT NULL,
  description text NOT NULL,
  uri text NOT NULL,
  source_ip text NOT NULL,
  user_agent text NOT NULL,
  headers text NOT NULL,
  body text NOT NULL,
  CONSTRAINT message_pkey PRIMARY KEY (id)
);