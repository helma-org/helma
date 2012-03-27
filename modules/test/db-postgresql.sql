
CREATE TABLE tb_person (
  person_id INTEGER NOT NULL,
  person_name VARCHAR(100),
  person_height INTEGER,
  person_dateofbirth TIMESTAMP,
  person_org_id INTEGER,
  PRIMARY KEY (person_id)
);

CREATE TABLE tb_organisation (
   org_id INTEGER NOT NULL,
   org_name VARCHAR(100),
   org_country VARCHAR(100),
   PRIMARY KEY (org_id)
);

CREATE USER helma WITH PASSWORD 'secret';
GRANT insert, delete, select, update ON tb_person, tb_organisation TO helma;

