
alter table cbo_db.observeddata
  add column has_material_differences tinyint(1) not null default 0 after materially_different;
  
update cbo_db.observeddata
         set has_material_differences = 1
  	   where materially_different = 1
  	     and material_differences is not null 
  	     and material_differences <> '';

create index ix7 on cbo_db.observeddata(has_material_differences, advertiser_id, campaign_id, observation_time, line_item_id);
create index ix8 on cbo_db.observeddata (advertiser_id, campaign_id, materially_different);
 
create index ix1 on historicaldata (hour, advertiser_id, campaign_id);
 