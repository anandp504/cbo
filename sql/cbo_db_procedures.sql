-- DON'T DELETE THIS IMPORTANT NOTE - Use $$ instead of SEMICOLON as DELIMITER in this file

USE CBO_DB$$

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

drop function if exists get_entropy$$

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

-- @author: Anand Parthasarathy
-- 05/22/13 - Anand - initial version created
DROP PROCEDURE IF EXISTS cbo_purge_data$$

CREATE PROCEDURE cbo_purge_data(retain_duration int, out error_code int, out message TEXT)
BEGIN
		
	DECLARE EXIT HANDLER FOR SQLEXCEPTION 
		BEGIN
			SET error_code = -1;
			ROLLBACK;
			SET message = concat(coalesce(message, ''), '|Error occurred when cleaning up CBO_DB data...');
		END;
	
	BEGIN
		DECLARE l_advertiser_id INT DEFAULT 0;
		DECLARE l_line_item_id INT DEFAULT 0;
		DECLARE l_campaign_id INT DEFAULT 0;
		DECLARE l_end_date DATE;
		DECLARE exit_loop BOOLEAN DEFAULT FALSE;
		DECLARE delete_count INT DEFAULT 0;
				
		-- DECLARE old_campaigns_cur CURSOR FOR SELECT advertiser_id, line_item_id, campaign_id FROM old_campaigns;
		DECLARE old_campaigns_cur CURSOR FOR SELECT advertiser_id, line_item_id, campaign_id, max(end_date) as end_date
    	FROM observeddata GROUP BY advertiser_id, line_item_id, campaign_id
  		HAVING end_date < date_sub(now(), INTERVAL retain_duration DAY);
		
		DECLARE CONTINUE HANDLER FOR NOT FOUND
			BEGIN
				SET message = concat(coalesce(message, ''), '|No more campaigns to process...');
				SET exit_loop = TRUE;
			END;
			
		OPEN old_campaigns_cur;
		
		old_campaigns_loop: LOOP
			
			FETCH old_campaigns_cur INTO l_advertiser_id, l_line_item_id, l_campaign_id, l_end_date;
			
			IF exit_loop THEN
				LEAVE old_campaigns_loop;
			END IF;
			
			START TRANSACTION;
			
			-- BIDHISTORY table
			SELECT COUNT(1) INTO delete_count FROM bidhistory WHERE advertiser_id = l_advertiser_id
			AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			
			IF (delete_count > 0) THEN			
				DELETE FROM bidhistory WHERE advertiser_id = l_advertiser_id AND line_item_id = l_line_item_id	AND campaign_id = l_campaign_id;
			END IF;
			
			-- CAMPAIGNSETTINGS table
			SELECT COUNT(1) INTO delete_count FROM campaignsettings WHERE advertiser_id = l_advertiser_id
			AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			
			IF (delete_count > 0) THEN	
				DELETE FROM campaignsettings WHERE advertiser_id = l_advertiser_id AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			END IF;
			
			-- CHANGELOG table
			SELECT COUNT(1) INTO delete_count FROM changelog WHERE advertiser_id = l_advertiser_id
			AND campaign_id = l_campaign_id;
			
			IF (delete_count > 0) THEN
				DELETE FROM changelog WHERE advertiser_id = l_advertiser_id AND campaign_id = l_campaign_id;
			END IF;
		
			-- DAILYIMPRESSIONBUDGETHISTORY table
			SELECT COUNT(1) INTO delete_count FROM dailyimpressionbudgethistory WHERE advertiser_id = l_advertiser_id
			AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			
			IF (delete_count > 0) THEN
				DELETE FROM dailyimpressionbudgethistory WHERE advertiser_id = l_advertiser_id
				AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			END IF;
			
			-- EVENTS table
			SELECT COUNT(1) INTO delete_count FROM events WHERE advertiser_id = l_advertiser_id AND campaign_id = l_campaign_id;
			
			IF (delete_count > 0) THEN
				DELETE FROM events WHERE advertiser_id = l_advertiser_id AND campaign_id = l_campaign_id;
			END IF;
		
			-- HIGH_WATER_MARK table
			SELECT COUNT(1) INTO delete_count FROM high_water_mark WHERE advertiser_id = l_advertiser_id AND campaign_id = l_campaign_id;
			
			IF (delete_count > 0) THEN
				DELETE FROM high_water_mark	WHERE advertiser_id = l_advertiser_id AND campaign_id = l_campaign_id;
			END IF;
					
			-- HISTORICALDATA table		
			SELECT COUNT(1) INTO delete_count FROM historicaldata WHERE advertiser_id = l_advertiser_id
			AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
				
			IF (delete_count > 0) THEN	
				DELETE FROM historicaldata WHERE advertiser_id = l_advertiser_id AND line_item_id = l_line_item_id
				AND campaign_id = l_campaign_id;
			END IF;
			
			-- NETWORK_ADVERTISER_FREQUENCY table
			SELECT COUNT(1) INTO delete_count FROM network_advertiser_frequency WHERE advertiser_id = l_advertiser_id
			AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
					
			IF (delete_count > 0) THEN		
				DELETE FROM network_advertiser_frequency WHERE advertiser_id = l_advertiser_id
				AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			END IF;
			
			-- NETWORK_SITE_DOMAIN_PERFORMANCE table
			SELECT COUNT(1) INTO delete_count FROM network_site_domain_performance WHERE advertiser_id = l_advertiser_id
			AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			
			IF(delete_count > 0) THEN
				DELETE FROM network_site_domain_performance	WHERE advertiser_id = l_advertiser_id
				AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			END IF;
		
			-- OBSERVEDDATA tab;e
			SELECT COUNT(1) INTO delete_count FROM observeddata WHERE advertiser_id = l_advertiser_id
			AND line_item_id = l_line_item_id AND campaign_id = l_campaign_id;
			
			IF (delete_count > 0) THEN
				DELETE FROM observeddata WHERE advertiser_id = l_advertiser_id	AND line_item_id = l_line_item_id
				AND campaign_id = l_campaign_id;
			END IF;
		
			COMMIT;
		
			SET message = concat(coalesce(message, ''), '|Processed campaig_id ', l_campaign_id);
			
		END LOOP;
		
		CLOSE old_campaigns_cur;
	END;

	SET error_code = 1;

END$$
