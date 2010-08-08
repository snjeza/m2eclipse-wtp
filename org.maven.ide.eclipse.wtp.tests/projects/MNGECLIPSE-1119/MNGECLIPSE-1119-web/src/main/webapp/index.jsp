<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="foo.bar.MNGECLIPSE_1119.util.Version"%><html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>MNGECLIPSE-1119 <%=Version.VALUE%></title>
</head>
<body>
<form action="greeting">
<label for="name">What is your name?</label>
<input type="text" name="name" id="name" />
<input type="submit" name="go" value="go"/>
</form>
<hr/>
${requestScope.greeting}
</body>
</html>