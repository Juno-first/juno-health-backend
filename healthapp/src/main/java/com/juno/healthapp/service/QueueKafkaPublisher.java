package com.juno.healthapp.service;

import com.juno.healthapp.dto.DepartmentQueueSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueKafkaPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public void publishQueueSnapshot(DepartmentQueueSnapshotEvent snapshot) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(snapshot);

            kafkaTemplate.send(
                    "department.queue.snapshot",
                    snapshot.departmentId().toString(),
                    payload
            );

            log.info("Published queue snapshot for department={}", snapshot.departmentId());
        } catch (Exception e) {
            log.error("Failed to publish queue snapshot", e);
        }
    }
}
