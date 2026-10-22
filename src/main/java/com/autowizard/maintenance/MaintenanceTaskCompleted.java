package com.autowizard.maintenance;

/**
 * A maintenance task was done. Published in the transaction that completes it; other modules
 * react after the commit without the maintenance module knowing them.
 */
public record MaintenanceTaskCompleted(Long taskId, Long vehicleId, Long userId) {
}
