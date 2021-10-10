package net.minestom.server.extras.blockplacement;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;

public class BlockPlaceMechanicRotation {

    public static void onPlace(Block block, PlayerBlockPlaceEvent event, boolean horizontalOnly, boolean usePlayerFacing, boolean invert) {
        block = event.getBlock();

        // Invert invert for block-face-placements
        invert = invert == usePlayerFacing;

        Vec playerDir = event.getPlayer().getPosition().direction();

        if(usePlayerFacing) {
            double absX = Math.abs(playerDir.x());
            double absY = Math.abs(playerDir.y());
            double absZ = Math.abs(playerDir.z());

            if(!horizontalOnly && absY > absX && absY > absZ) {
                if(playerDir.y() > 0 == invert) {
                    block = block.withProperty("facing", "down");
                } else {
                    block = block.withProperty("facing", "up");
                }
            } else if(absX > absZ) {
                if(playerDir.x() > 0 == invert) {
                    block = block.withProperty("facing", "west");
                } else {
                    block = block.withProperty("facing", "east");
                }
            } else {
                if(playerDir.z() > 0 == invert) {
                    block = block.withProperty("facing", "north");
                } else {
                    block = block.withProperty("facing", "south");
                }
            }
        } else {
            BlockFace face = event.getBlockFace();

            if(!invert) {
                face = face.getOppositeFace();
            }

            if(horizontalOnly && (face == BlockFace.BOTTOM || face == BlockFace.TOP)) {
                if(Math.abs(playerDir.x()) > Math.abs(playerDir.z())) {
                    if(playerDir.x() > 0 == invert) {
                        block = block.withProperty("facing", "west");
                    } else {
                        block = block.withProperty("facing", "east");
                    }
                } else {
                    if(playerDir.z() > 0 == invert) {
                        block = block.withProperty("facing", "north");
                    } else {
                        block = block.withProperty("facing", "south");
                    }
                }
                event.setBlock(block);
                return;
            }

            String faceName = face.name().toLowerCase();
            if(face == BlockFace.BOTTOM) faceName = "down";
            if(face == BlockFace.TOP) faceName = "up";

            block = block.withProperty("facing", faceName);
        }

        event.setBlock(block);
    }

}
