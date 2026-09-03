package com.pma.spring.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.notification.entity.NotificationPreference;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Integer> {

    List<NotificationPreference> findByUserId(int userId);

    Optional<NotificationPreference> findByUserIdAndChannel(int userId, String channel);
}
