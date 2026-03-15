package com.juno.healthapp.service;

import com.juno.healthapp.dto.QueueUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // Broadcast to everyone watching a department queue
    public void notifyDepartmentQueue(UUID departmentId, QueueUpdateEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/queue/" + departmentId,
                event
        );
    }

    // Send to a specific patient only
    public void notifyPatient(UUID patientId, QueueUpdateEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/patient/" + patientId,
                event
        );
    }
}