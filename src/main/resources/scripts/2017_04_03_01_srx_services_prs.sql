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
    join srx_services_prs.consent on student.consent_id = consent.id
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
    and CURRENT_DATE between consent.start_date and consent.end_date
  order by data_object.include_statement
$$ LANGUAGE sql;
