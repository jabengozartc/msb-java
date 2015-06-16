package io.github.tcdl.config.amqp;

import java.util.Optional;

import io.github.tcdl.config.ConfigurationUtil;
import io.github.tcdl.exception.ConfigurationException;

import com.typesafe.config.Config;

import static io.github.tcdl.config.ConfigurationUtil.getString;

public class AmqpBrokerConfig {
    
    private final int port;
    private final String host;
    private Optional<String> username;
    private Optional<String> password;
    private Optional<String> virtualHost;

    private String groupId;
    private final boolean durable;
    private final int consumerThreadPoolSize;

    public AmqpBrokerConfig(String host, int port, 
            Optional<String> username, Optional<String> password, Optional<String> virtualHost,
            String groupId, boolean durable, int consumerThreadPoolSize) {
        this.port = port;
        this.host = host;
        this.username = username;
        this.password = password;        
        this.virtualHost = virtualHost;
        this.groupId = groupId;
        this.durable = durable;
        this.consumerThreadPoolSize = consumerThreadPoolSize;
    }

    public static class AmqpBrokerConfigBuilder {
        private int port;
        private String host;
        private Optional<String> username;
        private Optional<String> password;
        private Optional<String> virtualHost;
        private String groupId;
        private boolean durable;
        private int consumerThreadPoolSize;

        public AmqpBrokerConfigBuilder(Config config) throws ConfigurationException {
            
            this.host = ConfigurationUtil.getString(config, "host");
            this.port = ConfigurationUtil.getInt(config, "port");

            this.username = ConfigurationUtil.getOptionalString(config, "username");
            this.password = ConfigurationUtil.getOptionalString(config, "password");
            this.virtualHost = ConfigurationUtil.getOptionalString(config, "virtualHost");
            
            this.groupId = ConfigurationUtil.getString(config, "groupId");
            this.durable = ConfigurationUtil.getBoolean(config, "durable");
            this.consumerThreadPoolSize = ConfigurationUtil.getInt(config, "consumerThreadPoolSize");
       }

        public AmqpBrokerConfig build() throws ConfigurationException {
            return new AmqpBrokerConfig(host, port, username, password, virtualHost, 
                    groupId, durable, consumerThreadPoolSize);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Optional<String> getUsername() {
        return username;
    }

    public Optional<String> getPassword() {
        return password;
    }

    public Optional<String> getVirtualHost() {
        return virtualHost;
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getConsumerThreadPoolSize() {
        return consumerThreadPoolSize;
    }

    @Override
    public String toString() {
        return String.format("AmqpBrokerConfig [host=%s, port=%d, username=%s, password=%s, virtualHost=%s, groupId=%s, durable=%s, consumerThreadPoolSize=%s]", 
                host, port, username, password, virtualHost, groupId, durable, consumerThreadPoolSize);
    }

}