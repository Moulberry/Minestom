package net.minestom.server.network.packet.client.login;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.LoginPluginProcessor;
import net.minestom.server.network.packet.client.ClientPreplayPacket;
import net.minestom.server.network.packet.server.login.LoginDisconnectPacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.network.player.PlayerSocketConnection;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class LoginPluginResponsePacket implements ClientPreplayPacket {
    private final static ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();
    public static final Component INVALID_PROXY_RESPONSE = Component.text("Invalid proxy response!", NamedTextColor.RED);

    public int messageId;
    public boolean successful;
    public byte[] data = new byte[0];

    @Override
    public void process(@NotNull PlayerConnection connection) {
        // Proxy support

        if (connection instanceof PlayerSocketConnection socketConnection) {
            LoginPluginProcessor processor = socketConnection.getLoginPluginProcessor();
            if (processor != null) {
                processor.process(this);
            }
        }
    }

    @Override
    public void read(@NotNull BinaryReader reader) {
        this.messageId = reader.readVarInt();
        this.successful = reader.readBoolean();
        if (successful) {
            this.data = reader.readRemainingBytes();
        }
    }

    @Override
    public void write(@NotNull BinaryWriter writer) {
        writer.writeVarInt(messageId);
        writer.writeBoolean(successful);
        if (successful) {
            writer.writeBytes(data);
        }
    }
}
