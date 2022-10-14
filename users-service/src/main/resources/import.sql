INSERT INTO users (username, password, enabled, first_name, last_name, email) VALUES ('andres','$2a$10$vBRZXJIuluGq/0txPQI0iu/OP2s60Jw8wYYV4tnYwJFHqKeqYpdw2',true, 'Andres', 'Guzman','profesor@bolsadeideas.com');
INSERT INTO users (username, password, enabled, first_name, last_name, email) VALUES ('admin','$2a$10$/ljzUnajxgSdZU9Ewvc0YONzFAbCcBiSeTaBkL8/UQJ.0oMqorAEW',true, 'John', 'Doe','jhon.doe@bolsadeideas.com');

INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');

INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO users_roles (user_id, role_id) VALUES (2, 2);
INSERT INTO users_roles (user_id, role_id) VALUES (2, 1);
