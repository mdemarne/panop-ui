# --- Creating the database with a clean evolution cycle

# --- !Ups

CREATE TABLE results (
  id VARCHAR ( 24 ) NOT NULL,
  datetime TIMESTAMP NOT NULL,
  url VARCHAR ( 255 ) NOT NULL,
  matches VARCHAR ( 255 ) NOT NULL,
  PRIMARY KEY(id, url)
);

# --- !Downs

DROP TABLE results;