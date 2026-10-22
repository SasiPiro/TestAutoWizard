package com.autowizard.reminder.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Emails a user when one of their reminders is created or changes.
 *
 * <p>
 * Called after the reminders are committed: an SMTP failure must not roll them back, and a
 * reminder is not emailed unless it exists. A failed email is logged and skipped; the
 * reminder is still in the app. Off unless {@code app.mail.enabled=true} (MAIL_ENABLED), so
 * a local run never tries to reach an SMTP server.
 * </p>
 */
@Slf4j
@Component
public class ReminderMailer {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public ReminderMailer(JavaMailSender mailSender,
                          @Value("${app.mail.enabled:false}") boolean enabled,
                          @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void send(List<ReminderService.ReminderNotice> notices) {
        if (!enabled) {
            log.debug("[REMINDER_EMAIL] outcome=skipped reason=mail_disabled count={}", notices.size());
            return;
        }
        for (ReminderService.ReminderNotice notice : notices) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(notice.email());
            message.setSubject("AutoWizard: " + notice.title());
            message.setText(notice.message() + ".\n\nApri AutoWizard per vedere i dettagli.");
            try {
                mailSender.send(message);
                log.info("[REMINDER_EMAIL] outcome=success reminderId={} userId={}", notice.reminderId(), notice.userId());
            } catch (MailException e) {
                log.warn("[REMINDER_EMAIL] outcome=failure reminderId={} userId={} reason={}",
                        notice.reminderId(), notice.userId(), e.getClass().getSimpleName());
            }
        }
    }
}
