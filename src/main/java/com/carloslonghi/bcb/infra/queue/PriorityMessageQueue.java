package com.carloslonghi.bcb.infra.queue;

import com.carloslonghi.bcb.model.Message;
import com.carloslonghi.bcb.model.enums.MessagePriority;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class PriorityMessageQueue {

    private final PriorityBlockingQueue<Message> queue;

    public PriorityMessageQueue() {
        this.queue = new PriorityBlockingQueue<>(100, new MessageComparator());
    }

    public void enqueue(Message message) {
        queue.offer(message);
    }

    public Message take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }

    public int sizeByPriority(MessagePriority priority) {
        return (int) queue.stream()
                .filter(msg -> msg.getPriority() == priority)
                .count();
    }

    private static class MessageComparator implements Comparator<Message> {
        @Override
        public int compare(Message m1, Message m2) {
            // URGENT < NORMAL no compareTo (mas URGENT tem prioridade)
            int priorityComparison = m1.getPriority().compareTo(m2.getPriority());
            if (priorityComparison != 0) {
                return -priorityComparison;
            }
            // Se mesma prioridade, ordenar por data de criação
            return m1.getCreatedAt().compareTo(m2.getCreatedAt());
        }
    }
}
