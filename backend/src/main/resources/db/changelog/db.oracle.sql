-- liquibase formatted sql

-- changeset create-user-table:1
CREATE TABLE users
(
    username VARCHAR2(255) PRIMARY KEY,
    password VARCHAR2(255)       NOT NULL,
    role     VARCHAR2(20)        NOT NULL,
    deleted  CHAR(1) DEFAULT 'N' not null,
    deleted_as number(1),
    asdasdas varchar(2),
    dsa NUMBER,
    ghfd NUMBER(2,2),
    sgfddf INT
);
