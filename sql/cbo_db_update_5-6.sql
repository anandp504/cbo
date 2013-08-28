delimiter $$

drop function if exists get_entropy $$

create function get_entropy(adv_id int, camp_id int, on_day timestamp) 
  returns double
begin
  declare total bigint;
  declare res double;
  set total = (select sum(imps) from network_site_domain_performance
               where advertiser_id = adv_id
               and   campaign_id = camp_id
               and   day = on_day);
  if ((total is null) or (total <= 0)) then set res = 0;
  else set res = (select sum(Pxi * LogPxi)
             FROM (SELECT imps, imps/total AS Pxi, log2(imps/total) AS LogPxi
                   FROM network_site_domain_performance n
                   where advertiser_id = adv_id
                   and   campaign_id = camp_id
                   and   day = on_day) T1);
  end if;

  if res >= 0 then set res = 0;
  else set res = -res;
  end if;

  return res;
  
end$$


delimiter ;