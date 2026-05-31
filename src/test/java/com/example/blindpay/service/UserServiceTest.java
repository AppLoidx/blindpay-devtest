package com.example.blindpay.service;

import com.example.blindpay.model.User;
import com.example.blindpay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlindPayApiService blindPayApi;

    @InjectMocks
    private UserService userService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder()
                .id(1L)
                .name("Alice Doe")
                .email("alice@example.com")
                .receiverId("re_alice")
                .bankAccountId("ba_alice")
                .walletId("bl_alice")
                .walletAddress("0xalice")
                .blockchainWalletId("bw_alice")
                .tosId("to_alice")
                .build();

        bob = User.builder()
                .id(2L)
                .name("Bob Smith")
                .email("bob@example.com")
                .receiverId("re_bob")
                .bankAccountId("ba_bob")
                .walletId("bl_bob")
                .walletAddress("0xbob")
                .blockchainWalletId("bw_bob")
                .tosId("to_bob")
                .build();
    }

    @Test
    void getAllUsers_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(alice, bob));
        List<User> result = userService.getAllUsers();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alice Doe");
        assertThat(result.get(1).getName()).isEqualTo("Bob Smith");
    }

    @Test
    void getUser_existingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        User result = userService.getUser(1L);
        assertThat(result.getName()).isEqualTo("Alice Doe");
        assertThat(result.getWalletId()).isEqualTo("bl_alice");
    }

    @Test
    void getUser_missingId_throwsIllegalArgument() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: 99");
    }

    @Test
    void payin_createsQuoteThenExecutes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(blindPayApi.createPayinQuote("bl_alice", 10000))
                .thenReturn(Map.of("id", "pq_123"));
        when(blindPayApi.createPayin("pq_123"))
                .thenReturn(Map.of("id", "pi_456", "status", "processing"));

        Map<String, Object> result = userService.payin(1L, 10000);

        assertThat(result.get("id")).isEqualTo("pi_456");
        assertThat(result.get("status")).isEqualTo("processing");
        verify(blindPayApi).createPayinQuote("bl_alice", 10000);
        verify(blindPayApi).createPayin("pq_123");
    }

    @Test
    void payout_createsQuoteThenExecutes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(blindPayApi.createPayoutQuote("ba_alice", 5000))
                .thenReturn(Map.of("id", "qt_789"));
        when(blindPayApi.createPayout("qt_789", "0xalice"))
                .thenReturn(Map.of("id", "po_abc", "status", "pending"));

        Map<String, Object> result = userService.payout(1L, 5000);

        assertThat(result.get("id")).isEqualTo("po_abc");
        assertThat(result.get("status")).isEqualTo("pending");
        verify(blindPayApi).createPayoutQuote("ba_alice", 5000);
        verify(blindPayApi).createPayout("qt_789", "0xalice");
    }

    @Test
    void transfer_createsQuoteThenExecutes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(blindPayApi.createTransferQuote("bl_alice", "0xbob", 3000))
                .thenReturn(Map.of("id", "tq_111"));
        when(blindPayApi.createTransfer("tq_111"))
                .thenReturn(Map.of("id", "tr_222", "status", "completed"));

        Map<String, Object> result = userService.transfer(1L, 2L, 3000);

        assertThat(result.get("id")).isEqualTo("tr_222");
        assertThat(result.get("status")).isEqualTo("completed");
        verify(blindPayApi).createTransferQuote("bl_alice", "0xbob", 3000);
        verify(blindPayApi).createTransfer("tq_111");
    }

    @Test
    void getBalance_callsGetWalletWithCorrectIds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(blindPayApi.getWalletBalance("re_alice", "bl_alice"))
                .thenReturn(Map.of("USDB", Map.of("symbol", "USDB", "amount", 0)));

        Map<String, Object> result = userService.getBalance(1L);

        assertThat(result).containsKey("USDB");
        verify(blindPayApi).getWalletBalance("re_alice", "bl_alice");
    }
}
