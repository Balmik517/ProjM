package com.pma.spring.web.service;

import java.util.List;

import com.pma.spring.web.entity.UserRegister;

public interface UserService {
	
	UserRegister save(UserRegister user);
		
	UserRegister findByName(String name); 
			
	UserRegister validate(String email, String password);
	
	List<UserRegister> findAll();
	
	void deleteById(int id);
	
	UserRegister updateUser(int  id);

}