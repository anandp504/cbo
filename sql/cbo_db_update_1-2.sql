/* 
 This file holds the SQL statements to update the CBO database from version 1 to version 2.
 It is to be copied into cbo_db_update.sql and the cbo_db.version file is to be changed so that
 the scripts will run at install time.

The following can only be run once.
*/

alter table bidhistory
 add column daily_impression_limit bigint null default null after bid;


DROP TABLE IF EXISTS observeddata_new;
CREATE TABLE observeddata_new (
  `advertiser_id` int(11) NOT NULL,
  `line_item_id` int(11) NOT NULL,
  `line_item_profile_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  `campaign_profile_id` int(11) NOT NULL,
  `observation_time` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `observation_day` timestamp NOT NULL default '0000-00-00 00:00:00',
  `base_bid` double NOT NULL,
  `max_bid` double NOT NULL,
  `daily_impressions_budget` int(11) NOT NULL,
  `lifetime_impressions_budget` int(11) NOT NULL,
  `start_date` timestamp NOT NULL default '0000-00-00 00:00:00',
  `end_date` timestamp NOT NULL default '0000-00-00 00:00:00',
  `first_impression_time` timestamp NOT NULL default '0000-00-00 00:00:00',
  `user_group_low` int(11) NOT NULL,
  `user_group_high` int(11) NOT NULL,
  `is_child` int(11) NOT NULL,
  `day_of_week` int(11) NOT NULL,
  `day_type` int(11) NOT NULL,
  `bidding_policy` varchar(32) NOT NULL,
  `line_item_json` text NOT NULL,
  `line_item_profile_json` text NOT NULL,
  `campaign_json` text NOT NULL,
  `campaign_profile_json` text NOT NULL,
  `combined_json` text NOT NULL,
  `combined_profile_json` text NOT NULL,
  `sequence_number` int(11) default NULL,
  `materially_different` tinyint(1) default NULL,
  `material_differences` text default NULL,
  `attribute_changed_but_will_not_affect_delivery` int null default null,
  `attribute_changed_with_unknown_effect_on_delivery` int null default null,
  `attribute_changed_increases_delivery` int null default null,
  `attribute_changed_decreases_delivery` int null default null,
  `attribute_increased_increases_delivery` int null default null,
  `attribute_decreased_decreases_delivery` int null default null,
  `attribute_increased_decreases_delivery` int null default null,
  `attribute_decreased_increases_delivery` int null default null,
  `targeting_widened_increases_delivery` int null default null,
  `targeting_narrowed_decreases_delivery` int null default null,
  PRIMARY KEY  (`advertiser_id`,`campaign_id`,`observation_time`)
) ENGINE=InnoDB;

create unique index ix1 on observeddata_new
    (campaign_id, advertiser_id, sequence_number, materially_different,
     observation_time);

create unique index ix2 on observeddata_new
    (campaign_id, advertiser_id, campaign_profile_id, observation_time);

create unique index ix3 on observeddata_new
    (campaign_profile_id, advertiser_id, campaign_id, observation_time);

create index ix4 on observeddata_new
    (line_item_id, advertiser_id, line_item_profile_id, observation_time);

create index ix5 on observeddata_new
    (line_item_profile_id, advertiser_id, line_item_id, observation_time);


insert into observedData_new
select advertiser_id,
       line_item_id,
       -1, -- line_item_profile_id,
       campaign_id,
       profile_id,
       observation_time,
       observation_day,
       base_bid,
       max_bid,
       daily_impressions_budget,
       lifetime_impressions_budget,
       start_date,
       end_date,
       first_impression_time,
       user_group_low,
       user_group_high,
       is_child,
       day_of_week,
       day_type,
       bidding_policy,
       "", -- line_item_json,
       "", -- line_item_profile_json,
       campaign_json,
       profile_json,
       campaign_json, -- combined_json,
       profile_json, -- combined_profile_json,
       null, -- sequence_number,
       null, -- materially_different,
       null, -- material_differences,
       null, -- attribute_changed_but_will_not_affect_delivery,
       null, -- attribute_changed_with_unknown_effect_on_delivery,
       null, -- attribute_changed_increases_delivery,
       null, -- attribute_changed_decreases_delivery,
       null, -- attribute_increased_increases_delivery,
       null, -- attribute_decreased_decreases_delivery,
       null, -- attribute_increased_decreases_delivery,
       null, -- attribute_decreased_increases_delivery,
       null, -- targeting_widened_increases_delivery,
       null  -- targeting_narrowed_decreases_delivery
from observeddata;


DROP TABLE IF EXISTS observeddata_old;

rename table observeddata to observeddata_old,
             observeddata_new to observeddata;






