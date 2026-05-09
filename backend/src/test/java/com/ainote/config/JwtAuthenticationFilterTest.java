package com.ainote.config;

import com.ainote.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWhenTokenIsValidAndUserExists() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(7L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("tester");
        when(userRepository.existsByIdAndUsernameAndIsDeletedFalse(7L, "tester")).thenReturn(true);

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(7L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void shouldSkipAuthenticationWhenUserNoLongerExists() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer stale-token");
        when(jwtUtil.validateToken("stale-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("stale-token")).thenReturn(9L);
        when(jwtUtil.getUsernameFromToken("stale-token")).thenReturn("ghost");
        when(userRepository.existsByIdAndUsernameAndIsDeletedFalse(9L, "ghost")).thenReturn(false);

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
