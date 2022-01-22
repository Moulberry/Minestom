package net.minestom.server.extras.blockplacement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.Direction;

class BlockPlaceMechanicLava {

    static void onInteract(ItemStack itemStack, PlayerUseItemOnBlockEvent event) {
        Block block = event.getInstance().getBlock(event.getPosition());

        if (block.compare(Block.CAULDRON)) {
            event.getInstance().setBlock(event.getPosition(), Block.LAVA_CAULDRON);
            return;
        }

        Direction dir = event.getBlockFace().toDirection();
        Point pos = event.getPosition().add(dir.normalX(), dir.normalY(), dir.normalZ());
        block = event.getInstance().getBlock(pos);
        if (block.isAir()) {
            event.getInstance().setBlock(pos, Block.LAVA);
        }
    }

}
