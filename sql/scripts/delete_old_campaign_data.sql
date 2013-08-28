-- Deletes data from campaigns in CBO that are more than 90 days old.
-- Then optimizes the tables.
-- This should be run with the bidder shut down.
-- Please back up the existing database before running this.

create temporary table old_campaigns as 
  select advertiser_id, campaign_id, max(end_date) as max_end_date
    from observeddata
group by advertiser_id, campaign_id
  having max_end_date < date_sub(now(), INTERVAL 90 DAY);

delete from bidhistory
      using bidhistory, old_campaigns
      where bidhistory.advertiser_id = old_campaigns.advertiser_id
        and bidhistory.campaign_id = old_campaigns.campaign_id;

delete from campaignsettings 
      using campaignsettings, old_campaigns 
      where campaignsettings.advertiser_id = old_campaigns.advertiser_id 
        and campaignsettings.campaign_id = old_campaigns.campaign_id;

delete from changelog
      using changelog, old_campaigns
      where changelog.advertiser_id = old_campaigns.advertiser_id
        and changelog.campaign_id = old_campaigns.campaign_id;

delete from dailyimpressionbudgethistory
      using dailyimpressionbudgethistory, old_campaigns
      where dailyimpressionbudgethistory.advertiser_id = old_campaigns.advertiser_id
        and dailyimpressionbudgethistory.campaign_id = old_campaigns.campaign_id;
        
delete from events
      using events, old_campaigns
      where events.advertiser_id = old_campaigns.advertiser_id
        and events.campaign_id = old_campaigns.campaign_id;

delete from high_water_mark
      using high_water_mark, old_campaigns
      where high_water_mark.advertiser_id = old_campaigns.advertiser_id
        and high_water_mark.campaign_id = old_campaigns.campaign_id;

delete from historicaldata
      using historicaldata, old_campaigns
      where historicaldata.advertiser_id = old_campaigns.advertiser_id
        and historicaldata.campaign_id = old_campaigns.campaign_id;

delete from observeddata
      using observeddata, old_campaigns
      where observeddata.advertiser_id = old_campaigns.advertiser_id
        and observeddata.campaign_id = old_campaigns.campaign_id;

delete from network_advertiser_frequency
      using network_advertiser_frequency, old_campaigns
      where network_advertiser_frequency.advertiser_id = old_campaigns.advertiser_id
        and network_advertiser_frequency.campaign_id = old_campaigns.campaign_id;

delete from network_site_domain_performance
      using network_site_domain_performance, old_campaigns
      where network_site_domain_performance.advertiser_id = old_campaigns.advertiser_id
        and network_site_domain_performance.campaign_id = old_campaigns.campaign_id;

drop table old_campaigns;

optimize table bidhistory;
optimize table campaignsettings;
optimize table changelog;
optimize table dailyimpressionbudgethistory;
optimize table events;
optimize table high_water_mark;
optimize table historicaldata;
optimize table observeddata;
optimize table network_advertiser_frequency;
optimize table network_site_domain_performance;



  