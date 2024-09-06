package com.pma.spring.web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService{

	

	@Autowired
	UserRepository userRepository;

	@Override
	public UserRegister save(UserRegister user) {
		UserRegister optionalSave = userRepository.save(user) ;
		return optionalSave;		

	}
	@Override
	public UserRegister findByName(String name) {
		// TODO Auto-generated method stub
		Optional<UserRegister> optional = userRepository.findByName(name);
		if(optional.isEmpty()) {
			return null;
		}
		return optional.get();
		//return loginRepository.;
	}


	@Override
	public UserRegister validate(String email, String password) {
		Optional<UserRegister> validation = userRepository.findByEmailAndPassword(email, password);
		if(validation.isEmpty()) {
			return null;
		}
		return validation.get();
		
	}
	
	@Override
	public List<UserRegister> findAll() {
		// TODO Auto-generated method stub		
		List<UserRegister> list = userRepository.findAll();		
		if(list.isEmpty()) {
			return null;
		}
		return list;
	}
	
	@Override
	public void deleteById(int id) {
		// TODO Auto-generated method stub
		userRepository.deleteById(id);
	}
	
	@Override public UserRegister updateUser(int id) { // TODO Auto-generated
		  
		  
		  Optional<UserRegister> optional= userRepository.findById(id);
		  
		  UserRegister user=null;
		  
		  if(optional.isPresent()) {
		  
		  user = optional.get(); 
		  }
		  else { 
			  throw new RuntimeException("User not found for id " +id); 
		  } 
		  return user; 
		  }
	
}