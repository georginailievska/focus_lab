package mk.focuslab.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Најавата не смее да биде отворена за пробување лозинки во недоглед. */
class AuthRateLimitFilterTest {

    private MockHttpServletRequest login(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    @DisplayName("По дваесет обиди од иста адреса најавата се одбива со 429")
    void blocksAfterTwentyAttempts() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 20; attempt++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(login("10.0.0.1"), response, new MockFilterChain());

            assertThat(response.getStatus()).as("обид " + attempt).isEqualTo(200);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(login("10.0.0.1"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Ограничувањето е по адреса, не за сите заедно")
    void limitIsPerClient() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 20; attempt++) {
            filter.doFilter(login("10.0.0.1"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(login("10.0.0.2"), other, new MockFilterChain());

        assertThat(other.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("X-Forwarded-For не се верува кога не стоиме зад proxy")
    void forwardedHeaderIsIgnoredByDefault() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        // Напаѓачот менува X-Forwarded-For на секое барање за да го измами броењето
        for (int attempt = 1; attempt <= 20; attempt++) {
            MockHttpServletRequest request = login("10.0.0.1");
            request.addHeader("X-Forwarded-For", "1.2.3." + attempt);
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest request = login("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.99");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request, blocked, new MockFilterChain());

        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Регистрацијата има свој, построг лимит")
    void registerHasItsOwnBudget() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 20; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
            request.setServletPath("/api/auth/register");
            request.setRemoteAddr("10.0.0.1");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setServletPath("/api/auth/register");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request, blocked, new MockFilterChain());

        assertThat(blocked.getStatus()).isEqualTo(429);

        // ...а најавата од истата адреса сè уште поминува
        MockHttpServletResponse stillFine = new MockHttpServletResponse();
        filter.doFilter(login("10.0.0.1"), stillFine, new MockFilterChain());

        assertThat(stillFine.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Останатите рути не ги допира")
    void otherPathsPassThrough() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 50; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sessions");
            request.setServletPath("/api/sessions");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
