package net.minestom.server.listener;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;

public class UseEntityListener {

    public static void useEntityListener(ClientInteractEntityPacket packet, Player player) {
        Entity entity = Entity.getEntity(packet.targetId);
        if (entity == null)
            return;
        ClientInteractEntityPacket.Type type = packet.type;

        // Player cannot interact with entities he cannot see
        if (!entity.isViewer(player))
            return;

        if (type == ClientInteractEntityPacket.Type.ATTACK) {
            if (entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) // Can't attack dead entities
                return;

            EntityAttackEvent entityAttackEvent = new EntityAttackEvent(player, entity);
            player.callEvent(EntityAttackEvent.class, entityAttackEvent);
        } else if (type == ClientInteractEntityPacket.Type.INTERACT) {
            PlayerEntityInteractEvent playerEntityInteractEvent = new PlayerEntityInteractEvent(player, entity, packet.hand);
            player.callEvent(PlayerEntityInteractEvent.class, playerEntityInteractEvent);
        } else {
            // TODO find difference with INTERACT
            PlayerEntityInteractEvent playerEntityInteractEvent = new PlayerEntityInteractEvent(player, entity, packet.hand);
            player.callEvent(PlayerEntityInteractEvent.class, playerEntityInteractEvent);
        }
    }

}
