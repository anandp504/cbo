<%@ taglib prefix="s" uri="/struts-tags" %>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="shortcut icon" href="images/favicon.ico" />
<title>Collective Bid Optimizer</title>
<script type="text/javascript" src="/cbo/assets/cbo.js"></script>
<script language="JavaScript" type="text/javascript">
  
</script>
</head>
<body background="images/bg_login.png">
<table cellspacing="0" cellpadding="0" width="100%" height="100%">
    <tr>
        <td align="center">
        <table cellspacing="0" cellpadding="0" width="300" height="50">
            <tr>
                <td background="images/table_header_bg.gif">
                    <font size="3" color="#FAFBED"><B>&nbsp;&nbsp;Collective&nbsp;Bid&nbsp;Optimizer</B></font>
                </td>
            </tr>
            <tr>
                <td>
                <table cellspacing="0" cellpadding="0" width="100%" height="50">
                    <td background="images/bl.gif" width="1"></td>
                    <td>
                    <table bgcolor="FFFFFF"  id="LoginBody" border="0" width="100%" height="150">
                        <tr>
                            <td align="center" valign="center">                           
                            
                            <form method="POST" action="login.action">
                            	<table border="0" cellspacing="5" width="100%">
                            		<tr>
                            			<td align="right">Username:</td>
                            			<td><input type="text" name="username" size="20" 
                            			           onkeydown="javascript:submitFormOnEnter('login.action', event);"/></td>
                            		</tr>
                            		<tr>
                            			<td align="right">Password:</td>
                            			<td><input type="password" name="password" size="20"
                            			           onkeydown="javascript:submitFormOnEnter('login.action', event);"/></td>
                            		</tr>
                            		<tr>
                            			<td colspan="2" align="center"><input type="submit" value="Login"/></td>
                            		</tr>
                            	</table>
                            </form>
                            </td>
                        </tr>
                    </table>
                    </td>
                    <td background="images/bl.gif" width="1"></td>
                </table>
                </td>
            </tr>
            <tr>
                <td valign="top">
                <table cellspacing="0" cellpadding="0" width="100%">
                    <td align="left" valign="top"><img
                        src="images/cfl.gif"></td>
                    <td background="images/cf.gif" width="100%"></td>
                    <td align="right" valign="top"><img
                        src="images/cfr.gif"></td>
                </table>
                </td>
            </tr>
        </table>
        <script type="text/javascript">
           document.forms[0].username.focus();
        </script>
</body>
</html>

