package com.autowizard.controller;

import com.autowizard.dto.ReminderResponse;
import com.autowizard.security.CurrentUser;
import com.autowizard.security.UserPrincipal;
import com.autowizard.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
public class ReminderController {

    private final ReminderService reminderService;

    @PutMapping("/{id}/notify")
    public ResponseEntity<Void> markAsNotified(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Marking reminder {} as notified for user: {}", id, currentUser.getId());
        reminderService.markAsNotified(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate/pending")
    public ResponseEntity<Set<ReminderResponse>> generatePendingTaskReminders(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Generating pending task reminders for user: {}", currentUser.getId());
        Set<ReminderResponse> reminders = reminderService.generatePendingTaskReminders(currentUser.getId());
        return ResponseEntity.ok(reminders);
    }

    @PostMapping("/generate/overdue")
    public ResponseEntity<Set<ReminderResponse>> generateOverdueTaskReminders(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Generating overdue task reminders for user: {}", currentUser.getId());
        Set<ReminderResponse> reminders = reminderService.generateOverdueTaskReminders(currentUser.getId());
        return ResponseEntity.ok(reminders);
    }
} 