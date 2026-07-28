CREATE DATABASE order_service;
CREATE DATABASE inventory_service;
CREATE DATABASE payment_service;
CREATE DATABASE shipping_service;
CREATE DATABASE invoice_service;

GRANT ALL PRIVILEGES ON DATABASE workflow_service TO camunda;
GRANT ALL PRIVILEGES ON DATABASE order_service TO camunda;
GRANT ALL PRIVILEGES ON DATABASE inventory_service TO camunda;
GRANT ALL PRIVILEGES ON DATABASE payment_service TO camunda;
GRANT ALL PRIVILEGES ON DATABASE shipping_service TO camunda;
GRANT ALL PRIVILEGES ON DATABASE invoice_service TO camunda;
