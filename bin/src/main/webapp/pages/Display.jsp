<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
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
	Welcome ${nameKey}


	<table>

		<tr>
			<td>UserName</td>
			<td>EmailId</td>
			<td>Date Of Birth</td>
			<td>MobileNo</td>
			<td>Address</td>
		</tr>
		<tr>
			<td>${loginKey.userName}</td>
			<td>${loginKey.email}</td>
			<td>${loginKey.dob}</td>
			<td>${loginKey.mobile}</td>
			<td>${loginKey.address}</td>
		</tr>
	</table>
</body>
</html>