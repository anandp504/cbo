DROP TABLE IF EXISTS `cbo_db`.`users`;
CREATE TABLE  `cbo_db`.`users` (
  `email_address` varchar(256) NOT NULL default '',
  `for_users` boolean NOT NULL default false,
  `for_admins` boolean NOT NULL default false,
  `id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;