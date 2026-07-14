package com.antonletov.speechmood.service;

import com.antonletov.speechmood.enums.MessageType;
import com.antonletov.speechmood.model.Chat;
import com.antonletov.speechmood.model.ChatMessage;
import com.antonletov.speechmood.model.User;
import com.antonletov.speechmood.repository.ChatMessageRepository;
import com.antonletov.speechmood.repository.ChatRepository;
import com.antonletov.speechmood.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    @TempDir
    Path tempDir;

    private User creator;
    private User participant;
    private Chat chat;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");

        participant = new User();
        participant.setId(2L);
        participant.setUsername("participant");

        chat = new Chat();
        chat.setId(100L);
        chat.setTitle("Group");
        chat.setGroupChat(true);
        chat.setCreator(creator);
        chat.setParticipants(new HashSet<>(Set.of(creator, participant)));

        ReflectionTestUtils.setField(chatService, "voiceDir", tempDir.toString());
    }

    @Test
    void createChat_shouldSaveChatWithCreatorAndParticipants() {
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chat result = chatService.createChat("Chat", 1L, Set.of(2L));

        assertThat(result.getParticipants()).containsExactlyInAnyOrder(creator, participant);
        assertThat(result.getTitle()).isEqualTo("Chat");
    }

    @Test
    void createGroupChat_shouldSetGroupFlagAndCreator() {
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chat result = chatService.createGroupChat("Group", 1L, Set.of(2L));

        assertThat(result.isGroupChat()).isTrue();
        assertThat(result.getCreator()).isEqualTo(creator);
        assertThat(result.getParticipants()).containsExactlyInAnyOrder(creator, participant);
    }

    @Test
    void getUserGroups_shouldReturnGroupsForUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(chatRepository.findAllByParticipantsContainingAndIsGroupChatTrue(creator)).thenReturn(List.of(chat));

        List<Chat> result = chatService.getUserGroups(1L);

        assertThat(result).containsExactly(chat);
    }

    @Test
    void addParticipant_shouldAddNewUser_whenInviterIsParticipant() {
        User newUser = new User();
        newUser.setId(3L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(userRepository.findById(3)).thenReturn(Optional.of(newUser));

        chatService.addParticipant(100L, 1L, 3L);

        assertThat(chat.getParticipants()).contains(newUser);
        verify(chatRepository).save(chat);
    }

    @Test
    void addParticipant_shouldThrow_whenInviterNotParticipant() {
        User outsider = new User();
        outsider.setId(5L);
        User newUser = new User();
        newUser.setId(3L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(5)).thenReturn(Optional.of(outsider));
        when(userRepository.findById(3)).thenReturn(Optional.of(newUser));

        assertThatThrownBy(() -> chatService.addParticipant(100L, 5L, 3L))
                .isInstanceOf(IllegalStateException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void addParticipant_shouldThrow_whenNewUserAlreadyParticipant() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> chatService.addParticipant(100L, 1L, 2L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeParticipant_shouldRemoveSelf_whenRequesterEqualsUserToRemove() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));

        chatService.removeParticipant(100L, 2L, 2L);

        assertThat(chat.getParticipants()).doesNotContain(participant);
        verify(chatRepository).save(chat);
    }

    @Test
    void removeParticipant_shouldThrow_whenRequesterIsNotUserToRemove() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> chatService.removeParticipant(100L, 1L, 2L))
                .isInstanceOf(IllegalStateException.class);

        verify(chatRepository, never()).save(any());
        verify(chatRepository, never()).delete(any());
    }

    @Test
    void removeParticipant_shouldThrow_whenUserNotParticipant() {
        User outsider = new User();
        outsider.setId(5L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(5)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> chatService.removeParticipant(100L, 5L, 5L))
                .isInstanceOf(IllegalStateException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void removeParticipant_shouldDeleteChat_whenLastParticipantLeaves() {
        chat.setParticipants(new HashSet<>(Set.of(participant)));

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));

        chatService.removeParticipant(100L, 2L, 2L);

        verify(chatRepository).delete(chat);
        verify(chatRepository, never()).save(any());
    }

    @Test
    void sendTextMessage_shouldSaveMessage_whenSenderIsParticipant() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage result = chatService.sendTextMessage(100L, 2L, "hello");

        assertThat(result.getContent()).isEqualTo("hello");
        assertThat(result.getSender()).isEqualTo(participant);
        assertThat(result.getChat()).isEqualTo(chat);
    }

    @Test
    void sendTextMessage_shouldThrow_whenSenderNotParticipant() {
        User outsider = new User();
        outsider.setId(5L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(5)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> chatService.sendTextMessage(100L, 5L, "hello"))
                .isInstanceOf(IllegalStateException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendVoiceMessage_shouldThrow_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "voice.webm", "audio/webm", new byte[0]);

        assertThatThrownBy(() -> chatService.sendVoiceMessage(100L, 2L, emptyFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendVoiceMessage_shouldThrow_whenFileIsNull() {
        assertThatThrownBy(() -> chatService.sendVoiceMessage(100L, 2L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendVoiceMessage_shouldThrow_whenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile("file", "voice.txt", "text/plain", "not-audio".getBytes());

        assertThatThrownBy(() -> chatService.sendVoiceMessage(100L, 2L, file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendVoiceMessage_shouldThrow_whenSenderNotParticipant() {
        User outsider = new User();
        outsider.setId(5L);
        MockMultipartFile file = new MockMultipartFile("file", "voice.webm", "audio/webm", "audio-bytes".getBytes());

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(5)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> chatService.sendVoiceMessage(100L, 5L, file))
                .isInstanceOf(IllegalStateException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendVoiceMessage_shouldSaveFileAndMessage_whenValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "voice.webm", "audio/webm", "audio-bytes".getBytes());

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage result = chatService.sendVoiceMessage(100L, 2L, file);

        assertThat(result.getType()).isEqualTo(MessageType.VOICE);
        assertThat(result.getAudioUrl()).startsWith("/uploads/voice/").endsWith(".webm");
        String savedFileName = result.getAudioUrl().substring("/uploads/voice/".length());
        assertThat(Files.exists(tempDir.resolve(savedFileName))).isTrue();
    }

    @Test
    void getGroupById_shouldReturnChat_whenIsGroupChat() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));

        Chat result = chatService.getGroupById(100L);

        assertThat(result).isEqualTo(chat);
    }

    @Test
    void getGroupById_shouldThrow_whenChatIsNotGroupChat() {
        chat.setGroupChat(false);
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.getGroupById(100L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getGroupById_shouldThrow_whenChatNotFound() {
        when(chatRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getGroupById(100L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addGroupMembers_shouldAddNewMembers_whenRequesterIsParticipant() {
        User newUser = new User();
        newUser.setId(3L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(userRepository.findById(3)).thenReturn(Optional.of(newUser));

        chatService.addGroupMembers(100L, 1L, Set.of(3L));

        assertThat(chat.getParticipants()).contains(newUser);
        verify(chatRepository).save(chat);
    }

    @Test
    void addGroupMembers_shouldThrow_whenRequesterNotParticipant() {
        User outsider = new User();
        outsider.setId(5L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(5)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> chatService.addGroupMembers(100L, 5L, Set.of(3L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeGroupMember_shouldRemoveMember_whenRequesterIsCreator() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));

        chatService.removeGroupMember(100L, 1L, 2L);

        assertThat(chat.getParticipants()).doesNotContain(participant);
        verify(chatRepository).save(chat);
    }

    @Test
    void removeGroupMember_shouldThrow_whenRequesterIsNotCreator() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(2)).thenReturn(Optional.of(participant));
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> chatService.removeGroupMember(100L, 2L, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void removeGroupMember_shouldThrow_whenTryingToRemoveCreator() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> chatService.removeGroupMember(100L, 1L, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(chatRepository, never()).save(any());
    }
}
