package com.hccommanditem.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * An interaction that executes a chat command when triggered.
 * Based on PyKTeleportToRespawnInteraction pattern.
 *
 * Usage in JSON:
 * {
 *   "Type": "ExecuteCommand",
 *   "Command": "help"
 * }
 */
public class ExecuteCommandInteraction extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Simple CODEC with Command field - matching decompiled code style (raw types)
    public static final BuilderCodec CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(
            ExecuteCommandInteraction.class,
            ExecuteCommandInteraction::new,
            (BuilderCodec)SimpleInstantInteraction.CODEC)
        .documentation("Executes a chat command when the item is used."))
        .appendInherited(new KeyedCodec("Command", Codec.STRING),
            (data, o) -> ((ExecuteCommandInteraction)data).command = (String)o,
            data -> ((ExecuteCommandInteraction)data).command,
            (data, parent) -> ((ExecuteCommandInteraction)data).command = ((ExecuteCommandInteraction)parent).command)
        .add()).build();

    protected String command;

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        LOGGER.at(Level.FINE).log("[HC_CommandItem] ExecuteCommandInteraction.firstRun called with command: " + command);

        Ref entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            LOGGER.at(Level.WARNING).log("[HC_CommandItem] Entity ref is null or invalid");
            return;
        }

        CommandBuffer buffer = context.getCommandBuffer();
        if (buffer == null) {
            LOGGER.at(Level.WARNING).log("[HC_CommandItem] Command buffer is null");
            return;
        }

        Player player = (Player)buffer.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            LOGGER.at(Level.WARNING).log("[HC_CommandItem] Player component is null");
            return;
        }

        if (command == null || command.isEmpty()) {
            LOGGER.at(Level.WARNING).log("[HC_CommandItem] Command is null or empty");
            return;
        }

        LOGGER.at(Level.FINE).log("[HC_CommandItem] Executing command '" + command + "' for player: " + player.getDisplayName());

        // Execute the command as the player
        CommandManager.get().handleCommand(player, command);
    }
}
