package com.mercadosolidario;

import com.mercadosolidario.security.ServiceRequestLookupRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRequestLookupRateLimiterTest {

    @Test
    void enforcesConfiguredPerAddressQuotaAndBoundedAddressCapacity() {
        ServiceRequestLookupRateLimiter limiter = new ServiceRequestLookupRateLimiter(2, 900, 1);
        MockHttpServletRequest firstAddress = requestFrom("192.0.2.1");
        MockHttpServletRequest secondAddress = requestFrom("192.0.2.2");

        assertThat(limiter.allow(firstAddress)).isTrue();
        assertThat(limiter.allow(firstAddress)).isTrue();
        assertThat(limiter.allow(firstAddress)).isFalse();
        assertThat(limiter.allow(secondAddress)).isFalse();
    }

    private MockHttpServletRequest requestFrom(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }
}
