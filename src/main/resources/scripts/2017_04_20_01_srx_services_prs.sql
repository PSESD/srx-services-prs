ALTER TABLE srx_services_prs.external_service
DROP CONSTRAINT external_service_unique;

ALTER TABLE srx_services_prs.external_service
ADD CONSTRAINT external_service_unique
UNIQUE (authorized_entity_id);
