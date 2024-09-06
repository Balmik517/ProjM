<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
 <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>   
<!DOCTYPE html>
<html>
<head>
<style>
@import url("Sample.css"); 
</style>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<body>

<table border="1">
<tr align="center">
<th colspan="5"><h1>Trainees Details</h1></th></tr>
<tr>
<th>User Name </th>
<th>Email Id</th>
<th>Date Of Birth</th>
<th>Mobile</th>
<th>Address</th>
</tr>
<c:forEach items="${listKey }" var="user" >
<tr>
<td>${user.userName }</td>
<td>${user.email }</td>
<td>${user.dob }</td>
<td>${user.mobile}</td>
<td>${user.address}</td>
</tr>
</c:forEach>

</table>
</body>
</html>