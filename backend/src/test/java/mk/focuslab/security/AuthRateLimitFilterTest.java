package mk.focuslab.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Најавата не смее да биде отворена за пробување лозинки — ни да се блокира сама. */
class AuthRateLimitFilterTest {

    private MockHttpServletRequest request(String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletRequest login(String ip) {
        return request("/api/auth/login", ip);
    }

    /** Ланец што се однесува како сервер: враќа зададен статус. */
    private FilterChain chainReturning(int status) {
        FilterChain chain = mock(FilterChain.class);

        try {
            doAnswer(call -> {
                MockHttpServletResponse response = call.getArgument(1);
                response.setStatus(status);
                return null;
            }).when(chain).doFilter(any(), any());
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }

        return chain;
    }

    /** Погрешна лозинка: серверот враќа 401. */
    private int failedLogin(AuthRateLimitFilter filter, String ip) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(login(ip), response, chainReturning(401));
        return response.getStatus();
    }

    /** Точна лозинка: серверот враќа 200. */
    private int successfulLogin(AuthRateLimitFilter filter, String ip) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(login(ip), response, chainReturning(200));
        return response.getStatus();
    }

    @Test
    @DisplayName("Дваесет погрешни лозинки минуваат, дваесет и првата се одбива")
    void blocksAfterTwentyFailures() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 20; attempt++) {
            assertThat(failedLogin(filter, "10.0.0.1")).as("обид " + attempt).isEqualTo(401);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        FilterChain chain = chainReturning(401);
        filter.doFilter(login("10.0.0.1"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Успешните најави не го трошат буџетот")
    void successfulLoginsDoNotCount() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        // Цела просторија се најавува од иста адреса — ниедна не смее да се блокира
        for (int attempt = 1; attempt <= 50; attempt++) {
            assertThat(successfulLogin(filter, "10.0.0.1")).as("најава " + attempt).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Успешна најава го брише броењето од претходните грешки")
    void successResetsTheCounter() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 19; attempt++) {
            failedLogin(filter, "10.0.0.1");
        }

        assertThat(successfulLogin(filter, "10.0.0.1")).isEqualTo(200);

        // По точната лозинка повторно има цели дваесет обиди
        for (int attempt = 1; attempt <= 20; attempt++) {
            assertThat(failedLogin(filter, "10.0.0.1")).as("обид " + attempt).isEqualTo(401);
        }
    }

    @Test
    @DisplayName("Ограничувањето е по адреса, не за сите заедно")
    void limitIsPerClient() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 20; attempt++) {
            failedLogin(filter, "10.0.0.1");
        }

        assertThat(failedLogin(filter, "10.0.0.2")).isEqualTo(401);
    }

    @Test
    @DisplayName("X-Forwarded-For не се верува кога не стоиме зад proxy")
    void forwardedHeaderIsIgnoredByDefault() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        // Напаѓачот менува X-Forwarded-For на секое барање за да го измами броењето
        for (int attempt = 1; attempt <= 20; attempt++) {
            MockHttpServletRequest attacker = login("10.0.0.1");
            attacker.addHeader("X-Forwarded-For", "1.2.3." + attempt);
            filter.doFilter(attacker, new MockHttpServletResponse(), chainReturning(401));
        }

        MockHttpServletRequest last = login("10.0.0.1");
        last.addHeader("X-Forwarded-For", "1.2.3.99");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(last, blocked, chainReturning(401));

        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Регистрацијата се брои и кога успее — инаку е бесплатна")
    void registerCountsEverySuccess() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 20; attempt++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/auth/register", "10.0.0.1"), response, chainReturning(200));
            assertThat(response.getStatus()).as("регистрација " + attempt).isEqualTo(200);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("/api/auth/register", "10.0.0.1"), blocked, chainReturning(200));

        assertThat(blocked.getStatus()).isEqualTo(429);

        // ...а најавата од истата адреса сè уште поминува
        assertThat(failedLogin(filter, "10.0.0.1")).isEqualTo(401);
    }

    @Test
    @DisplayName("Останатите рути не ги допира")
    void otherPathsPassThrough() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false);

        for (int attempt = 1; attempt <= 50; attempt++) {
            MockHttpServletRequest any = request("/api/sessions", "10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(any, response, new MockFilterChain());

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
