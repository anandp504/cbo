DROP TABLE IF EXISTS `cbo_db`.`network_advertiser_frequency`;
CREATE TABLE  `cbo_db`.`network_advertiser_frequency` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `hour` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `creative_frequency_bucket_id` int(11) NOT NULL,
  `creative_frequency_bucket` varchar(16) NOT NULL,
  `imps` bigint(20) NOT NULL,
  `clicks` bigint(20) NOT NULL,
  `media_cost` double NOT NULL,
  `cost_ecpm` double NOT NULL,
  PRIMARY KEY  (`advertiser_id`,`campaign_id`,`hour`,`creative_frequency_bucket_id`,`line_item_id`)
);

DROP TABLE IF EXISTS `cbo_db`.`network_site_domain_performance`;
CREATE TABLE  `cbo_db`.`network_site_domain_performance` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `day` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `site_domain` varchar(256) NOT NULL,
  `imps` bigint(20) NOT NULL,
  `clicks` bigint(20) NOT NULL,
  `media_cost` double NOT NULL,
  `cpm` double NOT NULL,
  PRIMARY KEY  (`advertiser_id`,`campaign_id`,`day`,`site_domain`,`line_item_id`)
);

DROP TABLE IF EXISTS `cbo_db`.`site_domain_report_known_dates`;
CREATE TABLE  `cbo_db`.`site_domain_report_known_dates` (
  `day` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`day`)
);
