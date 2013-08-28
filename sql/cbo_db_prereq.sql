-- This script should be run only once per database installation. Otherwise errors will be thrown due to
-- existence of duplicate users.

create database if not exists CBO_DB;
drop user CBOUSER;
create user CBOUSER identified by 'w3lc0m31';
grant SELECT on CBO_DB.* TO HC_USER identified by 'w3lc0m31';
--grant all on CBO_DB.* TO CBOUSER identified by 'w3lc0m31';
grant SELECT,INSERT,UPDATE,DELETE,EXECUTE on CBO_DB.* to 'CBOUSER'@'%' identified by 'w3lc0m31';
grant FILE on *.* to 'CBOUSER'@'%' identified by 'w3lc0m31';

GRANT SELECT ON mysql.* TO CBOUSER identified by 'w3lc0m31';
GRANT SELECT ON mysql.* TO CBOUSER@'localhost' identified by 'w3lc0m31';
