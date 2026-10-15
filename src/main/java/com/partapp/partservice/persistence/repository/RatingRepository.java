package com.partapp.partservice.persistence.repository;

import java.time.*;
import java.util.*;

import com.partapp.partservice.persistence.mapper.RatingMapper;
import com.partapp.partservice.persistence.entity.RatingEntity;
import org.springframework.stereotype.Component;

/** PostgreSQL-backed persistence for ratings. */
@Component
public class RatingRepository {
    public record Rating(UUID id, UUID topicId, UUID fromUserId, UUID toUserId, String score, Set<String> categories,
            String comment, Instant createdAt) {
    }

    public static final Set<String> CATEGORIES = Set.of("reliable", "punctual", "communicative", "friendly", "helpful",
            "average", "flat_communication", "no_show", "late", "harassment", "verbal_attack", "misinformation");

    private final RatingMapper mapper;

    public RatingRepository(RatingMapper mapper) {
        this.mapper = mapper;
    }

    public Rating getRating(UUID id) {
        return from(mapper.selectById(id));
    }

    public void putRating(UUID id, Rating v) {
        var e = new RatingEntity();
        e.id = v.id();
        e.topicId = v.topicId();
        e.fromUserId = v.fromUserId();
        e.toUserId = v.toUserId();
        e.score = v.score();
        e.comment = v.comment();
        e.createdAt = v.createdAt();
        if (mapper.selectById(id) == null)
            mapper.insert(e);
        else
            mapper.updateById(e);
    }

    public List<Rating> allRatings() {
        return mapper.selectList(null).stream().map(this::from).toList();
    }

    private Rating from(RatingEntity e) {
        return e == null ? null
                : new Rating(e.id, e.topicId, e.fromUserId, e.toUserId, e.score, Set.of(), e.comment, e.createdAt);
    }
}
