package com.pma.spring.web.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table
public class ProjectRegister {
	
	@Id
	@GeneratedValue
	private int projectId;
	
	@Column(length = 30)
	private String projectName;
	
	@Temporal(value = TemporalType.DATE)
	@DateTimeFormat(pattern="yyyy-MM-dd")
	Date startDate;
	
	@Temporal(value = TemporalType.DATE)
	@DateTimeFormat(pattern="yyyy-MM-dd")
	Date endDate;

	/**
	 * Lifecycle state of the project. Written by the project component, the
	 * project-management facade and the completion workflow, and read by
	 * billing, reporting, notification and integration code paths - which is
	 * what makes ownership of this row ambiguous.
	 */
	@Column(length = 20)
	private String status;

	@Temporal(value = TemporalType.TIMESTAMP)
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date completedAt;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Date completedAt) {
		this.completedAt = completedAt;
	}

	public int getProjectId() {
		return projectId;
	}

	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public ProjectRegister() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ProjectRegister(int projectId, String projectName, Date startDate, Date endDate) {
		super();
		this.projectId = projectId;
		this.projectName = projectName;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	@Override
	public String toString() {
		return "ProjectRegister [projectId=" + projectId + ", projectName=" + projectName + ", startDate=" + startDate
				+ ", endDate=" + endDate + "]";
	}
	
	
	

}