package rdhxb.TrustCheck.crbr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crbr")
public record CrbrProperties(
        String endpoint,
        int connectTimeout,
        int requestTimeout
) {}
