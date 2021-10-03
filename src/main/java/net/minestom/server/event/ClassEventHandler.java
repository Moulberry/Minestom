package net.minestom.server.event;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class ClassEventHandler<E extends Event> {

    public static ClassEventHandler<?> EMPTY_HANDLER = new ClassEventHandler<>();

    private final BitSet bitSet;

    private ClassEventHandler() {
        this.bitSet = new BitSet();
    }

    private ClassEventHandler(int nodeID) {
        this();
        this.bitSet.set(nodeID);
    }

    /*package-private*/ boolean hasNodeID(int nodeId) {
        return bitSet.get(nodeId);
    }

    @ApiStatus.Internal
    private static final BitSet REGISTERED_NODE_IDS = new BitSet();
    private static final HashMap<Integer, EventNode<?>> registeredNodeMap = new HashMap<>();

    public static <E extends Event> ClassEventHandler<E> create(@NotNull EventFilter<E, ? extends ClassEventHandlerProvider> filter,
                                                   Consumer<EventNode<E>> consumer) {
        int nodeId = REGISTERED_NODE_IDS.nextClearBit(0);

        String name = "class-handler-" + UUID.randomUUID();
        EventNode<E> node = EventNode.classEventHandler(name, filter, nodeId);
        MinecraftServer.getGlobalEventHandler().addChild(node);

        registeredNodeMap.put(nodeId, node);

        consumer.accept(node);

        return new ClassEventHandler<>(nodeId);
    }

    /**
     * This method should be used in only specific circumstances.
     * Improper use of this will result in undefined behaviour
     *
     * WARNING: Destroys ALL ClassEventHandlers represented by {@param classEventHandler}
     * Calling this method on a ClassEventHandler created by {@link #combine(ClassEventHandler[])}
     * will result in the ORIGINAL handlers being destroyed
     *
     * Attempting to use a ClassEventHandler after it has been destroyed will result in undefined behaviour
     *
     * @param classEventHandler The handler(s) to destroy
     */
    public static void destroy(ClassEventHandler<?> classEventHandler) {
        int nodeId = classEventHandler.bitSet.nextSetBit(0);
        while (nodeId != -1) {
            EventNode<?> node = registeredNodeMap.get(nodeId);
            if (node != null) {
                MinecraftServer.getGlobalEventHandler().removeChild(node);
            }
            REGISTERED_NODE_IDS.set(nodeId, false);

            nodeId = classEventHandler.bitSet.nextSetBit(nodeId);
        }
    }

    @SafeVarargs
    public static <E extends Event> ClassEventHandler<E> combine(ClassEventHandler<E>... handlers) {
        ClassEventHandler<E> newHandler = new ClassEventHandler<>();
        for (ClassEventHandler<E> handler : handlers) {
            newHandler.bitSet.or(handler.bitSet);
        }
        return newHandler;
    }

}
