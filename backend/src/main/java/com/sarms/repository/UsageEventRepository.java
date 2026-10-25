package com.sarms.repository;

import com.sarms.model.UsageEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface UsageEventRepository extends MongoRepository<UsageEvent, String> {
    List<UsageEvent> findByTimestampAfter(Instant since);
    boolean existsByDemoTrueAndTimestampAfter(Instant since);
}
