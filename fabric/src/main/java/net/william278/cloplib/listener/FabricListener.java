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

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.william278.cloplib.handler.Handler;
import net.william278.cloplib.handler.TypeChecker;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface FabricListener extends InspectorCallbackProvider {

    @NotNull
    OperationPosition getPosition(@NotNull Vec3 pos, @NotNull net.minecraft.world.level.Level world, float yaw, float pitch);

    @NotNull
    default OperationPosition getPosition(@NotNull BlockPos pos, @NotNull net.minecraft.world.level.Level world) {
        return this.getPosition(Vec3.atCenterOf(pos), world, 0.0f, 0.0f);
    }

    @NotNull
    OperationUser getUser(@NotNull Player player);

    @NotNull
    Handler getHandler();

    @NotNull
    TypeChecker getChecker();

    default Optional<ServerPlayer> getPlayerSource(@Nullable Entity e) {
        if (e == null) {
            return Optional.empty();
        }
        if (e instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        if (e instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        return e.getPassengers().stream()
                .filter(p -> p instanceof ServerPlayer)
                .map(p -> (ServerPlayer) p)
                .findFirst();
    }

    default boolean isMonster(@Nullable Entity entity) {
        return entity instanceof Monster || entity instanceof Enemy;
    }

    @NotNull
    static String getId(@NotNull Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @NotNull
    static String getId(@NotNull Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

}
