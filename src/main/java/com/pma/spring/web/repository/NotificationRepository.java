package com.pma.spring.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByUserId(int userId);

    List<Notification> findByProjectId(int projectId);

    List<Notification> findByStatus(String status);

    List<Notification> findByType(String type);

    long countByStatus(String status);
}
