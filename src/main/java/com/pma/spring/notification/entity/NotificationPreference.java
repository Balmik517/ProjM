package com.pma.spring.notification.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Per-user, per-channel opt-in/opt-out for notifications.
 *
 * {@code userId} refers to {@link com.pma.spring.web.entity.UserRegister}.
 * This table has exactly one reader today -
 * {@link com.pma.spring.notification.service.NotificationDigestService} - but
 * is designed to be read by the original
 * {@link com.pma.spring.web.service.NotificationService} too, which does not
 * happen yet.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "enabled")
    private boolean enabled;

    public NotificationPreference() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
