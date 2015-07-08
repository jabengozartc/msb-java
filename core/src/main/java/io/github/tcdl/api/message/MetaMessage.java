package io.github.tcdl.api.message;

import io.github.tcdl.config.ServiceDetails;

import java.time.Clock;
import java.time.Instant;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by rdro on 4/22/2015.
 */
public final class MetaMessage {

    private final Integer ttl;
    private final Instant createdAt;
    private final Long durationMs;
    private final ServiceDetails serviceDetails;

    private MetaMessage(@JsonProperty("ttl") Integer ttl, @JsonProperty("createdAt") Instant createdAt, @JsonProperty("durationMs") Long durationMs,
            @JsonProperty("serviceDetails") ServiceDetails serviceDetails) {
        Validate.notNull(createdAt, "the 'createdAt' must not be null");
        Validate.notNull(durationMs, "the 'durationMs' must not be null");
        Validate.notNull(serviceDetails, "the 'serviceDetails' must not be null");
        this.ttl = ttl;
        this.createdAt = createdAt;
        this.durationMs = durationMs;
        this.serviceDetails = serviceDetails;
    }

    public static class Builder {
        private Integer ttl;
        private Instant createdAt;
        private ServiceDetails serviceDetails;
        private Clock clock;

        public Builder(Integer ttl, Instant createdAt, ServiceDetails serviceDetails, Clock clock) {
            this.ttl = ttl;
            this.createdAt = createdAt;
            this.serviceDetails = serviceDetails;
            this.clock = clock;
        }     

        public MetaMessage build() {
            Long durationMs = clock.instant().toEpochMilli() - this.createdAt.toEpochMilli();
            return new MetaMessage(ttl, createdAt, durationMs, serviceDetails);
        }
    }

    public Integer getTtl() {
        return ttl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public ServiceDetails getServiceDetails() {
        return serviceDetails;
    }

    @Override
    public String toString() {
        return "MetaMessage [ttl=" + ttl + ", createdAt=" + createdAt + ", durationMs=" + durationMs + ", serviceDetails=" + serviceDetails + "]";
    }    
}