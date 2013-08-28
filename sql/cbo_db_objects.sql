USE CBO_DB;


DROP TABLE IF EXISTS advertisernames;
CREATE TABLE `advertisernames` (
  `id` int(11) NOT NULL PRIMARY KEY,
  `name` varchar(255) NOT NULL
 ) ENGINE=InnoDB CHARSET=UTF8;
  
  
DROP TABLE IF EXISTS bidhistory;
CREATE TABLE `bidhistory` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `bid_strategy` varchar(64) NOT NULL default 'ECP',
  `bid` double NOT NULL,
  `daily_impression_budget` bigint(20) DEFAULT NULL,
  `daily_impression_target` bigint(20) DEFAULT NULL,
  `event_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`advertiser_id`,`line_item_id`,`campaign_id`,`event_time`)
) ENGINE=InnoDB CHARSET=UTF8;
  
  
DROP TABLE IF EXISTS campaignnames;
CREATE TABLE `campaignnames` (
  `id` int(11) NOT NULL PRIMARY KEY,
  `name` varchar(255) NOT NULL
) ENGINE=InnoDB CHARSET=UTF8;


DROP TABLE IF EXISTS campaignsettings;
CREATE TABLE `campaignsettings` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `max_bid` double NOT NULL,
  `daily_budget_imps` bigint(20) NOT NULL,
  `policy` varchar(30) NOT NULL,
  PRIMARY KEY (`advertiser_id`,`line_item_id`,`campaign_id`)
) ENGINE=InnoDB CHARSET=UTF8;


DROP TABLE IF EXISTS changelog;
CREATE TABLE `changelog` (
  `advertiser_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `profile_id` int(11) NOT NULL,
  `change_type` varchar(64) NOT NULL,
  `s0` varchar(255) NOT NULL,
  `s1` varchar(255) NOT NULL,
  `s2` varchar(255) NOT NULL,
  `s3` varchar(255) NOT NULL,
  `n0` double NOT NULL,
  `n1` double NOT NULL,
  `n2` double NOT NULL,
  `n3` double NOT NULL,
  `campaign_json` longtext NOT NULL,
  `profile_json` longtext NOT NULL,
  `event_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`advertiser_id`,`campaign_id`,`event_time`)
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE IF EXISTS dailyimpressionbudgethistory;
CREATE TABLE `dailyimpressionbudgethistory` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `daily_budget` int(11) NOT NULL,
  `lifetime_budget` int NULL DEFAULT NULL,
  `event_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`advertiser_id`,`line_item_id`,`campaign_id`,`event_time`)
) ENGINE=InnoDB CHARSET=UTF8;
   

DROP TABLE If EXISTS `events`;
CREATE TABLE `events` (
  `advertiser_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `description` longtext NOT NULL,
  `event_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`campaign_id`,`event_type`,`advertiser_id`,`event_time`)
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE If EXISTS historicaldata;
CREATE TABLE `historicaldata` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `hour` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `imps` bigint(20) NOT NULL,
  `clicks` bigint(20) NOT NULL,
  `cost` double NOT NULL,
  PRIMARY KEY (`advertiser_id`,`line_item_id`,`campaign_id`,`hour`),
  KEY `ix1` (`hour`,`advertiser_id`,`campaign_id`)
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE IF EXISTS historicaldataknowndates;
CREATE TABLE `historicaldataknowndates` (
  `hour` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`hour`)
) ENGINE=InnoDB CHARSET=UTF8;


DROP TABLE IF EXISTS lineitemnames;
CREATE TABLE `lineitemnames` (
  `id` int(11) NOT NULL PRIMARY KEY,
  `name` varchar(255) NOT NULL
) ENGINE=InnoDB CHARSET=UTF8;


