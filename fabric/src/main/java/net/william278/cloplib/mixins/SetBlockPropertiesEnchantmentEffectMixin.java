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

import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.SetBlockProperties;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.william278.cloplib.events.EnchantmentEffectEvents;
import net.william278.cloplib.util.ClientSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// For emitting the place block operation when users have frost walker.
// Set and replace are both used here by the game and are nearly identical, so we mixin to both.
@Mixin(SetBlockProperties.class)
public abstract class SetBlockPropertiesEnchantmentEffectMixin {

    @Shadow
    @Final
    private Vec3i offset;

    // Frost walker runs its block placement prediction on the client too, so client
    // worlds are skipped; the listener checks again in case a future registration
    // forgets to.
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    void applyMixin(ServerLevel world, int level, EnchantedItemInUse context, Entity user, Vec3 pos,
                    CallbackInfo ci) {
        if (ClientSide.skip(world)) {
            return;
        }
        final InteractionResult result = EnchantmentEffectEvents.BEFORE_BLOCK_UPDATE.invoker().blockUpdate(user, world,
                BlockPos.containing(pos).offset(this.offset));
        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }

}
