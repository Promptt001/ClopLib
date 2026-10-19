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

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
//#if MC>=12105
import net.minecraft.world.entity.InsideBlockEffectApplier;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.InteractionResult;
import net.william278.cloplib.events.PressureBlockEvents;
import net.william278.cloplib.util.ClientSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {

    // entityInside runs during client movement prediction as well as on the server,
    // so client worlds are skipped; the listener checks again in case a future
    // registration forgets to.
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityCollisionMixin(Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier ech,
                                         boolean canCancelEffects, CallbackInfo ci) {
        if (ClientSide.skip(world)) {
            return;
        }
        final BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        final InteractionResult result = PressureBlockEvents.BEFORE_COLLISION.invoker().collide(world, pos, state, entity);
        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }

}
