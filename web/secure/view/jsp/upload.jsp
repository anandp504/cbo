<%@ taglib prefix="s" uri="/struts-tags" %>
<html>
<head>
<title>Upload page</title>
</head>
<body>

<s:if test="{#errorMessage} != 'null'}">
Error: <s:property value="errorMessage" escape="false"/>
</s:if>

<!--
<script language="JavaScript" type="text/javascript">

</script>
-->



<h3>Upload new bid instructions spreadsheet:</h3>
<form method="POST" ENCTYPE="multipart/form-data" action="/cbo/secure/services/setCampaignSummary.action">
Spreadsheet:&nbsp;<input type='file' name='spreadsheet' width="50" value='"Browse"' method=post />
<input type="Submit" name="SubmitAction" value="Upload">
</form> 
</body>
</html>
