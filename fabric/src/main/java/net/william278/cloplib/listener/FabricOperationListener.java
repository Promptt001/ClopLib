/*
 * This file is part of ClopLib, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.cloplib.listener;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.william278.cloplib.events.*;
import net.william278.cloplib.handler.Handler;
import net.william278.cloplib.handler.SpecialTypeChecker;
import net.william278.cloplib.handler.TypeChecker;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationType;
import net.william278.cloplib.operation.OperationUser;
import net.william278.cloplib.util.ClientSide;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * A listener for Fabric callbacks that can be used to cancel operations
 */
@Getter
public abstract class FabricOperationListener implements OperationListener, FabricWorldListener, FabricBreakListener,
        FabricUseItemListener, FabricUseBlockListener, FabricUseEntityListener, FabricEntityDamageListener,
        FabricEntityListener, FabricBlockMoveListener, FabricFireListener, FabricMoveListener {

    public static final Identifier CLOPLIB_LISTENERS = Identifier.fromNamespaceAndPath("cloplib", "listeners");

    private final Handler handler;
    private final TypeChecker checker;
    private final Map<InspectorCallbackProvider.InspectionTool, BiConsumer<OperationUser, OperationPosition>> inspectionToolHandlers;
    private final Map<UUID, BlockPos> lastBreakPositions;

    // Maps of registry blocks to operation types, precalculated on data (re)load for perf
    private final Map<String, OperationType> precalculatedItemMap = Maps.newHashMap();
    private final Map<String, OperationType> precalculatedBlockMap = Maps.newHashMap();

    @SuppressWarnings("unused")
    public FabricOperationListener(@NotNull Handler handler, @NotNull ModContainer modContainer) {
        this(
                handler,
                SpecialTypeChecker.load(Objects.requireNonNull(
                        getSpecialTypes(modContainer),
                        "Failed to load special types file")
                ),
                Maps.newHashMap()
        );
    }

    @ApiStatus.Internal
    public FabricOperationListener(@NotNull Handler handler, @NotNull SpecialTypeChecker checker,
                                   @NotNull HashMap<InspectorCallbackProvider.InspectionTool, BiConsumer<OperationUser, OperationPosition>> map) {
        this.handler = handler;
        this.checker = checker;
        this.inspectionToolHandlers = map;
        this.lastBreakPositions = Maps.newHashMap();
        this.registerCallbacks();
    }

    private void registerCallbacks() {
        // Fabric fires the interaction callbacks on the logical client as well as on the
        // server. Protection decisions are made on the server only; on the client every
        // callback passes, so the server stays the single source of truth and the client
        // never touches server-side state.
        PlayerBlockBreakEvents.BEFORE.register(this::onPlayerBreakBlock); // Server-side event only
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                level.isClientSide() ? InteractionResult.PASS
                        : onPlayerStartBreakBlock(player, level, hand, pos, direction));
        UseBlockCallback.EVENT.addPhaseOrdering(CLOPLIB_LISTENERS, Event.DEFAULT_PHASE); // Prevents weirdness
        UseBlockCallback.EVENT.register(CLOPLIB_LISTENERS, (player, level, hand, hit) ->
                level.isClientSide() ? InteractionResult.PASS : onPlayerUseBlock(player, level, hand, hit));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                level.isClientSide() ? InteractionResult.PASS
                        : onPlayerUseEntity(player, level, hand, entity, hit));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                level.isClientSide() ? InteractionResult.PASS
                        : onPlayerAttackEntity(player, level, hand, entity, hit));
        UseItemCallback.EVENT.register((player, level, hand) ->
                level.isClientSide() ? InteractionResult.PASS : onPlayerUseItem(player, level, hand)); // Registered last

        // Events raised by this library's own mixins: the mixins skip the client (see
        // ClientSide.skip), and the listeners check again so a future mixin cannot
        // forget it.
        LecternEvents.BEFORE_BOOK_TAKEN.register((level, pos, player) ->
                level.isClientSide() ? InteractionResult.PASS : onPlayerTakeLecternBook(level, pos, player));
        PressureBlockEvents.BEFORE_COLLISION.register((level, pos, state, entity) ->
                level.isClientSide() ? InteractionResult.PASS
                        : onEntityPhysicallyInteract(level, pos, state, entity));
        ProjectileEvents.BEFORE_BLOCK_HIT.register((pos, level, projectile, shooter, dispensedFrom) ->
                level.isClientSide() ? InteractionResult.PASS
                        : onProjectileHitBlock(pos, level, projectile, shooter, dispensedFrom));
        ProjectileEvents.BEFORE_ENTITY_HIT.register((entity, projectile, shooter, dispensedFrom) ->
                ClientSide.skip(entity.level()) ? InteractionResult.PASS
                        : onProjectileHitEntity(entity, projectile, shooter, dispensedFrom));
        DispenserEvents.BEFORE_PLACE.register((level, dispenser, placedPos) ->
                level.isClientSide() ? InteractionResult.PASS : onDispenserPlace(level, dispenser, placedPos));
        FluidEvents.BEFORE_FLOW.register((level, from, to) ->
                level.isClientSide() ? InteractionResult.PASS : onBlockFromTo(level, from, to));
        PistonEvents.BEFORE_ACTUATION.register((level, pistonPos, pistonDirection, affectedBlocks) ->
                level.isClientSide() ? InteractionResult.PASS
                        : onPistonActuate(level, pistonPos, pistonDirection, affectedBlocks));
        RaidEvents.BEFORE_START.register((level, pos, player) ->
                level.isClientSide() ? InteractionResult.PASS : onRaidTriggered(level, pos, player));
        FireTickEvents.BEFORE_BURN.register((level, pos) ->
                level.isClientSide() ? InteractionResult.PASS : onBlockBurn(level, pos));
        FireTickEvents.BEFORE_SPREAD.register((level, pos) ->
                level.isClientSide() ? InteractionResult.PASS : onFireSpread(level, pos));
        EnchantmentEffectEvents.BEFORE_BLOCK_UPDATE.register((entity, level, pos) ->
                level.isClientSide() ? InteractionResult.PASS : onPlayerFrostWalker(entity, level, pos));
        SpawnEvents.BEFORE_MOB_SPAWN.register((level, entity, reason) ->
                level.isClientSide() ? InteractionResult.PASS : onCreatureSpawn(level, entity, reason));
        ExplosionEvents.BEFORE_BLOCKS_BROKEN.register((explosion, blocks) ->
                ClientSide.skip(explosion.level()) ? blocks : onExplosionBreakBlocks(explosion, blocks));
        ExplosionEvents.BEFORE_DAMAGE_ENTITY.register((explosion, entity) ->
                ClientSide.skip(explosion.level()) ? InteractionResult.PASS
                        : onExplosionDamageEntity(explosion, entity));
        PlayerMovementEvents.BEFORE_MOVE.register(this::onPlayerMove); // ServerPlayer only

        // Register handlers for precalculating data
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(this::onDataReloaded);
    }

    // Recalculate block operation map when the server starts
    private void onServerStarted(MinecraftServer server) {
        this.precalculateMaps();
    }

    // Recalculate block operation map when the data pack is reloaded
    private void onDataReloaded(MinecraftServer server, CloseableResourceManager manager, boolean b) {
        this.precalculateMaps();
    }

    private void precalculateMaps() {
        precalculatedItemMap.clear();
        this.precalculateItems(precalculatedItemMap);
        precalculatedBlockMap.clear();
        this.precalculateBlocks(precalculatedBlockMap);
    }

    @Nullable
    private static InputStream getSpecialTypes(@NotNull ModContainer modContainer) {
        return modContainer.findPath(SPECIAL_TYPES_FILE)
                .map(path -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException ignored) {
                        return null;
                    }
                })
                .orElse(FabricOperationListener.class.getClassLoader().getResourceAsStream(SPECIAL_TYPES_FILE));
    }

    /**
     * Returns the {@link OperationPosition} of a pos and world
     *
     * @param pos   the location
     * @param world the world
     * @param yaw   the yaw
     * @param pitch the pitch
     * @return the OperationPosition of the location
     * @since 2.0
     */
    @NotNull
    public abstract OperationPosition getPosition(@NotNull Vec3 pos, @NotNull net.minecraft.world.level.Level world,
                                                  float yaw, float pitch);

    /**
     * Returns the {@link OperationUser} of a {@link Player}
     *
     * @param player the player
     * @return the OperationUser of the player
     * @since 2.0
     */
    @NotNull
    public abstract OperationUser getUser(@NotNull Player player);

    /**
     * Set the callback for when a player inspects a block while holding something
     *
     * @param tool     the tool the user must be holding to trigger the callback
     * @param callback the callback to set
     * @since 2.0
     */
    @Override
    public void setInspectorCallback(@NotNull InspectorCallbackProvider.InspectionTool tool,
                                     @NotNull BiConsumer<OperationUser, OperationPosition> callback) {
        inspectionToolHandlers.put(tool, callback);
    }

}
