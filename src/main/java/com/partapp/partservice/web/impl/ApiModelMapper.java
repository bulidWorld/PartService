package com.partapp.partservice.web.impl;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import com.partapp.partservice.api.PageResponse;
import com.partapp.partservice.persistence.repository.*;
import com.partapp.partservice.web.generated.model.*;

final class ApiModelMapper {
  private ApiModelMapper() {}

  private static OffsetDateTime time(Instant value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }

  static User user(UserRepository.User x) {
    return new User(x.id(), x.nickname(), x.communityId(), x.status(), time(x.communityChangedAt()))
        .gender(x.gender()).birthDate(x.birthDate()).avatarUrl(x.avatarUrl());
  }

  static TokenPair tokenPair(UserRepository.TokenPair x) {
    return new TokenPair(x.accessToken(), x.expiresIn(), x.refreshToken(), user(x.user()));
  }

  static Topic topic(TopicRepository.Topic x) {
    return new Topic(x.id(), x.publisherId(), x.communityId(), x.title(), x.detail(),
        new ArrayList<>(x.genders()), x.status(), time(x.createdAt()))
        .minAge(x.minAge()).maxAge(x.maxAge()).expiresAt(time(x.expiresAt()));
  }


  static Room room(RoomRepository.Room x) {
    return new Room(x.id(), x.topicId(), new ArrayList<>(x.members()), time(x.createdAt())).ownerId(x.ownerId());
  }

  static Message message(MessageRepository.Message x) {
    return new Message(x.id(), x.roomId(), x.senderId(), x.clientMessageId(), x.messageType(),
        x.contentMeta(), time(x.createdAt()), x.recalled()).content(x.content());
  }

  static Rating rating(RatingRepository.Rating x) {
    return new Rating(x.id(), x.topicId(), x.fromUserId(), x.toUserId(), Rating.ScoreEnum.fromValue(x.score()),
        new ArrayList<>(x.categories()), time(x.createdAt())).comment(x.comment());
  }

  static PageOfTopics topics(PageResponse<TopicRepository.Topic> x) {
    return new PageOfTopics().items(x.items().stream().map(ApiModelMapper::topic).toList())
        .nextCursor(x.nextCursor()).hasMore(x.hasMore());
  }


  static PageOfRooms rooms(PageResponse<RoomRepository.Room> x) {
    return new PageOfRooms().items(x.items().stream().map(ApiModelMapper::room).toList())
        .nextCursor(x.nextCursor()).hasMore(x.hasMore());
  }

  static PageOfMessages messages(PageResponse<MessageRepository.Message> x) {
    return new PageOfMessages().items(x.items().stream().map(ApiModelMapper::message).toList())
        .nextCursor(x.nextCursor()).hasMore(x.hasMore());
  }

  static PageOfRatings ratings(PageResponse<RatingRepository.Rating> x) {
    return new PageOfRatings().items(x.items().stream().map(ApiModelMapper::rating).toList())
        .nextCursor(x.nextCursor()).hasMore(x.hasMore());
  }
}
