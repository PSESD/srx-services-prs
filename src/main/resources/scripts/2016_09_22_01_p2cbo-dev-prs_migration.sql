SET NOCOUNT ON;
select '/* CONTACTS */';
select 'truncate table srx_services_prs.contact cascade;';
select 'insert into srx_services_prs.contact (id, name, title, email, phone, mailing_address, web_address) values (' 
+ CAST(ContactID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(Name, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(Title, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(Email, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(Phone, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(MailingAddress, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(WebAddress, '''', '''''') + '''', 'null') + ');'
from Contact;
select 'select setval(''srx_services_prs.contact_id_seq'', (select max(id) from srx_services_prs.contact));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* AUTHORIZED ENTITY */';
select 'truncate table srx_services_prs.authorized_entity cascade;';
select 'insert into srx_services_prs.authorized_entity (id, name, main_contact_id) values (' 
+ CAST(AuthorizedEntityID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(AuthorizedEntityName, '''', '''''') + '''', 'null') + ', '
+ ISNULL(CAST(MainContactID as varchar(50)), 'null') + ');'
from AuthorizedEntity;
select 'select setval(''srx_services_prs.authorized_entity_id_seq'', (select max(id) from srx_services_prs.authorized_entity));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* AUTHORIZED ENTITY PERSONNEL */';
select 'truncate table srx_services_prs.personnel cascade;';
select 'insert into srx_services_prs.personnel (id, authorized_entity_id, first_name, last_name) values (' 
+ CAST(PersonnelID as varchar(50)) + ', '
+ CAST(AuthorizedEntityID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(FirstName, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(LastName, '''', '''''') + '''', 'null') + ');'
from Personnel;
select 'select setval(''srx_services_prs.personnel_id_seq'', (select max(id) from srx_services_prs.personnel));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* EXTERNAL SERVICE */';
select 'truncate table srx_services_prs.external_service cascade;';
select 'insert into srx_services_prs.external_service (id, authorized_entity_id, name, description) values (' 
+ CAST(ExternalServiceID as varchar(50)) + ', '
+ CAST(AuthorizedEntityID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(ExternalServiceName, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(ExternalServiceDescription, '''', '''''') + '''', 'null') + ');'
from ExternalService;
select 'select setval(''srx_services_prs.external_service_id_seq'', (select max(id) from srx_services_prs.external_service));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* DISTRICT */';
select 'truncate table srx_services_prs.district cascade;';
select 'insert into srx_services_prs.district (id, name, nces_lea_code, zone_id, main_contact_id) values (' 
+ CAST(DistrictID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(DistrictName, '''', '''''') + '''', 'null') + ', '
+ CAST(NCESLEACode as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(ZoneID, '''', '''''') + '''', 'null') + ', '
+ ISNULL(CAST(MainContactID as varchar(50)), 'null') + ');'
from District;
select 'select setval(''srx_services_prs.district_id_seq'', (select max(id) from srx_services_prs.district));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* DISTRICT SERVICE */';
select 'truncate table srx_services_prs.district_service cascade;';
select 'insert into srx_services_prs.district_service (id, district_id, external_service_id, requires_personnel, initiation_date, expiration_date) values (' 
+ CAST(DistrictServiceID as varchar(50)) + ', '
+ CAST(DistrictID as varchar(50)) + ', '
+ CAST(ExternalServiceID as varchar(50)) + ', '
+ CASE RequiresPersonnel WHEN 1 THEN 'true' ELSE 'false' END + ', '
+ ISNULL('''' + REPLACE(InitiationDate, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(ExpirationDate, '''', '''''') + '''', 'null') + ');'
from DistrictService;
select 'select setval(''srx_services_prs.district_service_id_seq'', (select max(id) from srx_services_prs.district_service));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* DISTRICT SERVICE PERSONNEL */';
select 'truncate table srx_services_prs.district_service_personnel cascade;';
select 'insert into srx_services_prs.district_service_personnel (id, district_service_id, personnel_id, role) values (' 
+ CAST(AuthorizedPersonnelID as varchar(50)) + ', '
+ CAST(DistrictServiceID as varchar(50)) + ', '
+ CAST(PersonnelID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE([Role], '''', '''''') + '''', 'null') + ');'
from AuthorizedPersonnel;
select 'select setval(''srx_services_prs.district_service_personnel_id_seq'', (select max(id) from srx_services_prs.district_service_personnel));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* DATA SET */';
select 'truncate table srx_services_prs.data_set cascade;';
select 'insert into srx_services_prs.data_set (id, name, description) values (' 
+ CAST(DataSetID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(DataSetName, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(DataSetDescription, '''', '''''') + '''', 'null') + ');'
from DataSet;
select 'select setval(''srx_services_prs.data_set_id_seq'', (select max(id) from srx_services_prs.data_set));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* DATA OBJECT */';
select 'truncate table srx_services_prs.data_object cascade;';
select 'insert into srx_services_prs.data_object (id, data_set_id, name, filter_type, include_statement) values (' 
+ CAST(SIFDataObjectID as varchar(50)) + ', '
+ CAST(DataSetID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(SIFObjectName, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(FilterType, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(IncludeStatement, '''', '''''') + '''', 'null') + ');'
from SIFDataObject;
select 'select setval(''srx_services_prs.data_object_id_seq'', (select max(id) from srx_services_prs.data_object));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* DISTRICT SERVICE DATA SET */';
select 'truncate table srx_services_prs.district_service_data_set cascade;';
select 'insert into srx_services_prs.district_service_data_set (id, district_service_id, data_set_id) values (' 
+ CAST(DistrictServiceDataSetID as varchar(50)) + ', '
+ CAST(DistrictServiceID as varchar(50)) + ', '
+ CAST(DataSetID as varchar(50)) + ');'
from DistrictServiceDataSet;
select 'select setval(''srx_services_prs.district_service_data_set_id_seq'', (select max(id) from srx_services_prs.district_service_data_set));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* CONSENT */';
select 'truncate table srx_services_prs.consent cascade;';
select 'insert into srx_services_prs.consent (id, district_service_id, consent_type, start_date, end_date) values (' 
+ CAST(ConsentID as varchar(50)) + ', '
+ CAST(DistrictServiceID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(ConsentType, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(ConsentStartDate, '''', '''''') + '''', 'null') + ', '
+ ISNULL('''' + REPLACE(ConsentEndDate, '''', '''''') + '''', 'null') + ');'
from Consent;
select 'select setval(''srx_services_prs.consent_id_seq'', (select max(id) from srx_services_prs.consent));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* STUDENT */';
select 'truncate table srx_services_prs.student cascade;';
select 'insert into srx_services_prs.student (id, district_service_id, district_student_id, consent_id) values (' 
+ CAST(ServiceStudentID as varchar(50)) + ', '
+ CAST(DistrictServiceID as varchar(50)) + ', '
+ ISNULL('''' + REPLACE(DistrictStudentID, '''', '''''') + '''', 'null') + ', '
+ CAST(ConsentID as varchar(50)) + ');'
from ServiceStudent;
select 'select setval(''srx_services_prs.student_id_seq'', (select max(id) from srx_services_prs.student));';
SET NOCOUNT OFF;

SET NOCOUNT ON;
select '/* STUDENT PERSONNEL */';
select 'truncate table srx_services_prs.student_personnel cascade;';
select 'insert into srx_services_prs.student_personnel (id, student_id, personnel_id) values (' 
+ CAST(PersonnelStudentService.PersonnelStudentServiceID as varchar(50)) + ', '
+ CAST(PersonnelStudentService.ServiceStudentID as varchar(50)) + ', '
+ CAST(AuthorizedPersonnel.PersonnelID as varchar(50)) + ');'
from PersonnelStudentService join AuthorizedPersonnel on AuthorizedPersonnel.AuthorizedPersonnelID = PersonnelStudentService.AuthorizedPersonnelID;
select 'select setval(''srx_services_prs.student_personnel_id_seq'', (select max(id) from srx_services_prs.student_personnel));';
SET NOCOUNT OFF;