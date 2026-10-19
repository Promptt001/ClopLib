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

package net.william278.cloplib.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProjectileEvents {

    @NotNull
    public static final Event<BeforeEntityHit> BEFORE_ENTITY_HIT = EventFactory.createArrayBacked(
            BeforeEntityHit.class,
            (callbacks) -> (entity, projectile, shooter, dispensedFrom) -> {
                for (BeforeEntityHit listener : callbacks) {
                    final InteractionResult result = listener.entityHit(entity, projectile, shooter, dispensedFrom);
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    @NotNull
    public static final Event<BeforeBlockHit> BEFORE_BLOCK_HIT = EventFactory.createArrayBacked(
            BeforeBlockHit.class,
            (callbacks) -> (block, world, projectile, shooter, dispensedFrom) -> {
                for (BeforeBlockHit listener : callbacks) {
                    final InteractionResult result = listener.blockHit(block, world, projectile, shooter, dispensedFrom);
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    @FunctionalInterface
    public interface BeforeEntityHit {

        @NotNull
        InteractionResult entityHit(Entity hitEntity, Projectile projectile,
                               @Nullable Entity shooter, @Nullable BlockPos dispensedFrom);

    }

    @FunctionalInterface
    public interface BeforeBlockHit {

        @NotNull
        InteractionResult blockHit(BlockPos hitBlock, Level world, Projectile projectile,
                              @Nullable Entity shooter, @Nullable BlockPos dispensedFrom);


    }

}
