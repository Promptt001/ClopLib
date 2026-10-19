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

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.william278.cloplib.events.FireTickEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Carefully prevent fire from doing specific things.
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @Shadow @Final
    public static IntegerProperty AGE;

    // Prevent fire spread contextually
    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void trySpreadingFireMixin(Level world, BlockPos pos, int spreadFactor, RandomSource random,
                                       int currentAge, CallbackInfo cir) {
        final InteractionResult result = FireTickEvents.BEFORE_SPREAD.invoker().fireSpread(world, pos);
        if (result == InteractionResult.FAIL) {
            cir.cancel();
        }
    }

    // Prevent fire spread contextually
    @Inject(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private void getBurnChanceMixin(LevelReader world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        final InteractionResult result = FireTickEvents.BEFORE_BURN.invoker().fireBurn((Level) world, pos);
        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }

}

