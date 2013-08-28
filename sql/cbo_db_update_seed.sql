-- DON'T DELETE THIS IMPORTANT NOTE - Use $$ instead of SEMICOLON as DELIMITER in this file
--
-- This is used to update the table data in the database.
-- It is called update_seed rather than update for historical reasons.
-- It always should contain the code to do the latest update to the database.
--
-- This file currently contains the update from 12 to 13.
-- The index changes on observeddata table and remove partition on
-- changelog, events, historicaldata and network_site_domain_performance
-- are removed as part of the DB upgrade to version 13

USE CBO_DB$$




