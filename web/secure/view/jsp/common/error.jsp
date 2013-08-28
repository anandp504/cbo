<%@ taglib prefix="s" uri="/struts-tags" %>
<error>
    <id>500</id>
    <type>Application Error</type>
    <message><s:property value="errorMessage" escape="false" /></message>
    <redirectUrl><s:property value="errorMessage" escape="false" /></redirectUrl>
</error>
