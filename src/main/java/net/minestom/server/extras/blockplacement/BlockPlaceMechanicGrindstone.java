package net.minestom.server.extras.blockplacement;

import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;

class BlockPlaceMechanicGrindstone {

    static void onPlace(Block block, PlayerBlockPlaceEvent event) {
        block = event.getBlock();
        Direction dir = event.getBlockFace().toDirection();
        event.setBlock(switch (dir) {
            case UP -> block.withProperty("face", "floor");
            case DOWN -> block.withProperty("face", "ceiling");
            case NORTH, SOUTH, WEST, EAST -> block.withProperty("face", "wall");
        });
    }

}
