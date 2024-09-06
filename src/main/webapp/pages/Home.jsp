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
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link
	href="https://fonts.googleapis.com/css2?family=Tangerine:wght@700&display=swap"
	rel="stylesheet">
<style>
@import url("home.css");

body {
	background-image: url("laptop.jpg");
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


	<div
		style="background-repeat: no-repeat; background-size: cover; color: white; background-color: rgba(0, 0, 0, 0.616); text-align: center;"
		class="body-div container">
		<h1>Find Your Projects In One Go</h1>

		<div class="d-grid gap-2 col-4 mx-auto mt-5">
			<h6 style="margin-left:-260px;">Already have an account?</h6>
			<button class="btn" type="button" style="background:#89216B;color:#fff">
			<a style="color: white; text-decoration: none;" href="loginForm">Login</a></button>
			<h6 class="mt-3" style="margin-left:-270px;">Don't have an account?</h6>
			<button class="btn" type="button" style="background:#f12711;color:#fff">
			<a style="color: white; text-decoration: none;" href="registerForm">Sign Up</a></button>
		</div>
	</div>


</body>
</html>