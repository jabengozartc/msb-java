package io.github.tcdl.msb.config.amqp;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.tcdl.msb.api.exception.ConfigurationException;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AmqpBrokerConfigTest {

    final String charsetName = "UTF-8";
    final String host = "127.0.0.1";
    final int port = 5672;
    final String username = "user";
    final String password = "pwd";
    final String virtualHost = "127.10.10.10";
    final boolean useSSL = false;
    final String groupId = "msb-java";
    final boolean durable = false;
    final int consumerThreadPoolSize = 5;
    final int consumerThreadPoolQueueCapacity = 20;
    final boolean requeueRejectedMessages = true;

    @Test
    public void testBuildAmqpBrokerConfig() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        Config amqpConfig = ConfigFactory.parseString(configStr).getConfig("config.amqp");
        AmqpBrokerConfig.AmqpBrokerConfigBuilder brokerConfigBuilder = new AmqpBrokerConfig.AmqpBrokerConfigBuilder();
        AmqpBrokerConfig brokerConfig = brokerConfigBuilder.withConfig(amqpConfig).build();

        assertEquals(brokerConfig.getCharset(), Charset.forName(charsetName));
        assertEquals(brokerConfig.getHost(), host);
        assertEquals(brokerConfig.getPort(), port);
        assertEquals(brokerConfig.getGroupId().get(), groupId);
        assertEquals(brokerConfig.isDurable(), durable);
        assertEquals(brokerConfig.getConsumerThreadPoolSize(), consumerThreadPoolSize);
        assertEquals(brokerConfig.getConsumerThreadPoolQueueCapacity(), consumerThreadPoolQueueCapacity);
        assertEquals(brokerConfig.isRequeueRejectedMessages(), requeueRejectedMessages);

        assertEquals(brokerConfig.getUsername().get(), username);
        assertEquals(brokerConfig.getPassword().get(), password);
        assertEquals(brokerConfig.getVirtualHost().get(), virtualHost);
        assertFalse(brokerConfig.useSSL());
    }

    @Test
    public void testOptionalConfigurationOptions() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        Config amqpConfig = ConfigFactory.parseString(configStr).getConfig("config.amqp");
        
        AmqpBrokerConfig.AmqpBrokerConfigBuilder brokerConfigBuilder = new AmqpBrokerConfig.AmqpBrokerConfigBuilder();
        AmqpBrokerConfig brokerConfig = brokerConfigBuilder.withConfig(amqpConfig).build();

        //Verify empty optional values
        assertFalse(brokerConfig.getUsername().isPresent());
        assertFalse(brokerConfig.getPassword().isPresent());
        assertFalse(brokerConfig.getVirtualHost().isPresent());
        assertFalse(brokerConfig.getGroupId().isPresent());
    }

    @Test
    public void testHostConfigurationOption() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "host");
    }

    @Test
    public void testPortConfigurationOption() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "port");
    }

    @Test
    public void testDurableConfigurationOption() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "durable");
    }

    @Test
    public void testConsumerThreadPoolSizeConfigurationOption() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "consumerThreadPoolSize");
    }

    @Test
    public void testConsumerThreadPoolQueueCapacityConfigurationOption() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "consumerThreadPoolQueueCapacity");
    }

    @Test
    public void testCharsetConfigurationOption() {
        String configStr = "config.amqp {"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + " requeueRejectedMessages = " + requeueRejectedMessages + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "charsetName");
    }

    @Test(expected = ConfigurationException.class)
    public void testInvalidCharset() {
        String invalidCharset = "blah";

        String configStr = "config.amqp {"
                + " charsetName = \"" + invalidCharset + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + "}";

        AmqpBrokerConfig.AmqpBrokerConfigBuilder builder = createConfigBuilder(configStr);
        builder.build();
    }

    @Test
    public void testUseSSLConfigurationOption() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "useSSL");
    }

    @Test
    public void testRequeueRejectedMessagesOption() {
        String configStr = "config.amqp {"
                + " charsetName = \"" + charsetName + "\"\n"
                + " host = \"" + host + "\"\n"
                + " port = \"" + port + "\"\n"
                + " username = \"" + username + "\"\n"
                + " password = \"" + password + "\"\n"
                + " virtualHost = \"" + virtualHost + "\"\n"
                + " useSSL = \"" + useSSL + "\"\n"
                + " groupId = \"" + groupId + "\"\n"
                + " durable = " + durable + "\n"
                + " consumerThreadPoolSize = " + consumerThreadPoolSize + "\n"
                + " consumerThreadPoolQueueCapacity = " + consumerThreadPoolQueueCapacity + "\n"
                + "}";

        testMandatoryConfigurationOption(configStr, "requeueRejectedMessages");
    }

    private void testMandatoryConfigurationOption(String configStr, String path) {
        try {
            AmqpBrokerConfig.AmqpBrokerConfigBuilder builder = createConfigBuilder(configStr);
            builder.build();
            fail(String.format("Created AmqpBrokerConfig without Mandatory Configuration Option '%s'!", path));

        } catch (ConfigurationException expected) {
            assertTrue(String.format("Exception message doesn't mention '%s'?!", path),
                    expected.getMessage().contains(path));
        }
    }

    private AmqpBrokerConfig.AmqpBrokerConfigBuilder createConfigBuilder(String configStr) {
        Config amqpConfig = ConfigFactory.parseString(configStr).getConfig("config.amqp");
        return new AmqpBrokerConfig.AmqpBrokerConfigBuilder().withConfig(amqpConfig);
    }

}
