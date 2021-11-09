package net.minestom.server.event.server;

import net.minestom.server.event.Event;
import net.minestom.server.network.LoginPluginProcessor;

import java.util.Map;

public class LoginPluginMessageEvent implements Event {

    private Map<LoginPluginProcessor.LoginPluginRequest, LoginPluginProcessor.LoginPluginHandler> handlerMap;

    public LoginPluginMessageEvent(Map<LoginPluginProcessor.LoginPluginRequest, LoginPluginProcessor.LoginPluginHandler> handlerMap) {
        this.handlerMap = handlerMap;
    }

    public void register(String channel, byte[] data, LoginPluginProcessor.LoginPluginHandler responseHandler) {
        handlerMap.put(new LoginPluginProcessor.LoginPluginRequest(channel, data), responseHandler);
    }

}
