package net.minestom.server.network;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.network.packet.client.login.LoginPluginResponsePacket;
import net.minestom.server.network.packet.server.login.EncryptionRequestPacket;
import net.minestom.server.network.packet.server.login.LoginPluginRequestPacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.network.player.PlayerSocketConnection;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class LoginPluginProcessor {

    private final static ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final PlayerSocketConnection socketConnection;
    private final LoginPluginData data = new LoginPluginData();

    private final Map<Integer, LoginPluginHandler> waitingResponses;

    private LoginPluginProcessor(PlayerSocketConnection socketConnection,
                                 Map<Integer, LoginPluginHandler> waitingResponses) {
        this.socketConnection = socketConnection;
        this.waitingResponses = waitingResponses;
    }

    public static LoginPluginProcessor create(PlayerSocketConnection socketConnection,
                              Map<LoginPluginRequest, LoginPluginHandler> handlerMap) {
        int messageId = 0;

        Map<Integer, LoginPluginHandler> waitingResponses = new HashMap<>();

        for (Map.Entry<LoginPluginRequest, LoginPluginHandler> entry : handlerMap.entrySet()) {
            int id = messageId++;

            waitingResponses.put(id, entry.getValue());

            LoginPluginRequestPacket loginPluginRequestPacket = new LoginPluginRequestPacket();
            loginPluginRequestPacket.messageId = id;
            loginPluginRequestPacket.channel = entry.getKey().channel();
            loginPluginRequestPacket.data = entry.getKey().data();
            socketConnection.sendPacket(loginPluginRequestPacket);
        }

        return new LoginPluginProcessor(socketConnection, waitingResponses);
    }

    public void process(LoginPluginResponsePacket responsePacket) {
        LoginPluginHandler handler = waitingResponses.remove(responsePacket.messageId);
        if (handler != null) {
            handler.handle(responsePacket, socketConnection, data);
        }
    }

    // Called by ConnectionManager#updateWaitingLogins
    boolean checkFinished() {
        if(waitingResponses.isEmpty()) {
            finish();
            return true;
        }
        return false;
    }

    private void finish() {
        if (!data.doCustomAuth) {
            if (MojangAuth.isEnabled()) {
                EncryptionRequestPacket encryptionRequestPacket = new EncryptionRequestPacket(socketConnection);
                socketConnection.sendPacket(encryptionRequestPacket);
            }
            return;
        }

        if (data.socketAddress != null) {
            socketConnection.setRemoteAddress(data.socketAddress);
        }
        if (data.playerUsername != null) {
            socketConnection.UNSAFE_setLoginUsername(data.playerUsername);
        }

        final boolean bungee = BungeeCordProxy.isEnabled();

        final String username = socketConnection.getLoginUsername();
        UUID uuid = data.playerUuid;
        if (uuid == null && bungee) uuid = socketConnection.getBungeeUuid();
        if (uuid == null) uuid = CONNECTION_MANAGER.getPlayerConnectionUuid(socketConnection, username);

        Player player = CONNECTION_MANAGER.startPlayState(socketConnection, uuid, username, true);

        if (data.playerSkin != null) {
            player.setSkin(data.playerSkin);
        }
    }

    public static class LoginPluginData {
        public SocketAddress socketAddress = null;
        public UUID playerUuid = null;
        public String playerUsername = null;
        public PlayerSkin playerSkin = null;
        public boolean doCustomAuth = false;
    }

    public static record LoginPluginRequest(@NotNull String channel, byte[] data) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoginPluginRequest that = (LoginPluginRequest) o;
            return channel.equals(that.channel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(channel);
        }
    }

    public interface LoginPluginHandler {
        void handle(LoginPluginResponsePacket packet, PlayerSocketConnection connection, LoginPluginData loginData);
    }

}
