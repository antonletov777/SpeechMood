package com.antonletov.speechmood.service;

import com.antonletov.speechmood.model.Friendship;
import com.antonletov.speechmood.model.FriendshipStatus;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.FriendshipRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;


    @Transactional
    public void sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("Нельзя добавить в друзья самого себя");
        }

        User requester = getUser(requesterId);
        User addressee = getUser(addresseeId);

        friendshipRepository.findAnyRelationship(requester, addressee).ifPresent(f -> {
            throw new IllegalStateException("Связь между пользователями уже существует со статусом: " + f.getStatus());
        });

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);

        friendshipRepository.save(friendship);
        log.info("Заявка в друзья отправлена от {} к {}", requesterId, addresseeId);
    }


    @Transactional
    public void acceptRequest(Long addresseeId, Long requesterId) {
        Friendship friendship = getPendingFriendship(requesterId, addresseeId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        log.info("Пользователь {} принял заявку от {}", addresseeId, requesterId);
    }


    @Transactional
    public void declineRequest(Long addresseeId, Long requesterId) {
        Friendship friendship = getPendingFriendship(requesterId, addresseeId);
        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);
        log.info("Пользователь {} отклонил заявку от {}", addresseeId, requesterId);
    }


    @Transactional(readOnly = true)
    public List<Friendship> getIncomingRequests(Long userId) {
        User user = getUser(userId);
        return friendshipRepository.findAllByAddresseeAndStatus(user, FriendshipStatus.PENDING);
    }

    private User getUser(Long id) {
        return userRepository.findById(Math.toIntExact(id)).orElseThrow(() -> new EntityNotFoundException("Юзер не найден"));
    }

    private Friendship getPendingFriendship(Long requesterId, Long addresseeId) {
        User requester = getUser(requesterId);
        User addressee = getUser(addresseeId);

        return friendshipRepository.findByRequesterAndAddressee(requester, addressee)
                .filter(f -> f.getStatus() == FriendshipStatus.PENDING)
                .orElseThrow(() -> new IllegalStateException("Активная заявка не найдена"));
    }

    @Transactional(readOnly = true)
    public List<User> getFriends(Long userId) {
        User user = getUser(userId);

        List<Friendship> friendships = friendshipRepository.findAllByStatus(FriendshipStatus.ACCEPTED);

        return friendships.stream()
                .filter(f -> f.getRequester().equals(user) || f.getAddressee().equals(user))
                .map(f -> f.getRequester().equals(user) ? f.getAddressee() : f.getRequester())
                .toList();
    }

    public String getFriendshipStatus(User user1, User user2) {
        return friendshipRepository.findAnyRelationship(user1, user2)
                .map(f -> f.getStatus().name()) // Используем .name() для получения строки "PENDING", "ACCEPTED" и т.д.
                .orElse("NONE");
    }
}