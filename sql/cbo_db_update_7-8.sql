alter table observeddata
    add column last_modified timestamp NOT NULL default '0000-00-00 00:00:00',
    add column active boolean NULL default true,
    add column timezone varchar(16) NULL default NULL,
    add column stats_clicks bigint NOT NULL default 0,
    add column stats_ecpm double NOT NULL default 0.0,
    add column stats_imps bigint NOT NULL default 0,
    add column stats_media_cost double NOT NULL default 0.0,
    add column control_bid_strategy varchar(64) NOT NULL default 'ECP',
    add column control_max_bid double NOT NULL default 0.0,
    add column control_daily_impression_budget bigint NOT NULL default 0,
    add column control_daily_impression_target bigint NOT NULL default 0;

alter table bidhistory
    add column bid_strategy varchar(64) NOT NULL default 'ECP' after campaign_id,
    add column daily_impression_target bigint(20) DEFAULT NULL after daily_impression_limit;


alter table bidhistory
    change  daily_impression_limit daily_impression_budget bigint;

DROP TABLE IF EXISTS `cbo_db`.`perpetrators`;
CREATE TABLE  `cbo_db`.`perpetrators` (
  `perpetrator` varchar(256) NOT NULL default '',
  `event_name` varchar(256) NOT NULL default '',
  `event_time` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `param` text NOT NULL,
  PRIMARY KEY  (`event_name`,`perpetrator`,`event_time`)
) ENGINE=InnoDB;