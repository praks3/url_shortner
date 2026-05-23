package com.url_shortener.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitProperties {

    private Tier publicTier = new Tier(30);
    private Tier authenticated = new Tier(200);

    public Tier getPublic() { return publicTier; }
    public void setPublic(Tier t) { this.publicTier = t; }

    public Tier getAuthenticated() { return authenticated; }
    public void setAuthenticated(Tier t) { this.authenticated = t; }

    public static class Tier {
        private int requestsPerMinute;

        public Tier() {}
        public Tier(int rpm) { this.requestsPerMinute = rpm; }

        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int rpm) { this.requestsPerMinute = rpm; }
    }
}
