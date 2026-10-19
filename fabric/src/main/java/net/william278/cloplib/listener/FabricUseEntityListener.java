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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.Level;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FabricUseEntityListener extends FabricListener {

    @NotNull
    default InteractionResult onPlayerUseEntity(Player playerEntity, Level world, InteractionHand hand, Entity entity,
                                           @Nullable EntityHitResult entityHitResult) {
        if (!(playerEntity instanceof ServerPlayer player) ||
                player.isSpectator() || entity instanceof Player) {
            return InteractionResult.PASS;
        }

        if (entityHitResult == null) {
            return onPlayerClickEntity(player, entity, world, hand); // Click with hand
        }
        return onPlayerHitClickEntity(player, entity, world, hand, entityHitResult); // Click with item
    }

    @NotNull
    private InteractionResult onPlayerHitClickEntity(ServerPlayer player, Entity entity, Level world, InteractionHand hand,
                                                EntityHitResult entityHitResult) {

        if (entity instanceof ArmorStand || entity instanceof ItemFrame) {
            return getHandler().cancelOperation(Operation.of(
                    getUser(player),
                    OperationType.CONTAINER_OPEN,
                    getPosition(entityHitResult.getLocation(), world, entity.getYRot(), entity.getXRot()),
                    hand == InteractionHand.OFF_HAND
            )) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // Only do this check on mobs (above handlers manages other cases)
        if (entity instanceof Mob) {
            return getHandler().cancelOperation(Operation.of(
                    getUser(player),
                    OperationType.ENTITY_INTERACT,
                    getPosition(entityHitResult.getLocation(), world, entity.getYRot(), entity.getXRot()),
                    hand == InteractionHand.OFF_HAND
            )) ? InteractionResult.FAIL : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    private InteractionResult onPlayerClickEntity(ServerPlayer player, Entity entity, Level world, InteractionHand hand) {
        // Check against interacting with container vehicles
        if (((entity instanceof VehicleEntity && entity instanceof ContainerEntity))) {
            return getHandler().cancelOperation(Operation.of(
                    getUser(player),
                    OperationType.CONTAINER_OPEN,
                    getPosition(entity.position(), world, entity.getYRot(), entity.getXRot()),
                    hand == InteractionHand.OFF_HAND
            )) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // Allow interacting with hostiles
        if (isMonster(entity)) {
            return getHandler().cancelOperation(Operation.of(
                    getUser(player),
                    OperationType.PLAYER_DAMAGE_MONSTER,
                    getPosition(entity.position(), world, entity.getYRot(), entity.getXRot()),
                    hand == InteractionHand.OFF_HAND
            )) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        if (getHandler().cancelOperation(Operation.of(
                getUser(player),
                OperationType.ENTITY_INTERACT,
                getPosition(entity.position(), world, entity.getYRot(), entity.getXRot()),
                hand == InteractionHand.OFF_HAND
        ))) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

}
