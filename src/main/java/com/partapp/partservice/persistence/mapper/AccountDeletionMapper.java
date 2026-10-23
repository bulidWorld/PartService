package com.partapp.partservice.persistence.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Deletes dependent rows before their parent rows, using the existing foreign keys. */
@Mapper
public interface AccountDeletionMapper {
  @Select("SELECT id FROM \"user\" WHERE id = #{id} FOR UPDATE")
  UUID lockUser(@Param("id") UUID id);

  @Delete("""
      DELETE FROM user_rating_stat WHERE user_id = #{id} OR user_id IN (
        SELECT to_user_id FROM rating WHERE from_user_id = #{id} OR to_user_id = #{id}
          OR topic_id IN (SELECT id FROM topic WHERE publisher_id = #{id}))
      """)
  void deleteRatingStats(@Param("id") UUID id);

  // rating_category_item is removed by ON DELETE CASCADE.
  @Delete("""
      DELETE FROM rating WHERE from_user_id = #{id} OR to_user_id = #{id}
        OR topic_id IN (SELECT id FROM topic WHERE publisher_id = #{id})
      """)
  void deleteRatings(@Param("id") UUID id);

  // Recipients and participants cascade; rooms and message history remain.
  @Delete("DELETE FROM topic WHERE publisher_id = #{id}")
  void deleteTopics(@Param("id") UUID id);

  @Delete("DELETE FROM topic_recipient WHERE user_id = #{id}")
  void deleteRecipients(@Param("id") UUID id);


  @Delete("DELETE FROM topic_participant WHERE user_id = #{id}")
  void deleteParticipants(@Param("id") UUID id);

  @Delete("DELETE FROM chat_member WHERE user_id = #{id}")
  void deleteMembers(@Param("id") UUID id);

  @Delete("DELETE FROM chat_message_read WHERE user_id = #{id}")
  void deleteReadPositions(@Param("id") UUID id);

  @Delete("DELETE FROM refresh_token WHERE user_id = #{id}")
  void deleteRefreshTokens(@Param("id") UUID id);

  @Delete("""
      DELETE FROM verification_code v USING user_identity i
      WHERE i.user_id = #{id} AND v.identity_type = i.identity_type AND v.identity_value = i.identity_value
      """)
  void deleteVerificationCodes(@Param("id") UUID id);

  @Delete("""
      DELETE FROM service_state s WHERE
        (s.namespace = 'users' AND s.state_key = CAST(#{id} AS text))
        OR (s.namespace IN ('identities', 'access', 'refresh') AND s.state_value = to_jsonb(CAST(#{id} AS text)))
        OR (s.namespace = 'codes' AND EXISTS (
          SELECT 1 FROM user_identity i WHERE i.user_id = #{id}
            AND s.state_key IN (i.identity_type || ':' || i.identity_value || ':register',
                               i.identity_type || ':' || i.identity_value || ':login',
                               i.identity_type || ':' || i.identity_value || ':reset')))
      """)
  void deleteState(@Param("id") UUID id);

  @Delete("DELETE FROM user_identity WHERE user_id = #{id}")
  void deleteIdentities(@Param("id") UUID id);

  @Delete("DELETE FROM audit_log WHERE actor_user_id = #{id}")
  void deleteAuditLogs(@Param("id") UUID id);

  // User risk tags and remaining statistics cascade.
  @Delete("DELETE FROM \"user\" WHERE id = #{id}")
  void deleteUser(@Param("id") UUID id);
}
