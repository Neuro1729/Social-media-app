package com.authmodule.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.likes.sync-enabled", havingValue = "true", matchIfMissing = true)
public class LikeSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LikeSyncScheduler.class);

    private final LikeSyncService likeSyncService;

    public LikeSyncScheduler(LikeSyncService likeSyncService) {
        this.likeSyncService = likeSyncService;
    }

    @Scheduled(fixedDelayString = "${app.likes.sync-interval-ms:5000}")
    public void syncLikes() {
        int synced = likeSyncService.syncDirtyLikes();
        if (synced > 0) {
            log.debug("Synchronized likes for {} posts", synced);
        }
    }
}
