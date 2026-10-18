package com.mercadosolidario.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class InvitationMailConfiguration {

    @Bean
    @ConditionalOnExpression("'${SMTP_HOST:}'.trim().length() > 0")
    public JavaMailSender invitationJavaMailSender(Environment environment) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(environment.getRequiredProperty("SMTP_HOST"));
        sender.setPort(environment.getProperty("SMTP_PORT", Integer.class, 587));
        sender.setUsername(environment.getProperty("SMTP_USERNAME", ""));
        sender.setPassword(environment.getProperty("SMTP_PASSWORD", ""));

        Properties mailProperties = sender.getJavaMailProperties();
        boolean hasUsername = !sender.getUsername().isBlank();
        boolean hasPassword = !sender.getPassword().isBlank();
        if (hasUsername != hasPassword) {
            throw new IllegalStateException("SMTP_USERNAME and SMTP_PASSWORD must be configured together.");
        }
        mailProperties.put("mail.smtp.auth", Boolean.toString(hasUsername));
        mailProperties.put("mail.smtp.starttls.enable", environment.getProperty("SMTP_STARTTLS", "true"));
        mailProperties.put("mail.smtp.connectiontimeout", "5000");
        mailProperties.put("mail.smtp.timeout", "5000");
        mailProperties.put("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
