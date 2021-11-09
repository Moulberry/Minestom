package net.minestom.server.network.packet.client.login;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.server.LoginPluginMessageEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.LoginPluginProcessor;
import net.minestom.server.network.packet.client.ClientPreplayPacket;
import net.minestom.server.network.packet.server.login.EncryptionRequestPacket;
import net.minestom.server.network.packet.server.login.LoginDisconnectPacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.network.player.PlayerSocketConnection;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginStartPacket implements ClientPreplayPacket {

    private static final Component ALREADY_CONNECTED = Component.text("You are already on this server", NamedTextColor.RED);

    public String username = "";

    @Override
    public void process(@NotNull PlayerConnection connection) {
        final boolean isSocketConnection = connection instanceof PlayerSocketConnection;
        // Cache the login username and start compression if enabled
        if (isSocketConnection) {
            PlayerSocketConnection socketConnection = (PlayerSocketConnection) connection;
            socketConnection.UNSAFE_setLoginUsername(username);

            // Compression
            final int threshold = MinecraftServer.getCompressionThreshold();
            if (threshold > 0) {
                socketConnection.startCompression();
            }
        }
        // Proxy support (only for socket clients)
        if (isSocketConnection) {
            final PlayerSocketConnection socketConnection = (PlayerSocketConnection) connection;

            Map<LoginPluginProcessor.LoginPluginRequest, LoginPluginProcessor.LoginPluginHandler> handlerMap =
                    new HashMap<>();

            // Velocity support
            if (VelocityProxy.isEnabled()) {
                // This would be updated in an event
                handlerMap.put(new LoginPluginProcessor.LoginPluginRequest(VelocityProxy.PLAYER_INFO_CHANNEL,
                        null), (packet, data) -> {
                    if (packet.data != null && packet.data.length > 0) {
                        BinaryReader reader = new BinaryReader(packet.data);
                        if (VelocityProxy.checkIntegrity(reader)) {
                            // Get the real connection address
                            final InetAddress address = VelocityProxy.readAddress(reader);
                            final int port = ((java.net.InetSocketAddress) connection.getRemoteAddress()).getPort();
                            data.socketAddress = new InetSocketAddress(address, port);

                            data.playerUuid = reader.readUuid();
                            data.playerUsername = reader.readSizedString(16);

                            data.playerSkin = VelocityProxy.readSkin(reader);

                            data.doCustomAuth = true;
                        }
                    }
                });
            }

            LoginPluginMessageEvent loginPluginMessageEvent = new LoginPluginMessageEvent(handlerMap);
            EventDispatcher.call(loginPluginMessageEvent);

            if (!handlerMap.isEmpty()) {
                socketConnection.setLoginPluginProcessor(LoginPluginProcessor.create(socketConnection, handlerMap));
                CONNECTION_MANAGER.registerWaitingLogin(socketConnection);
                return;
            }
        }

        if (MojangAuth.isEnabled() && isSocketConnection) {
            // Mojang auth
            if (CONNECTION_MANAGER.getPlayer(username) != null) {
                connection.sendPacket(new LoginDisconnectPacket(ALREADY_CONNECTED));
                connection.disconnect();
                return;
            }
            final PlayerSocketConnection socketConnection = (PlayerSocketConnection) connection;
            socketConnection.setConnectionState(ConnectionState.LOGIN);
            EncryptionRequestPacket encryptionRequestPacket = new EncryptionRequestPacket(socketConnection);
            socketConnection.sendPacket(encryptionRequestPacket);
        } else {
            final boolean bungee = BungeeCordProxy.isEnabled();
            // Offline
            final UUID playerUuid = bungee && isSocketConnection ?
                    ((PlayerSocketConnection) connection).getBungeeUuid() :
                    CONNECTION_MANAGER.getPlayerConnectionUuid(connection, username);

            Player player = CONNECTION_MANAGER.startPlayState(connection, playerUuid, username, true);
            if (bungee && isSocketConnection) {
                player.setSkin(((PlayerSocketConnection) connection).getBungeeSkin());
            }
        }
    }

    @Override
    public void read(@NotNull BinaryReader reader) {
        this.username = reader.readSizedString(16);
    }

    @Override
    public void write(@NotNull BinaryWriter writer) {
        if (username.length() > 16)
            throw new IllegalArgumentException("Username is not allowed to be longer than 16 characters");
        writer.writeSizedString(username);
    }
}
