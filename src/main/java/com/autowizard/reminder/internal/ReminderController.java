package com.autowizard.reminder.internal;

import com.autowizard.identity.CurrentUser;
import com.autowizard.identity.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reminders are created by the nightly job; clients read them and mark them as notified. */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public ResponseEntity<List<ReminderResponse>> getOpenReminders(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(reminderService.getOpenReminders(currentUser.getId()));
    }

    @PutMapping("/{id}/notified")
    public ResponseEntity<Void> markAsNotified(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        reminderService.markAsNotified(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }
}
