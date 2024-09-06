<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>

<head>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Gudea&display=swap"
	rel="stylesheet">
<style>
@import url("updateProject.css");

body {
	background-image: url("laptop.jpg");
}
</style>
<link rel="stylesheet" href="/updateProject/style.css">
<link rel="stylesheet" type="text/css"
	href="https://stackpath.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css"
	integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3"
	crossorigin="anonymous">
<title>ProJeM</title>
</head>

<body>



	<div>
		<nav id="navbar">
			<div id="nav1">
				<h2>
					<i style="font-size: 40px;" class="fa fa-laptop"></i>&nbsp ProJeM
				</h2>
			</div>
			<div id="list">
				<ul>
					<li><a href="home" class="btn-1"><span><i
								class="fa fa-home"></i></span>Home</a></li>
					<li><a href="loginForm" class="btn-1"><span><i
								class="fa fa-user"></i></span>Login</a></li>
					<li><a href="registerForm" class="btn-1"><span><i
								class="fa fa-users"></i></span>Register</a></li>
					<li><a href="FAQ" class="btn-1"><span><i
								class="fa fa-question-circle"></i></span>FAQs</a></li>
					<li><a class="btn-1"><span><i
								style="font-size: 22px;" class="fa fa-bars"></i></span></a>
						<div class="sub-menu-1">
							<ul>
								<li><a href="about">About Us</a></li>
								<li><a href="contact">Contact Us</a></li>
								<li class="hover-me"><span><i
										class="fa fa-angle-right"></i> <a>Users</a>
										<div class="sub-menu-2">
											<ul>
												<li><a href="find">Find a User</a></li>
												<li><a href="displayAll">Users List</a></li>
											</ul>
										</div></li>
								<li class="hover-me"><span><i
										class="fa fa-angle-right"></i> <a>Projects</a>
										<div class="sub-menu-2">
											<ul>
												<li><a href="projectUpload">Add Project</a></li>
												<!-- <li><a href="#">Find a Project</a></li> -->
												<li><a href="projectList">Projects List</a></li>
											</ul>
										</div></li>
							</ul>

						</div></li>

				</ul>
			</div>
		</nav>
	</div>

	<div id="main-body">
		<form class="form" action="updateprojectData" method="post"
			style="width: 50%; padding: 20px 40px; margin-left: 10%;">
			<h4 align='center'>
				<strong style="color: white;">Upload Project here.</strong>
			</h4>
			<hr>

			<div class="form-group">
				<i class="fa fa-tasks fa-lg me-3 fa-fw"></i> <label>Project
					id :</label> <input type="text" id="projectId" name="projectId"
					value="${userKey.projectId}" readonly="readonly" minlength="3"
					maxlength="30" required="required" style="color:gray;"><br>
			</div>
			<div class="form-group">
				<i class="fa fa-tasks fa-lg me-3 fa-fw"></i> <label>Project
					Name :</label> <input type="text" id="projectName" name="projectName"
					value="${userKey.projectName}" minlength="3" maxlength="30"
					required="required" readonly="readonly" style="color:gray;"><br>
			</div>
			<div class="form-group">
				<i class="fa fa-calendar fa-lg me-3 fa-fw"></i> <label>Start
					Date :</label> <input type="date" id="startDate" name="startDate"
					value="${userKey.startDate}" required="required"><br>
			</div>
			<div class="form-group">
				<i class="fa fa-calendar fa-lg me-3 fa-fw"></i> <label>End
					Date :</label> <input type="date" id="endDate" name="endDate"
					value="${userKey.endDate}" required="required"><br>
			</div>


			<div class="form-group">
				<button type="reset" class="btn btn-primary">Reset</button>
				<button type="submit" class="btn btn-success">Submit</button>
			</div>
		</form>

		<div id="project-update-msg">
			<b>${updatemessage}</b>
		</div>
	</div>

</body>

</html>