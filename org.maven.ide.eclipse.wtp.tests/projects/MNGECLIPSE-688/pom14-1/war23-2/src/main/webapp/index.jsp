<%@page import="org.apache.commons.lang.time.DateFormatUtils"%>
<%@page import="java.util.Date"%>
<html>
<body>
<%=DateFormatUtils.format(new Date(), "dd/MM/yyyy hh:mm:ss") %><br/>
It's more interesting <a href="/war24-1">here</a>
</body>
</html>