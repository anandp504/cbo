// Use this for DB based authentication
//TIPLogin
//{
//    com.tumri.cbo.security.auth.DBLoginModule required 
//    debug="true" 
//    url="this is test url" 
//    driver="com.mysql.jdbc.Driver";
//};

// Use this login configuration for file based login
//TIPLogin
//{
//	com.tagish.auth.FileLogin required debug=true pwdFile="/opt/p4/depot/Tumri/tas/tip/config/passwd";
//};

// Use this login configuration for LDAP based login
TIPLogin
{
    com.tumri.cbo.security.auth.LDAPLoginModule required
    debug=true
    strongDebug=true
    tryFirstPass=false
    useFirstPass=false
    storePass=true
    user.provider.url="ldap://ldap.dc1.tumri.net/dc=tumri,dc=net"
    group.provider.url="ldap://ldap.dc1.tumri.net/dc=tumri,dc=net"
    useSharedState=true;
};
