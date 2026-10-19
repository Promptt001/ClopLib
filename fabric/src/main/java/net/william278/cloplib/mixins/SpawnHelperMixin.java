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

package net.william278.cloplib.mixins;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.NaturalSpawner;
import net.william278.cloplib.events.SpawnEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class SpawnHelperMixin {

    // Checks whether a position is a valid spawn spot for a specific mob, matching
    // the mob's position validation used by natural spawning.
    @Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
    private static void isValidSpawnMixin(ServerLevel world, Mob entity, double squaredDistance,
                                          CallbackInfoReturnable<Boolean> cir) {
        final InteractionResult result = SpawnEvents.BEFORE_MOB_SPAWN.invoker().spawn(world, entity, EntitySpawnReason.NATURAL);
        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}
