package com.carloslonghi.bcb.infra.worker;

import com.carloslonghi.bcb.infra.queue.PriorityMessageQueue;
import com.carloslonghi.bcb.model.Message;
import com.carloslonghi.bcb.model.enums.MessageStatus;
import com.carloslonghi.bcb.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class QueueProcessor {

    private final PriorityMessageQueue priorityMessageQueue;
    private final MessageRepository messageRepository;

    private final AtomicBoolean running = new AtomicBoolean(true);

    @PostConstruct
    public void start() {
        Thread worker = new Thread(() -> {
            while (running.get()) {
                Message message = null;
                try {
                    message = priorityMessageQueue.take();

                    message.setStatus(MessageStatus.PROCESSING);
                    messageRepository.save(message);

                    Thread.sleep(4000); // simula tempo de envio

                    message.setStatus(MessageStatus.SENT);
                    messageRepository.save(message);

                    Thread.sleep(4000);

                    message.setStatus(MessageStatus.DELIVERED);
                    messageRepository.save(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    message.setStatus(MessageStatus.FAILED);
                    messageRepository.save(message);
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
    }
}
