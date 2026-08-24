package com.aaax.service;

import com.aaax.entity.po.user_management.UserProfile;
import com.aaax.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("search should return empty list when searchText blank")
    void search_shouldReturnEmptyWhenBlank() {
        assertTrue(userProfileService.search("").isEmpty());
        assertTrue(userProfileService.search(null).isEmpty());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("searchNames should return empty list when searchText blank")
    void searchNames_shouldReturnEmptyWhenBlank() {
        assertTrue(userProfileService.searchNames("").isEmpty());
        assertTrue(userProfileService.searchNames(null).isEmpty());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("searchEmail should return empty list when searchText blank")
    void searchEmail_shouldReturnEmptyWhenBlank() {
        assertTrue(userProfileService.searchEmail(null).isEmpty());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("getByUserIds should delegate to repository")
    void getByUserIds_shouldDelegate() {
        List<UserProfile> profiles = List.of(UserProfile.builder().id(1L).userId(10L).build());
        when(userProfileRepository.findByUserIdIn(anyList())).thenReturn(profiles);

        List<UserProfile> result = userProfileService.getByUserIds(List.of(10L));

        assertEquals(1, result.size());
        verify(userProfileRepository).findByUserIdIn(anyList());
    }
}
