<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
	<!DOCTYPE html>
	<html lang="en">

	<head>
		<meta charset="UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Gudea&display=swap" rel="stylesheet">
		
		<style>
			@import url("UserLogin.css");
			body {
				background-image: url("laptop.jpg");
			}
		</style>
		<link rel="stylesheet" type="text/css"
			href="https://stackpath.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css">
		<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css"
			integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3" crossorigin="anonymous">
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
                    <li><a href="home" class="btn-1"><span><i class="fa fa-home"></i></span>Home</a></li>
                    <li><a href="loginForm" class="btn-1"><span><i class="fa fa-user"></i></span>Login</a></li>
                    <li><a href="registerForm" class="btn-1"><span><i class="fa fa-users"></i></span>Register</a></li>
                    <li><a href="FAQ" class="btn-1"><span><i class="fa fa-question-circle"></i></span>FAQs</a></li>
                    <li>
                        <a class="btn-1"><span><i style="font-size: 22px;" class="fa fa-bars"></i></span></a>
                        <div class="sub-menu-1">
                            <ul>
                                <li><a href="about">About Us</a></li>
                                <li><a href="contact">Contact Us</a></li>
                                <li class="hover-me"><span><i class="fa fa-angle-right"></i>
                                    <a>Users</a>
                                    <div class="sub-menu-2">
                                        <ul>
                                            <li><a href="find">Find a User</a></li>
                                            <li><a href="displayAll">Users List</a></li>
                                        </ul>                                       
                                    </div>
                                </li>
                                <li class="hover-me"><span><i class="fa fa-angle-right"></i>
                                    <a>Projects</a>
                                    <div class="sub-menu-2">
                                        <ul>
                                            <li><a href="projectUpload">Add Project</a></li>
                                            <!-- <li><a href="#">Find a Project</a></li> -->
                                            <li><a href="projectList">Projects List</a></li>
                                        </ul>                                       
                                    </div>
                                </li>
                            </ul>

                        </div>
                    </li>

                </ul>
            </div>
        </nav>
    </div>

		<div class="container" id="main-body" style="padding-top: 15%;background-repeat: no-repeat;
    background-size: cover;color:white;background-color: rgba(0, 0, 0, 0.616);">

			<div>
				<form style="width:60%" class="form" action="loginForm" method="post" onsubmit="return validatelogin()">

					<h4 align='center'>
						<strong style="color: white">User Login Form</strong>
					</h4>
					<hr>
					<div class="form-group">
						<i class="fa fa-envelope fa-lg me-3 fa-fw"></i>
						<label>Email :</label> <input id="login-email" type="email" name="email"><br>
					</div>
					<div class="form-group">
						<i class="fa fa-lock fa-lg me-3 fa-fw"></i>
						<label>Password :</label> <input id="login-password" type="password" name="password"><br>
						<div id="message"></div>
					</div>
					<div class="form-group">
						<button type="submit" class="btn btn-primary" onclick="validateCred()">Sign In</button>
					</div>
				</form>
			</div>

			<br>
			<div style="margin-left: 20%;">
				<h6 id="registrationSuccessMessage">${message}</h6>
				<h6 id="loginErrMsg">${errorKey}</h6>
			</div>
			
		</div>




	</body>

	</html>