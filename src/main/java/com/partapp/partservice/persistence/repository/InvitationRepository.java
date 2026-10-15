package com.partapp.partservice.persistence.repository;

import java.util.*;

import com.partapp.partservice.persistence.mapper.InvitationMapper;
import com.partapp.partservice.persistence.entity.InvitationEntity;
import org.springframework.stereotype.Component;

/** PostgreSQL-backed persistence for invitations. */
@Component
public class InvitationRepository {
  public record Invitation(UUID id, UUID topicId, UUID inviteeId, UUID inviterId, String status, UUID roomId) {
  }

  private final InvitationMapper mapper;

  public InvitationRepository(InvitationMapper mapper) {
    this.mapper = mapper;
  }

  public Invitation getInvitation(UUID id) {
    return from(mapper.selectById(id));
  }

  public void putInvitation(UUID id, Invitation v) {
    InvitationEntity e = new InvitationEntity();
    e.id = v.id();
    e.topicId = v.topicId();
    e.inviteeId = v.inviteeId();
    e.inviterId = v.inviterId();
    e.status = v.status();
    if (mapper.selectById(id) == null)
      mapper.insert(e);
    else
      mapper.updateById(e);
  }

  public List<Invitation> allInvitations() {
    return mapper.selectList(null).stream().map(this::from).toList();
  }

  private Invitation from(InvitationEntity e) {
    return e == null ? null : new Invitation(e.id, e.topicId, e.inviteeId, e.inviterId, e.status, null);
  }
}
