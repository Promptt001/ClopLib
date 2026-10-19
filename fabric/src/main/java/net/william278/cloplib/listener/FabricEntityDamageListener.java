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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface FabricEntityDamageListener extends FabricListener {

    @NotNull
    default InteractionResult onPlayerAttackEntity(Player playerEntity, Level world, InteractionHand hand, Entity entity,
                                              @Nullable EntityHitResult ignoredEntityHitResult) {
        return handlePlayerDamageEntity(playerEntity, entity);
    }

    @NotNull
    default InteractionResult onProjectileHitEntity(Entity hit, Projectile projectile,
                                               @Nullable Entity shooter, @Nullable BlockPos dispensedFrom) {
        final Optional<ServerPlayer> playerShooter = getPlayerSource(shooter);
        if (playerShooter.isPresent()) {
            return handlePlayerDamageEntity(playerShooter.get(), hit);
        }

        // Handle dispensers
        if (dispensedFrom != null) {
            final OperationPosition dispenserPos = getPosition(dispensedFrom, hit.level());
            return getHandler().cancelNature(
                    dispenserPos.getWorld(),
                    getPosition(hit.position(), hit.level(), hit.getYRot(), hit.getXRot()),
                    dispenserPos
            ) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // Ignore other projectile damage sources (e.g. monsters hitting other monsters, etc.)
        return InteractionResult.PASS;
    }

    @NotNull
    default InteractionResult onExplosionDamageEntity(Explosion explosion, Entity entity) {
        if (entity == explosion.getIndirectSourceEntity()) {
            return InteractionResult.PASS;
        }

        // Handle decorative blocks (hanging items)
        final OperationPosition entityPos = getPosition(entity.position(), entity.level(), entity.getYRot(), entity.getXRot());
        if ((entity instanceof HangingEntity || entity instanceof ArmorStand) &&
                getHandler().cancelOperation(Operation.of(
                        OperationType.EXPLOSION_DAMAGE_TERRAIN,
                        entityPos
                ))) {
            return InteractionResult.FAIL;
        }

        // Handle player triggered explosions
        final Optional<ServerPlayer> player = getPlayerSource(explosion.getIndirectSourceEntity());
        if (player.isPresent() && !player.get().isSpectator()) {
            return handlePlayerDamageEntity(player.get(), entity);
        }

        // All other causes
        if (!isMonster(entity) && getHandler().cancelOperation(Operation.of(
                OperationType.EXPLOSION_DAMAGE_ENTITY,
                entityPos
        ))) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    private InteractionResult handlePlayerDamageEntity(Player attackerEntity, Entity damaged) {
        if (!(attackerEntity instanceof ServerPlayer attacker) || attacker.isSpectator()) {
            return InteractionResult.PASS;
        }

        // Handle PvP between two players
        final OperationPosition damagedPos = getPosition(
                damaged.position(), damaged.level(),
                damaged.getYRot(), damaged.getXRot()
        );
        if (damaged instanceof ServerPlayer playerVictim) {
            return getHandler().cancelOperation(Operation.of(
                    getUser(attacker),
                    getUser(playerVictim),
                    OperationType.PLAYER_DAMAGE_PLAYER,
                    damagedPos
            )) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // Handle based on hit entity type
        return getHandler().cancelOperation(Operation.of(
                getUser(attacker),
                getPlayerDamageType(damaged),
                damagedPos
        )) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    @NotNull
    private OperationType getPlayerDamageType(@NotNull Entity entity) {
        OperationType type = OperationType.PLAYER_DAMAGE_ENTITY;
        if (isMonster(entity)) {
            type = OperationType.PLAYER_DAMAGE_MONSTER;
        } else if (entity instanceof VehicleEntity vehicle) {
            type = vehicle instanceof ContainerEntity ? OperationType.BLOCK_BREAK : OperationType.BREAK_VEHICLE;
        } else if (entity instanceof HangingEntity) {
            type = OperationType.BREAK_HANGING_ENTITY;
        } else if (!(entity instanceof LivingEntity)) {
            type = OperationType.BLOCK_BREAK;
        } else if (entity.hasCustomName()) {
            type = OperationType.PLAYER_DAMAGE_PERSISTENT_ENTITY;
        }
        return type;
    }

}
