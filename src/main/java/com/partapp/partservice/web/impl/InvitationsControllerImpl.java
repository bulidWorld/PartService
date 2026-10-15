package com.partapp.partservice.web.impl;

import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.persistence.repository.InvitationRepository;
import com.partapp.partservice.persistence.repository.RoomRepository;
import com.partapp.partservice.persistence.repository.TopicRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.InvitationRepository.Invitation;
import com.partapp.partservice.persistence.repository.RoomRepository.Room;
import com.partapp.partservice.persistence.repository.TopicRepository.Topic;
import com.partapp.partservice.api.PageResponse;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.api.InvitationsApiDelegate;
import com.partapp.partservice.web.generated.model.*;

@Service
class InvitationsControllerImpl implements InvitationsApiDelegate {
  private final UserRepository userRepo;
  private final InvitationRepository invitationRepo;
  private final TopicRepository topicRepo;
  private final RoomRepository roomRepo;
  InvitationsControllerImpl(UserRepository userRepo, InvitationRepository invitationRepo, TopicRepository topicRepo,
      RoomRepository roomRepo) {
    this.userRepo = userRepo;
    this.invitationRepo = invitationRepo;
    this.topicRepo = topicRepo;
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

  public Invitation accept(String bearer, UUID invitationId, boolean accepted) {
    UUID uid = userRepo.authenticate(bearer);
    Invitation i = invitationRepo.getInvitation(invitationId);
    if (i == null || !i.inviteeId().equals(uid))
      throw ApiException.notFound("邀请不存在");
    if (!i.status().equals("pending"))
      return i;
    Topic t = topicRepo.getTopic(i.topicId());
    if (!t.status().equals("open"))
      throw ApiException.conflict("主题已关闭");
    if (!accepted) {
      Invitation n = new Invitation(i.id(), i.topicId(), i.inviteeId(), i.inviterId(), "rejected", null);
      invitationRepo.putInvitation(i.id(), n);
      return n;
    }
    Room room = roomRepo.allRooms().stream().filter(x -> x.topicId().equals(t.id())).findFirst().orElseThrow();
    room.members().add(uid);
    Invitation n = new Invitation(i.id(), i.topicId(), i.inviteeId(), i.inviterId(), "accepted", room.id());
    invitationRepo.putInvitation(i.id(), n);
    return n;
  }

  public PageResponse<Invitation> invitations(String bearer, String status) {
    UUID id = userRepo.authenticate(bearer);
    List<Invitation> out = invitationRepo.allInvitations().stream().filter(i -> i.inviteeId().equals(id))
        .filter(i -> status == null || i.status().equals(status)).toList();
    return new PageResponse<>(out, null, false);
  }

  @Override
  public ResponseEntity<ApiResponseInvitations> listInvitations(String status, String cursor, Integer limit) {
    return ResponseEntity.ok(new ApiResponseInvitations(0, "ok", ApiModelMapper.invitations(invitations(authorization(), status)), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseInvitation> acceptInvitation(UUID id) {
    return ResponseEntity.ok(new ApiResponseInvitation(0, "ok", ApiModelMapper.invitation(accept(authorization(), id, true)), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseInvitation> rejectInvitation(UUID id) {
    return ResponseEntity.ok(new ApiResponseInvitation(0, "ok", ApiModelMapper.invitation(accept(authorization(), id, false)), requestId()));
  }
}
