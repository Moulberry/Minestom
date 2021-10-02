package net.minestom.server.event;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.function.Consumer;

public interface ClassListener {

    @ApiStatus.Internal
    BitSet INTERNAL_classListeners = new BitSet();

    static <E extends Event> BitSet createListener(@NotNull EventFilter<E, ? extends ClassListener> filter,
                                                Consumer<EventNode<E>> consumer) {
        int bit = INTERNAL_classListeners.nextClearBit(0);

        EventNode<E> node = EventNode.bit("class-listener", filter, bit);
        MinecraftServer.getGlobalEventHandler().addChild(node);

        consumer.accept(node);

        BitSet bitSet = new BitSet();
        bitSet.set(bit);
        return bitSet;
    }

    static BitSet combineListeners(BitSet listener1, BitSet listener2) {
        BitSet bitSet = new BitSet();
        bitSet.or(listener1);
        bitSet.or(listener2);
        return bitSet;
    }

    default boolean hasClassListenerID(int id) {
        return false;
    }

}
