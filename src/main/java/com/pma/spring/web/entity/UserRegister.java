package com.pma.spring.web.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
//import javax.persistence.Transient;

import org.springframework.format.annotation.DateTimeFormat;


@Entity
@Table
public class UserRegister{
	
	@Id
	@Column
	@GeneratedValue
	private int id;
	@Column(length=50)
	private String name;
//	@Column(length=50)
//	private String fathersName;
	@Temporal(value = TemporalType.DATE)
	@DateTimeFormat(pattern="yyyy-MM-dd")
	Date dob;
	@Column(length=100)
	private String address;
	@Column(length=10)
	private String phoneNumber;
	@Column(length=40)
	private String email;
	@Column(length=20)
	private String password;
//	@Column(length=20)
//	@Transient
//	private String confirmPassword;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
//	public String getFathersName() {
//		return fathersName;
//	}
//	public void setFathersName(String fathersName) {
//		this.fathersName = fathersName;
//	}
	public Date getDob() {
		return dob;
	}
	public void setDob(Date dob) {
		this.dob = dob;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
//	public String getConfirmPassword() {
//		return confirmPassword;
//	}
//	public void setConfirmPassword(String confirmPassword) {
//		this.confirmPassword = confirmPassword;
//	}
	
	
	public UserRegister() {
		super();
		// TODO Auto-generated constructor stub
	}
	public UserRegister(int id, String name, Date dob, String address, String phoneNumber,
			String email, String password) {
		super();
		this.id = id;
		this.name = name;
//		this.fathersName = fathersName;
		this.dob = dob;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.password = password;
//		this.confirmPassword = confirmPassword;
	}
	
	@Override
	public String toString() {
		return "UserRegister [id=" + id + ", name=" + name + ", dob=" + dob
				+ ", address=" + address + ", phoneNumber=" + phoneNumber + ", email=" + email + ", password="
				+ password + "]";
	}
	
	
		
		
}