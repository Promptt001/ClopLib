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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
//#if MC==12101
//$$ import net.minecraft.util.TypedActionResult;
//#endif
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.william278.cloplib.handler.TypeChecker;
import net.william278.cloplib.mixins.BucketItemMixin;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

public interface FabricUseItemListener extends FabricListener {

    // Map of use item predicates to operation types
    Map<BiPredicate<Item, TypeChecker>, OperationType> USE_ITEM_PREDICATE_MAP = Maps.newLinkedHashMap(Map.of(
            (i, c) -> i instanceof HangingEntityItem, OperationType.PLACE_HANGING_ENTITY,
            (i, c) -> c.isFarmMaterial(BuiltInRegistries.ITEM.getKey(i).toString()), OperationType.FARM_BLOCK_PLACE,
            (i, c) -> i instanceof BlockItem, OperationType.BLOCK_PLACE,
            (i, c) -> i instanceof BucketItem b && ((BucketItemMixin) b).getFluid() == Fluids.EMPTY , OperationType.FILL_BUCKET,
            (i, c) -> i instanceof BucketItem b && ((BucketItemMixin) b).getFluid() != Fluids.EMPTY, OperationType.EMPTY_BUCKET,
            (i, c) -> i instanceof EnderpearlItem || i == Items.CHORUS_FRUIT, OperationType.ENDER_PEARL_TELEPORT,
            (i, c) -> i instanceof ProjectileItem, OperationType.REDSTONE_INTERACT,
            (i, c) -> i instanceof BoatItem || i instanceof MinecartItem, OperationType.PLACE_VEHICLE,
            (i, c) -> i instanceof SpawnEggItem || i instanceof EggItem, OperationType.USE_SPAWN_EGG
    ));

    @NotNull
    Map<String, OperationType> getPrecalculatedItemMap();

    private Optional<OperationType> testItemPredicate(@NotNull Item item) {
        return USE_ITEM_PREDICATE_MAP.entrySet().stream()
                .filter(e -> e.getKey().test(item, getChecker()))
                .map(Map.Entry::getValue).findFirst();
    }

    default void precalculateItems(@NotNull Map<String, OperationType> map) {
        BuiltInRegistries.ITEM.forEach(i -> testItemPredicate(i).ifPresent(type -> map.put(FabricListener.getId(i), type)));
    }

    @NotNull
    //#if MC>=12104
    default InteractionResult onPlayerUseItem(Player playerEntity, Level world, InteractionHand hand) {
    //#else
    //$$ default TypedActionResult<ItemStack> onPlayerUseItem(Player playerEntity, Level world, InteractionHand hand) {
    //#endif
        final ItemStack item = playerEntity.getItemInHand(hand);
        if (item.isEmpty() || !(playerEntity instanceof ServerPlayer player)) {
            //#if MC>=12104
            return InteractionResult.PASS;
            //#else
            //$$ return TypedActionResult.pass(ItemStack.EMPTY);
            //#endif
        }

        // Check inspection items
        if (hand == InteractionHand.MAIN_HAND && this.handleInspectionCallbacks(player, world, item)) {
            //#if MC>=12104
            return InteractionResult.FAIL;
            //#else
            //$$ return TypedActionResult.fail(ItemStack.EMPTY);
            //#endif
        }

        // Check precalculated item operation map
        final OperationType operationType = getPrecalculatedItemMap().get(FabricListener.getId(item.getItem()));
        if (operationType == null) {
            //#if MC>=12104
            return InteractionResult.PASS;
            //#else
            //$$ return TypedActionResult.pass(ItemStack.EMPTY);
            //#endif
        }

        if (getHandler().cancelOperation(Operation.of(
                getUser(player),
                operationType,
                getUseItemPosition(player, world, hand, item),
                hand == InteractionHand.OFF_HAND
        ))) {
            //#if MC>=12104
            return InteractionResult.FAIL;
            //#else
            //$$ return TypedActionResult.fail(ItemStack.EMPTY);
            //#endif
        }
        //#if MC>=12104
        return InteractionResult.PASS;
        //#else
        //$$ return TypedActionResult.pass(ItemStack.EMPTY);
        //#endif
    }

