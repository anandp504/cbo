delimiter $$

DROP TABLE IF EXISTS High_Water_Mark$$

create table High_Water_Mark
    (advertiser_id int NOT NULL, 
     campaign_id int NOT NULL,
     hwm int NULL, 
     dirty boolean)
     ENGINE=InnoDB DEFAULT CHARSET=utf8$$

alter table High_Water_Mark add primary key (advertiser_id, campaign_id)$$

drop function if exists get_high_water_mark$$

create function get_high_water_mark
    (adv_id int, camp_id int)
  returns int
begin
  declare res int;
  declare dirtyP boolean;
  select dirty into dirtyP
  from high_water_mark t
  where t.advertiser_id = adv_id
  and   t.campaign_id   = camp_id;
  if(dirtyP is not null and dirtyP = true)
  then set res = null;
  else
    select hwm into res
    from high_water_mark t
    where t.advertiser_id = adv_id
    and   t.campaign_id = camp_id;
    if(res is null)
    then
      if(select t.advertiser_id from observeddata t
         where t.advertiser_id = adv_id
         and   t.campaign_id = camp_id
         limit 1) is null
      then set res = -1;
      else set res = null;
      end if;
    else
      update high_water_mark
      set dirty = true
      where high_water_mark.advertiser_id = adv_id
      and   high_water_mark.campaign_id = camp_id;
    end if;
  end if;
  return res;
end$$


drop procedure if exists record_high_water_mark$$

create procedure record_high_water_mark
    (in advertiser_id int, in campaign_id int, in mark int)
begin
  insert into high_water_mark
    values(advertiser_id, campaign_id, mark, false)
  on duplicate key update
      dirty = false,
      hwm = mark;
end$$


delimiter ;
