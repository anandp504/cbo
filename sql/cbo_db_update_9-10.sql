
alter table cbo_db.changelog
  change campaign_json campaign_json longtext NOT NULL,
  change profile_json profile_json longtext NOT NULL;

alter table cbo_db.events
  change description description longtext NOT NULL;

alter table cbo_db.observeddata
  change line_item_json line_item_json longtext NOT NULL,
  change line_item_profile_json line_item_profile_json longtext NOT NULL,
  change campaign_json campaign_json longtext NOT NULL,
  change campaign_profile_json campaign_profile_json longtext NOT NULL,
  change combined_json combined_json longtext NOT NULL,
  change combined_profile_json combined_profile_json longtext NOT NULL,
  change material_differences material_differences longtext;

alter table cbo_db.perpetrators
  change param param longtext NOT NULL;
  