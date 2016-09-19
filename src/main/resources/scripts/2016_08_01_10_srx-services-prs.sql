CREATE SCHEMA IF NOT EXISTS srx_services_prs;

/* ----------- CONTACT ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'contact_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.contact_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.contact
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.contact_id_seq'::regclass),
  name text,
  title text,
  email text,
  phone text,
  mailing_address text,
  web_address text,
  CONSTRAINT contact_pkey PRIMARY KEY (id)
);

/* ----------- AUTHORIZED ENTITY ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'authorized_entity_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.authorized_entity_id_seq
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
  id integer NOT NULL DEFAULT nextval('srx_services_prs.authorized_entity_id_seq'::regclass),
  name text NOT NULL,
  main_contact_id integer,
  CONSTRAINT authorized_entity_pkey PRIMARY KEY (id)
);

/* ----------- AUTHORIZED ENTITY PERSONNEL ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'personnel_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.personnel_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.personnel
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.personnel_id_seq'::regclass),
  authorized_entity_id integer NOT NULL,
  first_name text,
  last_name text,
  CONSTRAINT personnel_pkey PRIMARY KEY (id)
);

/* ----------- EXTERNAL SERVICE ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'external_service_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.external_service_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.external_service
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.external_service_id_seq'::regclass),
  authorized_entity_id integer NOT NULL,
  name text,
  description text,
  CONSTRAINT external_service_pkey PRIMARY KEY (id)
);

/* ----------- DISTRICT ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'district_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.district_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.district
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.district_id_seq'::regclass),
  name text NOT NULL,
  nces_lea_code text,
  zone_id text,
  main_contact_id integer,
  CONSTRAINT district_pkey PRIMARY KEY (id)
);


/* ----------- DISTRICT SERVICE ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'district_service_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.district_service_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.district_service
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.district_service_id_seq'::regclass),
  district_id integer NOT NULL,
  external_service_id integer NOT NULL,
  requires_personnel boolean NOT NULL,
  initiation_date date,
  expiration_date date,
  CONSTRAINT district_service_pkey PRIMARY KEY (id)
);


/* ----------- DISTRICT SERVICE PERSONNEL ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'district_service_personnel_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.district_service_personnel_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.district_service_personnel
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.district_service_personnel_id_seq'::regclass),
  district_service_id integer NOT NULL,
  personnel_id integer NOT NULL,
  role text,
  CONSTRAINT district_service_personnel_pkey PRIMARY KEY (id)
);


/* ----------- DATA SET ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'data_set_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.data_set_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.data_set
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.data_set_id_seq'::regclass),
  name text NOT NULL,
  description text,
  CONSTRAINT data_set_pkey PRIMARY KEY (id)
);


/* ----------- DATA OBJECT ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'data_object_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.data_object_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.data_object
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.data_object_id_seq'::regclass),
  data_set_id integer,
  name text NOT NULL,
  filter_type text NOT NULL,
  include_statement text NOT NULL,
  CONSTRAINT data_object_pkey PRIMARY KEY (id)
);

CREATE OR REPLACE FUNCTION srx_services_prs.update_data_set_data_object(d_s_id integer, d_o_id integer, n text, ft text, ins text) RETURNS void AS
$$
BEGIN
  IF NOT EXISTS(select * from srx_services_prs.data_object where data_set_id = d_s_id and id = d_o_id)
  THEN insert into srx_services_prs.data_object (id, data_set_id, name, filter_type, include_statement) values (DEFAULT, d_s_id, n, ft, ins);
  END IF;
END;
$$ LANGUAGE plpgsql;


/* ----------- DISTRICT SERVICE DATA SET ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'district_service_data_set_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.district_service_data_set_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.district_service_data_set
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.district_service_data_set_id_seq'::regclass),
  district_service_id integer NOT NULL,
  data_set_id integer NOT NULL,
  CONSTRAINT district_service_data_set_pkey PRIMARY KEY (id)
);

CREATE OR REPLACE FUNCTION srx_services_prs.update_district_service_data_set(dist_serv_id integer, d_s_id integer) RETURNS void AS
$$
BEGIN
  IF NOT EXISTS(select * from srx_services_prs.district_service_data_set where district_service_id = dist_serv_id and data_set_id = d_s_id)
  THEN insert into srx_services_prs.district_service_data_set (id, district_service_id, data_set_id) values (DEFAULT, dist_serv_id, d_s_id);
  END IF;
END;
$$ LANGUAGE plpgsql;



/* ----------- CONSENT ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'consent_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.consent_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.consent
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.consent_id_seq'::regclass),
  district_service_id integer NOT NULL,
  consent_type text NOT NULL,
  start_date date NOT NULL,
  end_date date NOT NULL,
  CONSTRAINT consent_pkey PRIMARY KEY (id)
);


/* ----------- STUDENT ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'student_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.student_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.student
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.student_id_seq'::regclass),
  district_service_id integer NOT NULL,
  consent_id integer NOT NULL,
  district_student_id text NOT NULL,
  CONSTRAINT student_pkey PRIMARY KEY (id)
);


/* ----------- STUDENT PERSONNEL ----------- */

DO
$$
BEGIN
  IF NOT EXISTS (
    SELECT * FROM information_schema.sequences
    WHERE sequence_schema = 'srx_services_prs' AND sequence_name = 'student_personnel_id_seq'
    )
  THEN
    CREATE SEQUENCE srx_services_prs.student_personnel_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS srx_services_prs.student_personnel
(
  id integer NOT NULL DEFAULT nextval('srx_services_prs.student_personnel_id_seq'::regclass),
  student_id integer NOT NULL,
  personnel_id integer NOT NULL,
  CONSTRAINT student_personnel_pkey PRIMARY KEY (id)
);
