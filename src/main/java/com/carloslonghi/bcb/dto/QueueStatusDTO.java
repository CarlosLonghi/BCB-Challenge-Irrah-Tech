package com.carloslonghi.bcb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class QueueStatusDTO {
    private int queuedTotal;
    private int queuedUrgent;
    private int queuedNormal;
    private int processing;
    private int sent;
    private int delivered;
    private int failed;
}
