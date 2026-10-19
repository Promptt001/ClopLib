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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.william278.cloplib.handler.TypeChecker;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

public interface FabricUseBlockListener extends FabricListener {

    // Map of use block predicates to operation types
    Map<BiPredicate<Block, TypeChecker>, OperationType> USE_BLOCK_PREDICATE_MAP = Maps.newLinkedHashMap(Map.of(
            (b, c) -> b instanceof CampfireBlock || b instanceof BeaconBlock, OperationType.CONTAINER_OPEN,
            (b, c) -> b instanceof SignBlock, OperationType.BLOCK_PLACE,
            (b, c) -> c.isFarmMaterial(FabricListener.getId(b)), OperationType.FARM_BLOCK_INTERACT,
            (b, c) -> b instanceof DoorBlock || b instanceof TrapDoorBlock
                    || b instanceof FenceGateBlock, OperationType.BLOCK_INTERACT,
            (b, c) -> c.isPressureSensitiveMaterial(FabricListener.getId(b)) || b instanceof LeverBlock ||
                    b instanceof ButtonBlock || b instanceof RedStoneOreBlock, OperationType.REDSTONE_INTERACT
    ));

    @NotNull
    Map<String, OperationType> getPrecalculatedBlockMap();

    @NotNull
    Map<String, OperationType> getPrecalculatedItemMap();

    private Optional<OperationType> testBlockPredicate(@NotNull Block block) {
        return USE_BLOCK_PREDICATE_MAP.entrySet().stream()
                .filter(e -> e.getKey().test(block, getChecker()))
                .map(Map.Entry::getValue).findFirst();
    }

    default void precalculateBlocks(@NotNull Map<String, OperationType> map) {
        BuiltInRegistries.BLOCK.forEach((i) -> testBlockPredicate(i).ifPresent(
                type -> map.put(BuiltInRegistries.BLOCK.getKey(i).toString(), type)));
    }

    @NotNull
    default InteractionResult onPlayerUseBlock(Player playerEntity, Level world, InteractionHand hand, BlockHitResult blockHit) {
        if (blockHit.getType() != HitResult.Type.BLOCK || !(playerEntity instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        // Check clicked block is not air
        final BlockState blockState = world.getBlockState(blockHit.getBlockPos());
        if (blockState == null || blockState.isAir()) {
            return InteractionResult.PASS;
        }

        // Check entity
        OperationType operationType = checkUseBlockEntity(player, world, world.getBlockEntity(blockHit.getBlockPos()));
        if (operationType != null && getHandler().cancelOperation(Operation.of(
                getUser(player),
                operationType,
                getPosition(blockHit.getBlockPos(), world),
                hand == InteractionHand.OFF_HAND
        ))) {
            return InteractionResult.FAIL;
        }

        // Check the clicked block's own interaction (doors, levers, farmland and similar)
        operationType = getPrecalculatedBlockMap().get(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString());
        if (operationType != null && getHandler().cancelOperation(Operation.of(
                getUser(player),
                operationType,
                getPosition(blockHit.getBlockPos(), world),
                hand == InteractionHand.OFF_HAND
        ))) {
            return InteractionResult.FAIL;
        }

        // Check what the held item would place, at the block vanilla would place it in
        final ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && (held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem)
                && getHandler().cancelOperation(Operation.of(
                        getUser(player),
                        getPrecalculatedItemMap().getOrDefault(
                                FabricListener.getId(held.getItem()), OperationType.BLOCK_PLACE),
                        getPosition(getPlacementTarget(player, hand, held, blockHit), world)
                ))) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    // The block a right click with this item would change. Block items follow vanilla's
    // own placement rules, including replaceable blocks such as grass or snow layers,
    // which are replaced in place rather than built against. Buckets act on the block
    // in front of the clicked face.
    @NotNull
    private BlockPos getPlacementTarget(@NotNull ServerPlayer player, @NotNull InteractionHand hand,
                                        @NotNull ItemStack held, @NotNull BlockHitResult hit) {
        if (held.getItem() instanceof BlockItem blockItem) {
            final BlockPlaceContext context = new BlockPlaceContext(player, hand, held, hit);
            final BlockPlaceContext adjusted = blockItem.updatePlacementContext(context);
            return (adjusted != null ? adjusted : context).getClickedPos();
        }
        return hit.getBlockPos().relative(hit.getDirection());
    }

    @Nullable
    private OperationType checkUseBlockEntity(@NotNull ServerPlayer player, @NotNull Level world,
                                              @Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        if (blockEntity instanceof WorldlyContainer || blockEntity instanceof Container ||
                blockEntity instanceof CampfireBlockEntity || blockEntity instanceof BeaconBlockEntity) {
            return OperationType.CONTAINER_OPEN;
        }
        if (blockEntity instanceof SignBlockEntity) {
            return OperationType.BLOCK_PLACE;
        }
        return null;
    }

    @NotNull
    default InteractionResult onEntityPhysicallyInteract(Level world, BlockPos blockPos, BlockState state, Entity entity) {
        // Check if the block was a pressure sensitive block
        if (!getChecker().isPressureSensitiveMaterial(FabricListener.getId(state.getBlock()))) {
            return InteractionResult.PASS;
        }

        // Get the player source
        final Optional<ServerPlayer> playerSource = getPlayerSource(entity);
        if (playerSource.isEmpty() || playerSource.get().isSpectator()) {
            return InteractionResult.PASS;
        }

        // Check if this is allowed
        final ServerPlayer player = playerSource.get();
        if (getHandler().cancelOperation(Operation.of(
                getUser(player),
                OperationType.REDSTONE_INTERACT,
                getPosition(blockPos, world),
                true
        ))) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    default InteractionResult onPlayerTakeLecternBook(Level world, BlockPos blockPos, Player playerEntity) {
        if (!(playerEntity instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        // Check if this is allowed
        if (getHandler().cancelOperation(Operation.of(
                getUser(player),
                OperationType.CONTAINER_OPEN,
                getPosition(blockPos, world),
                true
        ))) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    default InteractionResult onProjectileHitBlock(BlockPos blockPos, Level world, Projectile projectile,
                                              @Nullable Entity shooter, @Nullable BlockPos dispensedFrom) {
        // Check players shooting stuff
        final Optional<ServerPlayer> playerShooter = getPlayerSource(shooter);
        if (playerShooter.isPresent()) {
            return getHandler().cancelOperation(Operation.of(
                    getUser(playerShooter.get()),
                    OperationType.REDSTONE_INTERACT,
                    getPosition(blockPos, world)
            )) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // Check dispenser hitting a block
        if (dispensedFrom != null) {
            final OperationPosition dispenserPos = getPosition(dispensedFrom, world);
            return getHandler().cancelNature(
                    dispenserPos.getWorld(),
                    getPosition(blockPos, world),
                    dispenserPos
            ) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // Prevent mobs from shooting projectiles to do stuff
        return getHandler().cancelOperation(Operation.of(
                OperationType.MONSTER_DAMAGE_TERRAIN,
                getPosition(blockPos, world)
        )) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

}
