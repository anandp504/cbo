<%@ page import="java.util.*" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<ResultSuccess>
    <success>
        <code>100</code>
        <message><s:property value="message" escape="false" /></message>
    </success>
    <operation><s:property value="operation" escape="false" /></operation>
    <id><s:property value="id" escape="false" /></id>
    <s:property value="dataXml" escape="false" />
</ResultSuccess>