    @NotNull
    private OperationPosition getUseItemPosition(@NotNull ServerPlayer player, @NotNull Level world,
                                                 @NotNull InteractionHand hand, @NotNull ItemStack item) {
        // Raycast where the player could place a block
        final Vec3 start = player.getEyePosition(0.0f);
        final Vec3 rot = player.getViewVector(0.0f);
        final Vec3 end = start.add(
                rot.x * player.blockInteractionRange(),
                rot.y * player.blockInteractionRange(),
                rot.z * player.blockInteractionRange()
        );

        // On a hit perform the item placement contextual check
        final BlockHitResult result = world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (result.getType() == HitResult.Type.BLOCK) {
            return getPosition(new BlockPlaceContext(player, hand, item, result).getClickedPos(), world);
        }
        return getPosition(player.position(), world, player.getYRot(), player.getXRot());
    }

    // Handle frost walker freezing ground
    @NotNull
    default InteractionResult onPlayerFrostWalker(Entity entity, Level world, BlockPos pos) {
        final Optional<ServerPlayer> player = getPlayerSource(entity);
        final OperationPosition blockPos = getPosition(pos, world);
        if (player.isPresent() && getHandler().cancelOperation(Operation.of(
                getUser(player.get()),
                OperationType.BLOCK_PLACE,
                blockPos,
                true
        ))) {
            return InteractionResult.FAIL;
        }

        // Cancel natural frost walker
        return getHandler().cancelNature(
                blockPos.getWorld(),
                getPosition(entity.blockPosition(), world),
                blockPos
        ) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    // Handle claim inspection callbacks
    default boolean handleInspectionCallbacks(ServerPlayer player, Level world, ItemStack item) {
        final InspectionTool tool = getTool(item);
        if (!getInspectionToolHandlers().containsKey(tool)) {
            return false;
        }

        final HitResult hit = player.pick(getInspectionDistance(), 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            final OperationPosition position = getPosition(((BlockHitResult) hit).getBlockPos(), world);
            if (!getHandler().handleInspection(position)) {
                return false;
            }
            getInspectionToolHandlers().get(tool).accept(getUser(player), position);
            return true;
        }
        return false;
    }

    @NotNull
    private InspectorCallbackProvider.InspectionTool getTool(@NotNull ItemStack item) {
        final InspectorCallbackProvider.InspectionTool.InspectionToolBuilder builder = InspectorCallbackProvider
                .InspectionTool.builder()
                .material(FabricListener.getId(item.getItem()));
        CustomModelData customModelDataComponent = item.getComponents().get(DataComponents.CUSTOM_MODEL_DATA);
        //#if MC>=12104
        if (customModelDataComponent != null && !customModelDataComponent.floats().isEmpty()) {
            builder.useCustomModelData(true).customModelData(customModelDataComponent.floats().get(0).intValue());
        }
        //#else
        //$$ if (customModelDataComponent != null) {
        //$$     builder.useCustomModelData(true).customModelData(customModelDataComponent.value());
        //$$ }
        //#endif
        return builder.build();
    }

    @NotNull
    default InteractionResult onDispenserPlace(Level world, BlockPos dispenserPos, BlockPos blockPos) {
        final OperationPosition dispenserPosition = getPosition(dispenserPos, world);
        
        // Check if redstone operations are allowed
        if (getHandler().cancelOperation(Operation.of(
                OperationType.REDSTONE_ACTUATE,
                dispenserPosition,
                true
        ))) {
            return InteractionResult.FAIL;
        }
        
        final OperationPosition block = getPosition(blockPos, world);
        return getHandler().cancelNature(
                block.getWorld(),
                dispenserPosition,
                block
        ) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

}
