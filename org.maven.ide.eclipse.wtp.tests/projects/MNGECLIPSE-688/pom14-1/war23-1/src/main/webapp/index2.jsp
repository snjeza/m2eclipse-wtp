<html>
<body>
<form action="HelloServlet">
Enter yout name : <input type="text" name="name">
<input  type="submit" value="greet!">  
</form>
<% 
String message = (String)request.getAttribute("message");
if (message != null){
%>
<h2><%=message%></h2>
<%}%>
</body>
</html>
