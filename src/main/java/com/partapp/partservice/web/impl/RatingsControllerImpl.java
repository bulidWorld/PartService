package com.partapp.partservice.web.impl;

import java.util.*;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.api.Requests;
import com.partapp.partservice.persistence.repository.RatingRepository;
import com.partapp.partservice.persistence.repository.RoomRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.RatingRepository.Rating;
import com.partapp.partservice.api.PageResponse;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.api.RatingsApiDelegate;
import com.partapp.partservice.web.generated.model.*;

@Service
class RatingsControllerImpl implements RatingsApiDelegate {
  private final UserRepository userRepo;
  private final RatingRepository ratingRepo;
  private final RoomRepository roomRepo;
  RatingsControllerImpl(UserRepository userRepo, RatingRepository ratingRepo, RoomRepository roomRepo) {
    this.userRepo = userRepo;
    this.ratingRepo = ratingRepo;
    this.roomRepo = roomRepo;
  }

  protected String authorization() {
    ServletRequestAttributes a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return a == null ? null : a.getRequest().getHeader("Authorization");
  }

  protected String requestId() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    Object id = attributes == null ? null : attributes.getRequest().getAttribute("requestId");
    return id == null ? "" : id.toString();
  }

  public Rating rate(String bearer, UUID topicId, Requests.Rating r) {
    UUID uid = userRepo.authenticate(bearer);
    if (uid.equals(r.toUserId()))
      throw ApiException.bad("不能评价自己");
    if (!ratingRepo.CATEGORIES.containsAll(r.categoryCodes() == null ? Set.of() : r.categoryCodes()))
      throw ApiException.bad("包含无效评价分类");
    boolean both = roomRepo.allRooms().stream().filter(x -> x.topicId().equals(topicId))
        .anyMatch(x -> x.members().contains(uid) && x.members().contains(r.toUserId()));
    if (!both)
      throw ApiException.conflict("双方未共同参与该主题");
    if (ratingRepo.allRatings().stream()
        .anyMatch(x -> x.topicId().equals(topicId) && x.fromUserId().equals(uid) && x.toUserId().equals(r.toUserId())))
      throw ApiException.conflict("该主题已评价");
    if (!Set.of("good", "neutral", "bad").contains(r.score()))
      throw ApiException.bad("评分无效");
    Rating x = new Rating(UUID.randomUUID(), topicId, uid, r.toUserId(), r.score(),
        r.categoryCodes() == null ? Set.of() : Set.copyOf(r.categoryCodes()), r.comment(), Instant.now());
    ratingRepo.putRating(x.id(), x);
    return x;
  }

  public PageResponse<Rating> ratings(UUID userId) {
    return new PageResponse<>(ratingRepo.allRatings().stream().filter(x -> x.toUserId().equals(userId))
        .sorted(Comparator.comparing(Rating::createdAt).reversed()).toList(), null, false);
  }

  @Override
  public ResponseEntity<ApiResponseRatingCategories> listRatingCategories() {
    return ResponseEntity.ok(new ApiResponseRatingCategories(0, "ok", List.of("reliable", "punctual", "communicative", "friendly", "helpful", "average", "flat_communication",
        "no_show", "late", "harassment", "verbal_attack", "misinformation"), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseRatings> listUserRatings(UUID id, String cursor, Integer limit) {
    return ResponseEntity.ok(new ApiResponseRatings(0, "ok", ApiModelMapper.ratings(ratings(id)), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseRating> rateTopicParticipant(UUID id, RatingRequest x) {
    return ResponseEntity.ok(new ApiResponseRating(0, "ok", ApiModelMapper.rating(rate(authorization(), id, new Requests.Rating(x.getToUserId(), x.getScore(),
        x.getCategoryCodes() == null ? null : Set.copyOf(x.getCategoryCodes()), x.getComment()))), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseRatingSummary> getMyRatingSummary() {
    return ResponseEntity.ok(new ApiResponseRatingSummary(0, "ok", new RatingSummary(0, 0, 0, 0, List.of()), requestId()));
  }
}
