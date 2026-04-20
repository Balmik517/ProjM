package com.pma.spring.web.service;

import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Legacy email service using javax.mail directly.
 * javax.mail was replaced by jakarta.mail in newer versions.
 * Uses javax.activation (removed from JDK 11).
 */
@Service
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class);

    @Value("${mail.smtp.host:smtp.example.com}")
    private String smtpHost;

    @Value("${mail.smtp.port:587}")
    private int smtpPort;

    @Value("${mail.smtp.username:admin@example.com}")
    private String smtpUsername;

    @Value("${mail.smtp.password:password}")
    private String smtpPassword;

    @Value("${mail.from:noreply@example.com}")
    private String fromAddress;

    private Session mailSession;

    @PostConstruct
    public void initMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", smtpHost);
        props.put("mail.debug", "true");

        mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        logger.info("javax.mail Session initialized for SMTP: " + smtpHost);
    }

    /**
     * Send a plain text email using javax.mail.
     */
    public boolean sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            message.setSentDate(new Date());

            Transport.send(message);
            logger.info("Email sent to: " + to);
            return true;
        } catch (MessagingException e) {
            logger.error("Failed to send email to: " + to, e);
            return false;
        }
    }

    /**
     * Send HTML email with optional file attachment using javax.activation.
     */
    public boolean sendHtmlEmailWithAttachment(String to, String subject, String htmlBody, String attachmentPath) {
        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setSentDate(new Date());

            Multipart multipart = new MimeMultipart();

            // HTML body part
            BodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // File attachment using javax.activation (removed in JDK 11)
            if (attachmentPath != null) {
                BodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(attachmentPath);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(attachmentPath);
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);
            Transport.send(message);
            logger.info("HTML email with attachment sent to: " + to);
            return true;
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email to: " + to, e);
            return false;
        }
    }

    /**
     * Send registration notification email.
     */
    public void sendRegistrationEmail(String userName, String email) {
        String subject = "Welcome to Project Management App";
        String body = "<html><body>"
                + "<h2>Welcome, " + userName + "!</h2>"
                + "<p>Your account has been created successfully.</p>"
                + "<p>Login at: http://localhost:8086/loginForm</p>"
                + "</body></html>";

        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");
            message.setSentDate(new Date());

            Transport.send(message);
            logger.info("Registration email sent to: " + email);
        } catch (MessagingException e) {
            logger.error("Failed to send registration email", e);
        }
    }
}
