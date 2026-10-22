package com.autowizard.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The only source of "now". Time-dependent rules (overdue tasks, reminders) are tested by
 * replacing this bean with a fixed time, instead of depending on when the test runs.
 *
 * <p>
 * Dates in the domain are local date-times as the user sees them (a service is scheduled
 * "at 10:00"), so the clock runs in the zone of the users, {@code app.time-zone}.
 * </p>
 */
@Component
public class TimeService {

    private final Clock clock;

    public TimeService(@Value("${app.time-zone:Europe/Rome}") ZoneId zone) {
        this.clock = Clock.system(zone);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
