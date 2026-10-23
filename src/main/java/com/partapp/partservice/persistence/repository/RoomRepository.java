package com.partapp.partservice.persistence.repository;

import java.time.*;
import java.util.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.partapp.partservice.persistence.mapper.*;
import com.partapp.partservice.persistence.entity.*;
import org.springframework.stereotype.Component;

@Component
public class RoomRepository {
    public record Room(UUID id, UUID topicId, UUID ownerId, Set<UUID> members, Instant createdAt) {
    }

    private final ChatRoomMapper rooms;
    private final ChatMemberMapper members;

    public RoomRepository(ChatRoomMapper r, ChatMemberMapper m) {
        rooms = r;
        members = m;
    }

    public Room getRoom(UUID id) {
        var e = rooms.selectById(id);
        return e == null ? null : from(e);
    }

    public void putRoom(UUID id, Room v) {
        var e = new ChatRoomEntity();
        e.id = v.id();
        e.topicId = v.topicId();
        e.createdAt = v.createdAt();
        e.status = "active";
        if (rooms.selectById(id) == null)
            rooms.insert(e);
        else
            rooms.updateById(e);
        members.delete(new QueryWrapper<ChatMemberEntity>().eq("room_id", id));
        for (UUID u : v.members()) {
            var m = new ChatMemberEntity();
            m.roomId = id;
            m.userId = u;
            m.role = u.equals(v.ownerId()) ? "owner" : "member";
            members.insert(m);
        }
    }

    public List<Room> allRooms() {
        return rooms.selectList(null).stream().map(this::from).toList();
    }

    private Room from(ChatRoomEntity e) {
        Set<UUID> ms = new HashSet<>();
        var rows = members.selectList(new QueryWrapper<ChatMemberEntity>().eq("room_id", e.id));
        rows.forEach(x -> ms.add(x.userId));
        UUID owner = rows.stream().filter(x -> "owner".equals(x.role)).map(x -> x.userId).findFirst().orElse(null);
        return new Room(e.id, e.topicId, owner, ms, e.createdAt);
    }
}
