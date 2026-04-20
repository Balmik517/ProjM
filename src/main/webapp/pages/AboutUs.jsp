<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Gudea&display=swap"
	rel="stylesheet">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link
	href="https://fonts.googleapis.com/css2?family=Tangerine:wght@700&display=swap"
	rel="stylesheet">
<style>
@import url("about.css");

body {
	background-image: url("laptop.jpg");
	font-family: "Gudea", sans-serif;
}
</style>
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

	<div id="main-body" class="container"
		style="background-repeat: no-repeat; background-size: cover; color: white; background-color: rgba(0, 0, 0, 0.616);">
		<div id="div-1">
			<h3>What is Project Management App?</h3>
			<hr>
			<p>A project management app is a means of tracking a project
				details in a single place. The important informations of every
				projects will be listed out here. It can help one to track all the
				project names, name of members involved, start and end date for all.
			</p>
		</div>
		<div id="div-2">
			<h3>What is included in a project management report?</h3>
			<hr>
			<p>Project management reports should include the following key
				characteristics no matter which business, industry, or even type of
				project the report is for:</p>
		</div>
		<div id="div-3">
			<h4>All the identifying project information:</h4>
			<hr>
			<p>
				1. The project name <br> 2. Name of the project members <br>
				3. Start date of the project <br> 4. Expected end date <br>
			</p>
		</div>

	</div>
</body>
</html>