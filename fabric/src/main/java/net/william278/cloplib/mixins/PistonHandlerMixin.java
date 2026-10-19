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

import com.google.common.collect.Streams;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.william278.cloplib.events.PistonEvents;
import net.william278.cloplib.util.ClientSide;
import org.jetbrains.annotations.Unmodifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PistonStructureResolver.class)
public abstract class PistonHandlerMixin {

    @Final
    @Shadow
    private Level level;
    @Final
    @Shadow
    private BlockPos pistonPos; // Piston location
    @Final
    @Shadow
    private Direction pistonDirection;
    @Final
    @Shadow
    private List<BlockPos> toPush;
    @Final
    @Shadow
    private List<BlockPos> toDestroy;

    // Piston structures resolve on the client too (block event animation), so client
    // worlds are skipped; the listener checks again in case a future registration
    // forgets to.
    // When one block is destroyed
    @Inject(method = "resolve", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void calculatePushSingleDestroyMixin(CallbackInfoReturnable<Boolean> cir) {
        if (ClientSide.skip(level)) {
            return;
        }
        final InteractionResult result = PistonEvents.BEFORE_ACTUATION.invoker()
                .actuate(level, pistonPos, pistonDirection, getChangedBlocks());
        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    // When one or more blocks are moved or destroyed
    @Inject(method = "resolve", at = @At(value = "RETURN", ordinal = 4), cancellable = true)
    private void calculatePushOneOrMoreMoveMixin(CallbackInfoReturnable<Boolean> cir) {
        if (ClientSide.skip(level)) {
            return;
        }
        final InteractionResult result = PistonEvents.BEFORE_ACTUATION.invoker()
                .actuate(level, pistonPos, pistonDirection, getChangedBlocks());
        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Unique
    @Unmodifiable
    private List<BlockPos> getChangedBlocks() {
        return Streams.concat(toPush.stream(), toDestroy.stream()).toList();
    }

}
