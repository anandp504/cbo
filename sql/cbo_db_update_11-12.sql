
-- This just updates the indexes by creating a new table,
-- copying the old data into the new table, and then renaming.

DROP TABLE IF EXISTS observeddata_v12;
CREATE TABLE `observeddata_v12` (
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
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;


insert into observeddata_v12 
   (select * from observeddata);

drop table observeddata;

rename table observeddata_v12 to observeddata;

alter table changelog remove partitioning;
alter table events remove partitioning;
alter table historicaldata remove partitioning;
alter table network_site_domain_performance remove partitioning;


