package com.authmodule.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory fan-out for post engagement events (likes/comments).
 * Suitable for a single backend instance; scale-out would need a shared bus.
 */
@Component
public class PostEventHub {

    private static final Logger log = LoggerFactory.getLogger(PostEventHub.class);
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> listeners = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID postId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        listeners.computeIfAbsent(postId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(postId, emitter));
        emitter.onTimeout(() -> remove(postId, emitter));
        emitter.onError(ex -> remove(postId, emitter));
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("postId", postId.toString()), MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            remove(postId, emitter);
        }
        return emitter;
    }

    public void publish(UUID postId, String eventName, Object payload) {
        List<SseEmitter> emitters = listeners.get(postId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                log.debug("Dropping dead SSE subscriber for post {}", postId);
                remove(postId, emitter);
                try {
                    emitter.completeWithError(ex);
                } catch (Exception ignored) {
                    // already closed
                }
            }
        }
    }

    private void remove(UUID postId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = listeners.get(postId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            listeners.remove(postId, emitters);
        }
    }
}
