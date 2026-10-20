package com.partapp.partservice.persistence.repository;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.partapp.partservice.persistence.mapper.AccountDeletionMapper;
import com.partapp.partservice.web.ApiException;

@Service
public class AccountDeletionService {
  private final UserRepository users;
  private final AccountDeletionMapper deletion;

  public AccountDeletionService(UserRepository users, AccountDeletionMapper deletion) {
    this.users = users;
    this.deletion = deletion;
  }

  @Transactional
  public void deleteCurrentUser(String bearer) {
    UUID id = users.authenticate(bearer);
    if (deletion.lockUser(id) == null)
      throw ApiException.unauthorized("用户不存在");
    deletion.deleteRatingStats(id);
    deletion.deleteRatings(id);
    deletion.deleteTopics(id);
    deletion.deleteRecipients(id);
    deletion.deleteInvitations(id);
    deletion.deleteParticipants(id);
    deletion.clearMemberReadPositions(id);
    deletion.clearMessageReadPositions(id);
    deletion.deleteMembers(id);
    deletion.deleteReadPositions(id);
    deletion.deleteMessages(id);
    deletion.deleteRefreshTokens(id);
    deletion.deleteVerificationCodes(id);
    deletion.deleteState(id);
    deletion.deleteIdentities(id);
    deletion.deleteAuditLogs(id);
    deletion.deleteUser(id);
  }
}
