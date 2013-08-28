<%@ page import="java.util.*" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<ResultError>
    <error>
        <id>300</id>
        <type>Validation Error</type>
        <message><s:property value="errorMessage" escape="false" /></message>
    </error>
</ResultError>
