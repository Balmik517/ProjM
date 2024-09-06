package com.pma.spring.web.controller;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
//import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.service.ProjectServiceImpl;
import com.pma.spring.web.service.UserServiceImpl;

@Controller

public class HomeController {


	@Autowired
	UserServiceImpl userService;

	@Autowired
	ProjectServiceImpl projectService;
	
	

	// -------------- Home Page ------------------

	@RequestMapping("home")
	public String open() 
	{
		return "Home";
	}

	
	// --------------Register Display page -----------------

	@GetMapping("registerForm")
	public String register() 
	{
		return "UserRegister";
	}

	
	// ------------- Register page Fetching ------------------

	@PostMapping("registerForm")
	public ModelAndView saveUser(UserRegister user) 
	{
		System.out.println(user);

		if (userService.save(user) != null) 
		{
			return new ModelAndView("UserLogin", "message", "User Registered Successfully.");
		}
		return null;

	}

	
	// ----------------Login Display Page ----------

	@GetMapping("loginForm")
	public String login() 
	{
		return "UserLogin";
	}

	
	// --------------- Login Page ----------------

	@PostMapping("loginForm")
	public ModelAndView authenticate(String email, String password) 
	{
		UserRegister user = userService.validate(email, password);

		if (user != null) 
		{
			return new ModelAndView("Home", "nameKey", email);
		} 
		else 
		{
			return new ModelAndView("UserLogin", "errorKey", "Please enter valid Email and Password!");
		}
	}

	
	// -------------- Contact Us Page --------------------

	@RequestMapping("contact")
	public String contact() 
	{
		return "ContactUs";
	}

	
	// ---------------- About Us ------------------

	@RequestMapping("about")
	public String about() 
	{
		return "AboutUs";
	}

	
	// -------------- FindUser Get the Page --------
	@GetMapping("find")
	public String findUser() 
	{
		return "Find";
	}

	
// ------------------ Find user View Page -----------------

	@PostMapping("findUser")
	public ModelAndView findByName(String name) 
	{

		ModelAndView modelAndView = null;
		try 
		{
			UserRegister user = userService.findByName(name);

			if (user != null) 
			{
				modelAndView = new ModelAndView("UserProfile", "loginKey", user);
			}
			else
			{
				modelAndView = new ModelAndView("Find", "findMessage", "Please enter a valid user to find !!!");
			}
		} 
		catch (EntityNotFoundException e) 
		{
			modelAndView = new ModelAndView("UserLogin", "ek", "please enter valid userid to find");
		}
		return modelAndView;
	}

	
	// ----------------------- View All Users Page view -----------------

	@GetMapping("displayAll")
	ModelAndView viewAllUsers() 
	{

		ModelAndView modelAndView = null;
		try 
		{
			List<UserRegister> listUser = userService.findAll();

			if (!listUser.isEmpty())
			{
				modelAndView = new ModelAndView("ViewAll", "listKey", listUser);
			}
		} 
		catch (EntityNotFoundException e) 
		{
			modelAndView = new ModelAndView("Find", "ek", "please enter valid userid to find");
		}
		return modelAndView;
	}

	
	// -------------------- FAQ page ------------------------

	@RequestMapping("FAQ")
	public String faq() 
	{
		return "FAQ";
	}

	
//	----------------- Delete User ------------------

	@RequestMapping("/deleteuser/{id}")
	public String deleteUser(@PathVariable(value = "id") int id) 
	{
		this.userService.deleteById(id);
		return "redirect:/displayAll";
	}

	
//	-------------------- Project Upload ---------------

	@GetMapping("projectUpload")
	public String projectUploadView() 
	{
		return "UploadProject";
	}

	
//	------------------ Save Project --------------------

	@PostMapping("projectUpload")
	public ModelAndView projectUpload(ProjectRegister project) 
	{
		System.out.println(project);

		if (projectService.save(project) != null) 
		{
			return new ModelAndView("UploadProject");
		}
		return null;
	}

	
//	------------------ Display list of Projects -------------------

	@GetMapping("projectList")
	ModelAndView viewAllProjects() 
	{

		ModelAndView modelAndView = null;
		try {
			List<ProjectRegister> listProjects = projectService.findAll();

			if (!listProjects.isEmpty()) 
			{
				modelAndView = new ModelAndView("ViewAllProjects", "listProjectKey", listProjects);
			}
		} 
		catch (EntityNotFoundException e) 
		{
			modelAndView = new ModelAndView("UploadProject");
		}
		return modelAndView;
	}

	
//	--------------- Update user view ------------------------

	@RequestMapping("/updateUser")

	public ModelAndView updateUser(@RequestParam(value = "id") int id) 
	{

		UserRegister user = userService.updateUser(id);
		if (user != null)
			return new ModelAndView("UpdateUser", "userKey", user);
		return null;
	}

	
//   ------------------------ Update User ---------------------

	@RequestMapping(value = "updateUserData", method = RequestMethod.POST)

	public ModelAndView updateUserData(UserRegister user) 
	{

		ModelAndView modelAndView = null;
		if (userService.save(user) != null)
			modelAndView = new ModelAndView("UpdateUser", "updatemessage", "User data updated");
		return modelAndView;
	}

	
//	------------------------- Update Project view page -------------------------

	@RequestMapping("/updateproject")

	public ModelAndView updateProject(@RequestParam(value = "id") int id) {

		ProjectRegister projectRegister = projectService.updateProject(id);
		if (projectRegister != null)
			return new ModelAndView("UpdateProject", "userKey", projectRegister);
		return null;

	}
	

// ------------------------ Update Project ---------------------

	@RequestMapping(value = "updateprojectData", method = RequestMethod.POST)

	public ModelAndView updateProjectData(ProjectRegister projectRegister) {
		ModelAndView modelAndView = null;
		if (projectService.save(projectRegister) != null)
			modelAndView = new ModelAndView("UpdateProject", "updatemessage", "project data updated");
		return modelAndView;
	}
	

//	  ----------------------- Delete Project -------------------

	@RequestMapping("/deleteProject/{id}")
	public String deleteProject(@PathVariable(value = "id") int id) {
		this.projectService.deleteById(id);
		return "redirect:/projectList";
	}



}