DROP TABLE IF EXISTS observeddata;
CREATE TABLE `observeddata` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `line_item_profile_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `campaign_profile_id` int(11) NOT NULL,
  `observation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `observation_day` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `base_bid` double NOT NULL,
  `max_bid` double NOT NULL,
  `daily_impressions_budget` int(11) NOT NULL,
  `lifetime_impressions_budget` int(11) NOT NULL,
  `start_date` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `end_date` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `first_impression_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `user_group_low` int(11) NOT NULL,
  `user_group_high` int(11) NOT NULL,
  `is_child` int(11) NOT NULL,
  `day_of_week` int(11) NOT NULL,
  `day_type` int(11) NOT NULL,
  `bidding_policy` varchar(32) CHARACTER SET LATIN1 NOT NULL,
  `line_item_json` longtext NOT NULL,
  `line_item_profile_json` longtext NOT NULL,
  `campaign_json` longtext NOT NULL,
  `campaign_profile_json` longtext NOT NULL,
  `combined_json` longtext NOT NULL,
  `combined_profile_json` longtext NOT NULL,
  `sequence_number` int(11) DEFAULT NULL,
  `materially_different` tinyint(1) DEFAULT NULL,
  `has_material_differences` tinyint(1) DEFAULT 0,
  `material_differences` longtext,
  `attribute_changed_but_will_not_affect_delivery` int(11) DEFAULT NULL,
  `attribute_changed_with_unknown_effect_on_delivery` int(11) DEFAULT NULL,
  `attribute_changed_increases_delivery` int(11) DEFAULT NULL,
  `attribute_changed_decreases_delivery` int(11) DEFAULT NULL,
  `attribute_increased_increases_delivery` int(11) DEFAULT NULL,
  `attribute_decreased_decreases_delivery` int(11) DEFAULT NULL,
  `attribute_increased_decreases_delivery` int(11) DEFAULT NULL,
  `attribute_decreased_increases_delivery` int(11) DEFAULT NULL,
  `targeting_widened_increases_delivery` int(11) DEFAULT NULL,
  `targeting_narrowed_decreases_delivery` int(11) DEFAULT NULL,
  `last_modified` timestamp NOT NULL default '0000-00-00 00:00:00',
  `active` boolean NULL default true,
  `timezone` varchar(16) NULL default NULL,
  `stats_clicks` bigint NOT NULL default 0,
  `stats_ecpm` double NOT NULL default 0.0,
  `stats_imps` bigint NOT NULL default 0,
  `stats_media_cost` double NOT NULL default 0.0,
  `control_bid_strategy` varchar(64) NOT NULL default 'ECP',
  `control_max_bid` double NOT NULL default 0.0,
  `control_daily_impression_budget` bigint NOT NULL default 0,
  `control_daily_impression_target` bigint NOT NULL default 0,
  PRIMARY KEY (`observation_time`,`advertiser_id`,`campaign_id`),
  UNIQUE KEY `ix2` (`campaign_id`,`advertiser_id`,`campaign_profile_id`,`observation_time`),
  UNIQUE KEY `ix3` (`campaign_profile_id`,`advertiser_id`,`campaign_id`,`observation_time`),
  UNIQUE KEY `ix1` (`campaign_id`,`advertiser_id`,`sequence_number`,`materially_different`,`observation_time`),
  KEY `ix4` (`line_item_id`,`advertiser_id`,`line_item_profile_id`,`observation_time`),
  KEY `ix5` (`line_item_profile_id`,`advertiser_id`,`line_item_id`,`observation_time`),
  KEY `ix6` (`materially_different`, `has_material_differences`, `advertiser_id`, `campaign_id`, `sequence_number`, `observation_time`),
  KEY `ix7` (`has_material_differences`,`advertiser_id`,`campaign_id`,`observation_time`,`line_item_id`),
  KEY `ix8` (`advertiser_id`, `campaign_id`, `materially_different`),
  KEY `ix9` (`campaign_id`,`sequence_number`),
  KEY `ix10` (`advertiser_id`, `campaign_id`, `bidding_policy`(4), `observation_time`)
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE IF EXISTS `cbo_db`.`high_water_mark`;
create table high_water_mark (
  advertiser_id int NOT NULL, 
  campaign_id int NOT NULL,
  hwm int NULL, 
  dirty boolean,
  PRIMARY KEY (advertiser_id, campaign_id)
) ENGINE=InnoDB CHARSET=UTF8;

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
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE IF EXISTS `cbo_db`.`network_site_domain_performance`;
CREATE TABLE  `cbo_db`.`network_site_domain_performance` (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `day` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `site_domain` varchar(255) NOT NULL,
  `imps` bigint(20) NOT NULL,
  `clicks` bigint(20) NOT NULL,
  `media_cost` double NOT NULL,
  `cpm` double NOT NULL,
  PRIMARY KEY  (`advertiser_id`,`campaign_id`,`day`,`site_domain`,`line_item_id`),
  KEY `ix` (`campaign_id`, `advertiser_id`, `day`)
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE IF EXISTS `cbo_db`.`site_domain_report_known_dates`;
CREATE TABLE  `cbo_db`.`site_domain_report_known_dates` (
  `day` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`day`)
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE IF EXISTS `cbo_db`.`perpetrators`;
CREATE TABLE  `cbo_db`.`perpetrators` (
  `perpetrator` varchar(255) NOT NULL default '',
  `event_name` varchar(255) NOT NULL default '',
  `event_time` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `param` longtext NOT NULL,
  PRIMARY KEY  (`event_name`,`perpetrator`,`event_time`)
) ENGINE=InnoDB CHARSET=UTF8;

DROP TABLE IF EXISTS `cbo_db`.`users`;
CREATE TABLE  `cbo_db`.`users` (
  `email_address` varchar(255) NOT NULL default '',
  `for_users` boolean NOT NULL default false,
  `for_admins` boolean NOT NULL default false,
  `id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB CHARSET=UTF8;

-- ============================= VERSION ===================

CREATE TABLE IF NOT EXISTS version_info (
 VERSION       INT(11)         NOT NULL PRIMARY KEY
) ENGINE=InnoDB CHARSET=UTF8;
