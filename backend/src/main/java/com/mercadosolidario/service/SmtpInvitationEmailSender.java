package com.mercadosolidario.service;

import com.mercadosolidario.exception.InvitationEmailUnavailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpInvitationEmailSender implements InvitationEmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Override
    public void sendPasswordSetupLink(String email, String fullName, String setupLink) {
        send(email, "Defina sua senha - Mercado Solidário", """
                Olá, %s.

                Sua conta do Mercado Solidário foi criada. Para definir sua senha, acesse o link abaixo:
                %s

                O link é individual, pode ser usado uma única vez e expira em 24 horas.
                Se você não esperava este convite, ignore esta mensagem.
                """.formatted(fullName, setupLink));
    }

    @Override
    public void sendPasswordChangeLink(String email, String fullName, String changeLink, int validMinutes) {
        send(email, "Troca de senha do primeiro acesso - Mercado Solidário", """
                Olá, %s.

                Recebemos um acesso à sua conta do Mercado Solidário com a senha temporária.
                Para continuar, defina sua nova senha (mínimo de 8 caracteres) pelo link abaixo:
                %s

                O link é individual, pode ser usado uma única vez e expira em %d minutos.
                Se não foi você, ignore esta mensagem: sua conta continua bloqueada até a troca.
                """.formatted(fullName, changeLink, validMinutes));
    }

    @Override
    public void sendWithCopy(String to, String cc, String subject, String text) {
        sendInternal(to, cc, subject, text);
    }

    @Override
    public void sendToMany(String[] to, String subject, String text) {
        sendInternalMany(to, subject, text);
    }

    private void sendInternalMany(String[] to, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null || fromAddress.isBlank() || smtpPassword.isBlank()) {
            throw new InvitationEmailUnavailableException("Envio de e-mail não configurado.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            log.error("E-mail could not be delivered (subject: {}).", subject, exception);
            throw new InvitationEmailUnavailableException("Não foi possível enviar o e-mail. Verifique a configuração SMTP.");
        }
    }

    @Override
    public void sendWithAttachment(String to, String subject, String text,
                                   String attachmentName, byte[] attachment, String contentType) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null || fromAddress.isBlank() || smtpPassword.isBlank()) {
            throw new InvitationEmailUnavailableException("Envio de e-mail não configurado.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            if (attachment != null && attachmentName != null) {
                helper.addAttachment(attachmentName,
                    new org.springframework.core.io.ByteArrayResource(attachment),
                    contentType != null ? contentType : "application/octet-stream");
            }
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            log.error("E-mail could not be delivered (subject: {}).", subject, exception);
            throw new InvitationEmailUnavailableException("Não foi possível enviar o e-mail. Verifique a configuração SMTP.");
        }
    }

    private void send(String to, String subject, String text) {
        sendInternal(to, null, subject, text);
    }

    private void sendInternal(String to, String cc, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null || fromAddress.isBlank() || smtpPassword.isBlank()) {
            throw new InvitationEmailUnavailableException("Envio de e-mail não configurado.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            if (cc != null && !cc.isBlank() && !cc.equalsIgnoreCase(to)) {
                helper.setCc(cc);
            }
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            log.error("E-mail could not be delivered (subject: {}).", subject, exception);
            throw new InvitationEmailUnavailableException("Não foi possível enviar o e-mail. Verifique a configuração SMTP.");
        }
    }
}
