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

CREATE OR REPLACE FUNCTION delete_contact_row ()
 RETURNS trigger LANGUAGE plpgsql AS
$$
BEGIN
 DELETE FROM srx_services_prs.contact WHERE id = OLD.main_contact_id;
 RETURN NEW;
END;
$$;

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
  CONSTRAINT authorized_entity_pkey PRIMARY KEY (id),
  CONSTRAINT authorized_entity_unique UNIQUE (name)
);

DROP TRIGGER IF EXISTS delete_authorized_entity_contact ON srx_services_prs.authorized_entity;

CREATE TRIGGER delete_authorized_entity_contact
 AFTER DELETE
 ON srx_services_prs.authorized_entity
 FOR EACH ROW
 EXECUTE PROCEDURE delete_contact_row();


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
  authorized_entity_id integer NOT NULL REFERENCES srx_services_prs.authorized_entity ON DELETE CASCADE,
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
  authorized_entity_id integer NOT NULL REFERENCES srx_services_prs.authorized_entity ON DELETE CASCADE,
  name text ,
  description text,
  CONSTRAINT external_service_pkey PRIMARY KEY (id),
  CONSTRAINT external_service_unique UNIQUE (authorized_entity_id, name)
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
  CONSTRAINT district_pkey PRIMARY KEY (id),
  CONSTRAINT district_unique UNIQUE (name)
);

DROP TRIGGER IF EXISTS delete_district_contact ON srx_services_prs.district;

CREATE TRIGGER delete_district_contact
 AFTER DELETE
 ON srx_services_prs.district
 FOR EACH ROW
 EXECUTE PROCEDURE delete_contact_row();


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
  district_id integer NOT NULL REFERENCES srx_services_prs.district ON DELETE CASCADE,
  external_service_id integer NOT NULL REFERENCES srx_services_prs.external_service ON DELETE CASCADE,
  requires_personnel boolean NOT NULL,
  initiation_date date,
  expiration_date date,
  CONSTRAINT district_service_pkey PRIMARY KEY (id),
  CONSTRAINT district_service_unique UNIQUE (district_id, external_service_id)
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
  district_service_id integer NOT NULL REFERENCES srx_services_prs.district_service ON DELETE CASCADE,
  personnel_id integer NOT NULL REFERENCES srx_services_prs.personnel ON DELETE CASCADE,
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
  CONSTRAINT data_set_pkey PRIMARY KEY (id),
  CONSTRAINT data_set_unique UNIQUE (name)
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
  data_set_id integer REFERENCES srx_services_prs.data_set ON DELETE CASCADE,
  name text NOT NULL,
  filter_type text NOT NULL,
  include_statement text NOT NULL,
  CONSTRAINT data_object_pkey PRIMARY KEY (id),
  CONSTRAINT data_object_unique UNIQUE (data_set_id, name, filter_type, include_statement)
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
  district_service_id integer NOT NULL REFERENCES srx_services_prs.district_service ON DELETE CASCADE,
  data_set_id integer NOT NULL REFERENCES srx_services_prs.data_set ON DELETE CASCADE,
  CONSTRAINT district_service_data_set_pkey PRIMARY KEY (id),
  CONSTRAINT district_service_data_set_unique UNIQUE (district_service_id, data_set_id)
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
  district_service_id integer NOT NULL REFERENCES srx_services_prs.district_service ON DELETE CASCADE,
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
  district_service_id integer NOT NULL REFERENCES srx_services_prs.district_service ON DELETE CASCADE,
  consent_id integer NOT NULL REFERENCES srx_services_prs.consent ON DELETE CASCADE,
  district_student_id text NOT NULL,
  CONSTRAINT student_pkey PRIMARY KEY (id),
  CONSTRAINT student_unique UNIQUE (district_service_id, district_student_id)
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
  student_id integer NOT NULL REFERENCES srx_services_prs.student ON DELETE CASCADE,
  personnel_id integer NOT NULL REFERENCES srx_services_prs.personnel ON DELETE CASCADE,
  CONSTRAINT student_personnel_pkey PRIMARY KEY (id)
);


/* ----------- FILTER SET ----------- */

CREATE OR REPLACE FUNCTION srx_services_prs.get_filter_set(
  data_object_name_param text,
  zone_id_param text,
  external_service_id_param integer,
  district_student_id_param text,
  authorized_entity_id_param integer,
  personnel_id_param integer) RETURNS TABLE(
    data_object_id integer,
    data_set_id integer,
    filter_type text,
    data_object_name text,
    include_statement text
  ) AS
$$
  select distinct
    data_object.id,
    data_object.data_set_id,
    data_object.filter_type,
    data_object.name,
    data_object.include_statement
  from srx_services_prs.authorized_entity
    join srx_services_prs.external_service on external_service.authorized_entity_id = authorized_entity.id
    join srx_services_prs.district_service on district_service.external_service_id = external_service.id
    join srx_services_prs.district on district.id = district_service.district_id
    join srx_services_prs.student on student.district_service_id = district_service.id
    join srx_services_prs.district_service_data_set on district_service_data_set.district_service_id = district_service.id
    join srx_services_prs.data_set on data_set.id = district_service_data_set.data_set_id
    join srx_services_prs.data_object on data_object.data_set_id = data_set.id
    left join srx_services_prs.district_service_personnel on district_service_personnel.district_service_id = district_service.id
    left join srx_services_prs.student_personnel on student_personnel.student_id = student.id and student_personnel.personnel_id = district_service_personnel.personnel_id
  where district.zone_id = zone_id
    and authorized_entity.id = authorized_entity_id_param
    and external_service.id = external_service_id_param
    and student.district_student_id = district_student_id_param
    and (personnel_id_param is null or student_personnel.personnel_id = personnel_id_param)
    and data_object.name = data_object_name_param
    and CURRENT_DATE between district_service.initiation_date and district_service.expiration_date
  order by data_object.include_statement
$$ LANGUAGE sql;
