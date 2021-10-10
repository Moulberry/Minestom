package net.minestom.server.extras.blockplacement;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventBinding;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerBlockUpdateNeighborEvent;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;

import java.util.HashSet;
import java.util.Set;

public final class PlacementRules {

    /* Data Definition (Note: Initialized at end of file) */

    private static Set<NamespaceID> ROTATION_INVERT;
    private static Set<NamespaceID> USE_BLOCK_FACING;
    private static final Set<Integer> ROTATION_VERTICAL = new HashSet<>();

    /* Filters */

    private static final EventBinding<BlockEvent> STAIRS_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isStairs)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicStairShape::onPlace)
            .map(PlayerBlockUpdateNeighborEvent.class, BlockPlaceMechanicStairShape::onNeighbor)
            .build();

    private static final EventBinding<BlockEvent> WALLS_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isWall)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicWall::onPlace)
            .map(PlayerBlockUpdateNeighborEvent.class, BlockPlaceMechanicWall::onNeighbor)
            .build();

    private static final EventBinding<BlockEvent> SLAB_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isSlab)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicSlab::onPlace)
            .build();

    private static final EventBinding<BlockEvent> CHEST_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isChest)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicChestType::onPlace)
            .map(PlayerBlockUpdateNeighborEvent.class, BlockPlaceMechanicChestType::onNeighbor)
            .build();

    private static final EventBinding<BlockEvent> FENCE_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isFence)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicFence::onPlace)
            .map(PlayerBlockUpdateNeighborEvent.class, BlockPlaceMechanicFence::onNeighbor)
            .build();

    private static final EventBinding<BlockEvent> POINTED_DRIPSTONE_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isPointedDripstone)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicPointedDripstone::onPlace)
            .map(PlayerBlockUpdateNeighborEvent.class, BlockPlaceMechanicPointedDripstone::onNeighbor)
            .build();

    private static final EventBinding<BlockEvent> GLOW_LICHEN_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isGlowLichen)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicGlowLichen::onPlace)
            .build();

    private static final EventBinding<BlockEvent> VINE_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isVine)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicVine::onPlace)
            .build();

    private static final EventBinding<BlockEvent> LEVER_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::isLever)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicLever::onPlace)
            .build();

    private static final EventBinding<BlockEvent> ROTATION_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::hasRotation)
            .map(PlayerBlockPlaceEvent.class, (block, event) -> {
                BlockPlaceMechanicRotation.onPlace(block, event,
                        !ROTATION_VERTICAL.contains(event.getBlock().id()),
                        !USE_BLOCK_FACING.contains(event.getBlock().namespace()),
                        ROTATION_INVERT.contains(event.getBlock().namespace()));
            })
            .build();

    private static final EventBinding<BlockEvent> AXIS_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::hasAxis)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicAxis::onPlace)
            .build();

    private static final EventBinding<BlockEvent> HALF_BINDING = EventBinding.filtered(EventFilter.BLOCK, PlacementRules::hasHalf)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicHalf::onPlace)
            .build();

    private static final EventBinding<BlockEvent> WALL_REPLACEMENT_BINDING =
            EventBinding.filtered(EventFilter.BLOCK, BlockPlaceMechanicWallReplacement::shouldReplace)
            .map(PlayerBlockPlaceEvent.class, BlockPlaceMechanicWallReplacement::onPlace)
            .build();

    /* Checks */

    private static final NamespaceID MINECRAFT_STAIRS = NamespaceID.from("minecraft:stairs");
    private static boolean isStairs(Block block) {
        return block.getMinecraftTags().contains(MINECRAFT_STAIRS);
    }

    private static final NamespaceID MINECRAFT_WALLS = NamespaceID.from("minecraft:walls");
    private static boolean isWall(Block block) {
        return block.getMinecraftTags().contains(MINECRAFT_WALLS);
    }

    private static final NamespaceID MINECRAFT_SLABS = NamespaceID.from("minecraft:slabs");
    private static boolean isSlab(Block block) {
        return block.getMinecraftTags().contains(MINECRAFT_SLABS);
    }

    private static boolean isChest(Block block) {
        return block.compare(Block.CHEST) || block.compare(Block.TRAPPED_CHEST);
    }

    private static final NamespaceID MINECRAFT_FENCES = NamespaceID.from("minecraft:fences");
    private static boolean isFence(Block block) {
        return block.getMinecraftTags().contains(MINECRAFT_FENCES);
    }

    private static boolean isPointedDripstone(Block block) {
        return block.compare(Block.POINTED_DRIPSTONE);
    }

    private static boolean isGlowLichen(Block block) {
        return block.compare(Block.GLOW_LICHEN);
    }

    private static boolean isVine(Block block) {
        return block.compare(Block.VINE);
    }

    private static boolean isLever(Block block) {
        return block.compare(Block.LEVER);
    }

    private static boolean hasRotation(Block block) {
        return block.getProperty("facing") != null;
    }

    private static boolean hasAxis(Block block) {
        return block.getProperty("axis") != null;
    }

    private static boolean hasHalf(Block block) {
        return block.getProperty("half") != null;
    }

    /* Init */

	public static void init() {
        // Replacements
        MinecraftServer.getGlobalEventHandler().register(WALL_REPLACEMENT_BINDING);

        // Blockstates
        MinecraftServer.getGlobalEventHandler().register(ROTATION_BINDING);
        MinecraftServer.getGlobalEventHandler().register(AXIS_BINDING);
        MinecraftServer.getGlobalEventHandler().register(HALF_BINDING);

        // Specific blocks
        MinecraftServer.getGlobalEventHandler().register(STAIRS_BINDING);
        MinecraftServer.getGlobalEventHandler().register(WALLS_BINDING);
        MinecraftServer.getGlobalEventHandler().register(SLAB_BINDING);
        MinecraftServer.getGlobalEventHandler().register(CHEST_BINDING);
        MinecraftServer.getGlobalEventHandler().register(FENCE_BINDING);
        MinecraftServer.getGlobalEventHandler().register(GLOW_LICHEN_BINDING);
        MinecraftServer.getGlobalEventHandler().register(VINE_BINDING);
        MinecraftServer.getGlobalEventHandler().register(LEVER_BINDING);
        MinecraftServer.getGlobalEventHandler().register(POINTED_DRIPSTONE_BINDING);

        for(short stateId=0; stateId<32767; stateId++) {
            Block block = Block.fromStateId(stateId);
            if(block == null) continue;

            if(block.getProperty("facing") != null) {
                for(String direction : block.properties().values()) {
                    if(direction.equals("up") || direction.equals("down")) {
                        ROTATION_VERTICAL.add(block.id());
                    }
                }
            }

            if(block.getMinecraftTags().contains(NamespaceID.from("minecraft:wall_signs"))) {
                USE_BLOCK_FACING.add(block.namespace());
            }
        }
	}

    /* Data */

    static {
        ROTATION_INVERT = Set.of(
                NamespaceID.from("minecraft:barrel"),
                NamespaceID.from("minecraft:command_block"),
                NamespaceID.from("minecraft:repeating_command_block"),
                NamespaceID.from("minecraft:chain_command_block"),
                NamespaceID.from("minecraft:dispenser"),
                NamespaceID.from("minecraft:dropper"),
                NamespaceID.from("minecraft:chest"),
                NamespaceID.from("minecraft:trapped_chest"),
                NamespaceID.from("minecraft:observer"),
                NamespaceID.from("minecraft:beehive"),
                NamespaceID.from("minecraft:bee_nest"),
                NamespaceID.from("minecraft:piston")
        );
        USE_BLOCK_FACING = new HashSet<>(Set.of(
                NamespaceID.from("minecraft:glow_lichen"),
                NamespaceID.from("minecraft:cocoa"),
                NamespaceID.from("minecraft:dead_tube_coral_wall_fan"),
                NamespaceID.from("minecraft:dead_brain_coral_wall_fan"),
                NamespaceID.from("minecraft:dead_bubble_coral_wall_fan"),
                NamespaceID.from("minecraft:dead_fire_coral_wall_fan"),
                NamespaceID.from("minecraft:dead_horn_coral_wall_fan"),
                NamespaceID.from("minecraft:tube_coral_wall_fan"),
                NamespaceID.from("minecraft:brain_coral_wall_fan"),
                NamespaceID.from("minecraft:bubble_coral_wall_fan"),
                NamespaceID.from("minecraft:fire_coral_wall_fan"),
                NamespaceID.from("minecraft:horn_coral_wall_fan"),
                NamespaceID.from("minecraft:ladder"),
                NamespaceID.from("minecraft:tripwire_hook"),
                NamespaceID.from("minecraft:vine"),
                NamespaceID.from("minecraft:wall_torch"),
                NamespaceID.from("minecraft:redstone_wall_torch"),
                NamespaceID.from("minecraft:soul_wall_torch"),
                NamespaceID.from("minecraft:skeleton_wall_skull"),
                NamespaceID.from("minecraft:wither_skeleton_wall_skull"),
                NamespaceID.from("minecraft:zombie_wall_head"),
                NamespaceID.from("minecraft:player_wall_head"),
                NamespaceID.from("minecraft:creeper_wall_head"),
                NamespaceID.from("minecraft:dragon_wall_head")
        ));
    }

}
