package net.minestom.server.event;

import org.jetbrains.annotations.NotNull;

public interface ClassEventHandlerProvider {

    default @NotNull ClassEventHandler<?> getClassEventHandler() {
        return ClassEventHandler.EMPTY_HANDLER;
    }

}
