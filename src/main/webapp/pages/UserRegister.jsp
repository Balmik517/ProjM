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
<link href="https://fonts.googleapis.com/css2?family=Gudea&display=swap" rel="stylesheet">

<style>
@import url("UserReg.css");
body{
background-image:url("laptop.jpg");
font-family: "Gudea",sans-serif;
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

	<div id="main-body" style="padding-top: 10%;background-repeat: no-repeat;
    background-size: cover;color:white;background-color: rgba(0, 0, 0, 0.616);" >
		
		<form class="form" action="registerForm" method="post"
			onsubmit="return validateRegister()" style="width: 50%;
      padding: 20px 40px; margin-left:10%;">
			<h4 align='center'>
				<strong style="color:white;">User Registration Form</strong>
			</h4><hr>
			<div class="form-group">
			<i class="fa fa-user fa-lg me-3 fa-fw"></i>
				<label>Name :</label> <input type="text" id="name" name="name"
					minlength="3" maxlength="30" required="required"><br>
			</div>
			<!-- 
			<div class="form-group">
				<label>Father's Name :</label> <input type="text" name="fathersName"><br>
			</div>
			 -->
			<div class="form-group">
			<i class="fa fa-calendar fa-lg me-3 fa-fw"></i>
				<label>Date of Birth :</label> <input type="date" id="dob"
					name="dob" required="required"><br>
			</div>
			<div class="form-group">
			<i class="fa fa-address-card fa-lg me-3 fa-fw"></i>
				<label>Address :</label>
				<textarea name="address" id="" cols="30" rows="5"></textarea>
			</div>
			<br> 
			<div class="form-group">
			<i class="fa fa-phone fa-lg me-3 fa-fw"></i>
				<label>Phone :</label> <input type="tel" id="phoneNumber"
					name="phoneNumber" pattern="[789][0-9]{9}" required="required"><br>
			</div>
			<div class="form-group">
			<i class="fa fa-envelope fa-lg me-3 fa-fw"></i>
				<label>Email :</label> <input type="email" id="email" name="email"
					required="required"><br>
				<div id="emailError"></div>
			</div>

			<div class="form-group">
			<i class="fa fa-lock fa-lg me-3 fa-fw"></i>
				<label>Password :</label> <input type="password" id="password"
					name="password" required="required"><br>
				<div id="message" style="float: right; color: red; font-size: 12px;"></div>
			</div>
			<!-- 
			<div class="form-group">
				<label>Confirm Password :</label> <input type="Password" id="confirmPassword"
					name="confirmPassword" required="required"><br>
					<div id="confirmPwdError" style="float: right;"></div>
			</div>
			 -->
			
			<div class="form-group">
				<button type="reset" class="btn btn-primary">Reset</button>
				<button type="submit" class="btn btn-success">Submit</button>
			</div>
			
		</form>
		<br> <br>

	</div>
	<script src="userRegister.js"></script>
</body>

</html>