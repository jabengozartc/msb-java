package io.github.tcdl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.tcdl.config.MsbConfigurations;
import io.github.tcdl.messages.MessageFactory;

/**
 * Contains all singletone beans required for MSB
 *
 * Created by rdro on 5/27/2015.
 */
public class MsbContext {

    private MsbConfigurations msbConfig;
    private MessageFactory messageFactory;
    private ChannelManager channelManager;

    public MsbContext(MsbConfigurations msbConfig, MessageFactory messageFactory, ChannelManager channelManager) {
        this.msbConfig = msbConfig;
        this.messageFactory = messageFactory;
        this.channelManager = channelManager;
    }

    public MsbConfigurations getMsbConfig() {
        return msbConfig;
    }

    public void setMsbConfig(MsbConfigurations msbConfig) {
        this.msbConfig = msbConfig;
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public void setMessageFactory(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public static class MsbContextBuilder {
        public MsbContext build() {
            Config config = ConfigFactory.load();
            MsbConfigurations msbConfig = new MsbConfigurations(config);
            ChannelManager channelManager = new ChannelManager(msbConfig);
            MessageFactory messageFactory = new MessageFactory(msbConfig.getServiceDetails());

            return new MsbContext(msbConfig, messageFactory, channelManager);
        }
    }
}
