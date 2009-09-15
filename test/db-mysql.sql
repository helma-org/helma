
CREATE DATABASE helmaTest;
USE helmaTest;

GRANT ALL ON helmaTest.* TO helma@localhost IDENTIFIED BY 'secret';

CREATE TABLE tb_person (
  person_id MEDIUMINT(10) NOT NULL,
  person_name TINYTEXT,
  person_height TINYINT unsigned,
  person_dateofbirth DATETIME,
  person_org_id MEDIUMINT(10) unsigned,
  PRIMARY KEY (person_id)
);

CREATE TABLE tb_organisation (
   org_id MEDIUMINT(10) unsigned NOT NULL,
   org_name TINYTEXT,
   org_country TINYTEXT,
   PRIMARY KEY (org_id)
);
