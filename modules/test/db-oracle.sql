CREATE TABLE tb_person (
  person_id NUMBER(10) NOT NULL,
  person_name VARCHAR2(255),
  person_height NUMBER(10),
  person_dateofbirth DATE,
  person_org_id NUMBER(10),
  PRIMARY KEY (person_id)
);

CREATE TABLE tb_organisation (
   org_id NUMBER(10) NOT NULL,
   org_name VARCHAR2(255),
   org_country VARCHAR2(255),
   PRIMARY KEY (org_id)
);
