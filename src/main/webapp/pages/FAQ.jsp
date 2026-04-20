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

<link rel="stylesheet" type="text/css"
	href="https://stackpath.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css"
	integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3"
	crossorigin="anonymous">
<style>
@import url("FAQ.css");
body {
	background-image: url("laptop.jpg");
	font-family: "Gudea", sans-serif;
	
}
</style>
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
                                           <!--  <li><a href="#">Find a Project</a></li> -->
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

	<main class="container">
		<h3 class="faq-heading">FAQ'S</h3>
		<section class="faq-container">
			<div class="faq-one">
				<!-- faq question -->
				<h5 class="faq-page">What is Project Management System?</h5>
				<!-- faq answer -->
				<div class="faq-body">
					<p>It's a tool to help you coordinate team tasks so everyone
						knows who's doing what. Share files, and status updates. Plus, get
						a complete view of work so teams tackle the right tasks at the
						right time.</p>
				</div>
			</div>
			<hr class="hr-line" />
			<div class="faq-two">
				<!-- faq question -->
				<h5 class="faq-page">Why do you need it?</h5>
				<!-- faq answer -->
				<div class="faq-body">
					<p>Managing projects is hard work. Even harder if you don't use
						software or have work spread across too many tools. Use one tool
						to organize projects, make communication easy, and meet deadlines.</p>
				</div>
			</div>
			<hr class="hr-line" />
			<div class="faq-three">
				<!-- faq question -->
				<h5 class="faq-page">What is included in this application?</h5>
				<!-- faq answer -->
				<div class="faq-body">
					<p>In this application, user can register, login, upload a
						project, delete a project, see the project lists, modify a
						project's details etc.</p>
				</div>
			</div>
		</section>
	</main>
	<script src="FAQ.js"></script>
</body>

</html>