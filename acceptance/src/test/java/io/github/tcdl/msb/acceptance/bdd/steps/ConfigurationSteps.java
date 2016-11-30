package io.github.tcdl.msb.acceptance.bdd.steps;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.github.tcdl.msb.acceptance.MsbTestHelper;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Steps to manipulate with MSB configuration
 */
public class ConfigurationSteps {

    protected final MsbTestHelper helper = MsbTestHelper.getInstance();

    private String MSB_CONFIG_ROOT = "msbConfig";
    private String VALIDATE_MESSAGE = MSB_CONFIG_ROOT + ".validateMessage";

    private String MSB_BROKER_CONFIG_ROOT = "msbConfig.brokerConfig";
    private String MSB_THREADING_CONFIG_ROOT = "msbConfig.threadingConfig";
    private String MSB_BROKER_CONSUMER_THREAD_POOL_SIZE = MSB_THREADING_CONFIG_ROOT + ".consumerThreadPoolSize";
    private String MSB_BROKER_CONSUMER_THREAD_POOL_PREFETCH_COUNT = MSB_BROKER_CONFIG_ROOT + ".prefetchCount";

    private Config config = ConfigFactory.load();

    @Given("MSB configuration with validate message $validate")
    public void initWithValidateMessage(boolean validate) {
        config = config.withValue(VALIDATE_MESSAGE, ConfigValueFactory.fromAnyRef(validate));
    }

    @Given("MSB configuration with consumer thread pool size $size")
    public void initWithConsumerThreadPoolSize(int size) {
        config = config.withValue(MSB_BROKER_CONSUMER_THREAD_POOL_SIZE, ConfigValueFactory.fromAnyRef(size));
    }

    @Given("MSB configuration with consumer prefetch count $count")
    public void initWithConsumerPrefetchCount(int capacity) {
        config = config.withValue(MSB_BROKER_CONSUMER_THREAD_POOL_PREFETCH_COUNT, ConfigValueFactory.fromAnyRef(capacity));
    }


    @Given("start MSB")
    public void initMSB() {
        helper.initWithConfig(config);
    }

    @Then("shutdown MSB")
    public void shutdownMSB() {
        helper.shutdown();
    }

}

