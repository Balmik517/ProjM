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

	
	<form action="regForm" method="post">
		<table cellpadding="10" cellspacing="10" border=10 align="center"
			bgcolor="ctan">
			<tr>
				<td>userName</td>
				<td><input type="text" name="userName"></td>
			</tr>
			<tr>
				<td>password</td>
				<td><input type="password" name="password"></td>
			</tr>
			<tr>
				<td>DOB</td>
				<td><input type="Date" name="dob"></td>
			</tr>
			<tr>
				<td>EmailId</td>
				<td><input type="email" name="email"></td>
			</tr>
			<tr>
				<td>Mobile</td>
				<td><input type="text" name="mobile"></td>
			</tr>
			<tr>
				<td>Address</td>
				<td><input type="text" name="address"></td>
			</tr>
			<tr>
				<td colspan="2">"<input type="submit" value="click"></td>
			</tr>
		</table>
	</form>
</body>
</html>