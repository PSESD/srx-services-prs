ALTER TABLE srx_services_prs.contact
ADD CONSTRAINT web_address_unique
UNIQUE (web_address);