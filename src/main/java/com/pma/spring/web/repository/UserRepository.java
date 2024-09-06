package com.pma.spring.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.UserRegister;

@Repository
public interface UserRepository extends JpaRepository<UserRegister, Integer> {

	// custom automation
	
	Optional<com.pma.spring.web.entity.UserRegister> findByName(String name);
		
	Optional<com.pma.spring.web.entity.UserRegister> findByEmailAndPassword(String email,String password);
	
}