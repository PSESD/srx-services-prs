CREATE SCHEMA IF NOT EXISTS srx_services_prs;

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;


CREATE TABLE IF NOT EXISTS srx_services_prs.authorized_entity
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.id_seq'::regclass),
  name text NOT NULL,
  main_contact_id integer,
  CONSTRAINT authorized_entity_pkey PRIMARY KEY (id)
);