package com.antonletov.speechmood.service;

import com.antonletov.speechmood.enums.FriendshipStatus;
import com.antonletov.speechmood.model.Friendship;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.FriendshipRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendshipService friendshipService;

    private User requester;
    private User addressee;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setUsername("requester");

        addressee = new User();
        addressee.setId(2L);
        addressee.setUsername("addressee");
    }

    @Test
    void sendRequest_shouldThrow_whenRequesterEqualsAddressee() {
        assertThatThrownBy(() -> friendshipService.sendRequest(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void sendRequest_shouldCreatePendingFriendship_whenNoExistingRelationship() {
        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2)).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findAnyRelationship(requester, addressee)).thenReturn(Optional.empty());

        friendshipService.sendRequest(1L, 2L);

        verify(friendshipRepository).save(argThatFriendship(f ->
                f.getRequester() == requester
                        && f.getAddressee() == addressee
                        && f.getStatus() == FriendshipStatus.PENDING));
    }

    @Test
    void sendRequest_shouldThrow_whenRelationshipAlreadyExists() {
        Friendship existing = new Friendship();
        existing.setStatus(FriendshipStatus.ACCEPTED);

        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2)).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findAnyRelationship(requester, addressee)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> friendshipService.sendRequest(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACCEPTED");

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendRequest_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.sendRequest(1L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void acceptRequest_shouldSetStatusAccepted_whenPendingRequestExists() {
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);

        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2)).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findByRequesterAndAddressee(requester, addressee)).thenReturn(Optional.of(friendship));

        friendshipService.acceptRequest(2L, 1L);

        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void acceptRequest_shouldThrow_whenNoPendingRequestExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2)).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findByRequesterAndAddressee(requester, addressee)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.acceptRequest(2L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptRequest_shouldThrow_whenRequestIsNotPending() {
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.DECLINED);

        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2)).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findByRequesterAndAddressee(requester, addressee)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.acceptRequest(2L, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void declineRequest_shouldSetStatusDeclined_whenPendingRequestExists() {
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);

        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2)).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findByRequesterAndAddressee(requester, addressee)).thenReturn(Optional.of(friendship));

        friendshipService.declineRequest(2L, 1L);

        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.DECLINED);
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void getIncomingRequests_shouldReturnPendingRequestsForAddressee() {
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);

        when(userRepository.findById(2)).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findAllByAddresseeAndStatus(addressee, FriendshipStatus.PENDING))
                .thenReturn(List.of(friendship));

        List<Friendship> result = friendshipService.getIncomingRequests(2L);

        assertThat(result).containsExactly(friendship);
    }

    @Test
    void getFriends_shouldReturnOtherParty_whenUserIsRequesterOrAddressee() {
        Friendship asRequester = new Friendship();
        asRequester.setRequester(requester);
        asRequester.setAddressee(addressee);
        asRequester.setStatus(FriendshipStatus.ACCEPTED);

        User thirdUser = new User();
        thirdUser.setId(3L);
        thirdUser.setUsername("third");

        Friendship asAddressee = new Friendship();
        asAddressee.setRequester(thirdUser);
        asAddressee.setAddressee(requester);
        asAddressee.setStatus(FriendshipStatus.ACCEPTED);

        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(friendshipRepository.findAllByStatus(FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(asRequester, asAddressee));

        List<User> result = friendshipService.getFriends(1L);

        assertThat(result).containsExactlyInAnyOrder(addressee, thirdUser);
    }

    @Test
    void getFriends_shouldIgnoreFriendshipsNotInvolvingUser() {
        User thirdUser = new User();
        thirdUser.setId(3L);
        User fourthUser = new User();
        fourthUser.setId(4L);

        Friendship unrelated = new Friendship();
        unrelated.setRequester(thirdUser);
        unrelated.setAddressee(fourthUser);
        unrelated.setStatus(FriendshipStatus.ACCEPTED);

        when(userRepository.findById(1)).thenReturn(Optional.of(requester));
        when(friendshipRepository.findAllByStatus(FriendshipStatus.ACCEPTED)).thenReturn(List.of(unrelated));

        List<User> result = friendshipService.getFriends(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getFriendshipStatus_shouldReturnStatusName_whenRelationshipExists() {
        Friendship friendship = new Friendship();
        friendship.setStatus(FriendshipStatus.ACCEPTED);

        when(friendshipRepository.findAnyRelationship(requester, addressee)).thenReturn(Optional.of(friendship));

        String status = friendshipService.getFriendshipStatus(requester, addressee);

        assertThat(status).isEqualTo("ACCEPTED");
    }

    @Test
    void getFriendshipStatus_shouldReturnNone_whenNoRelationshipExists() {
        when(friendshipRepository.findAnyRelationship(requester, addressee)).thenReturn(Optional.empty());

        String status = friendshipService.getFriendshipStatus(requester, addressee);

        assertThat(status).isEqualTo("NONE");
    }

    private Friendship argThatFriendship(java.util.function.Predicate<Friendship> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
