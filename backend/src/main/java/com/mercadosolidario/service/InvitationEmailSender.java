package com.mercadosolidario.service;

public interface InvitationEmailSender {
    void sendPasswordSetupLink(String email, String fullName, String setupLink);

    /** Link individual de troca de senha do primeiro acesso. Lança InvitationEmailUnavailableException se não enviar. */
    void sendPasswordChangeLink(String email, String fullName, String changeLink, int validMinutes);

    /** E-mail genérico com cópia (ex.: aviso de tarefa). Lança InvitationEmailUnavailableException se não enviar. */
    void sendWithCopy(String to, String cc, String subject, String text);

    /** E-mail para vários destinatários (ex.: aviso à equipe). Lança InvitationEmailUnavailableException se não enviar. */
    void sendToMany(String[] to, String subject, String text);

    /** E-mail com anexo opcional (ex.: comprovante de caixa). Lança InvitationEmailUnavailableException se não enviar. */
    void sendWithAttachment(String to, String subject, String text,
                            String attachmentName, byte[] attachment, String contentType);
}
