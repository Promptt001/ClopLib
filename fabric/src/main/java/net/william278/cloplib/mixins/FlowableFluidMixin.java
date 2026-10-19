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

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
//#if MC==12101
//$$ import net.minecraft.world.level.material.Fluid;
//#endif
import net.minecraft.world.level.material.FluidState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.william278.cloplib.events.FluidEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(FlowingFluid.class)
public abstract class FlowableFluidMixin {

    // Performance: Ignore up/down fluid flow
    @Unique
    private static final Set<Direction> CLOPLIB_IGNORED_DIRECTIONS = Set.of(Direction.UP, Direction.DOWN);

    //#if MC>=12104
    // Fluid spreading runs in scheduled ticks on the server; blocking this method
    // prevents fluid entering the target block, mirroring the vanilla pass-through
    // check it wraps.
    @Inject(method = "canMaybePassThrough(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Z", at = @At("HEAD"), cancellable = true)
    private void canFlowThroughMixin(BlockGetter blockView, BlockPos fromPos, BlockState fromState, Direction flowDirection,
                                     BlockPos toPos, BlockState toState,
                                     FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (!(blockView instanceof ServerLevel world) || CLOPLIB_IGNORED_DIRECTIONS.contains(flowDirection)) {
            return;
        }

        final InteractionResult result = FluidEvents.BEFORE_FLOW.invoker().flow(world, fromPos, toPos);
        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(false);
        }
    }
    //#else
    //$$ @Inject(method = "canFlowThrough", at = @At("HEAD"), cancellable = true)
    //$$ private void canFlowThroughMixin(BlockGetter blockView, Fluid fluid, BlockPos toPos, BlockState state, Direction flowDirection, BlockPos fromPos,
    //$$     BlockState fromState, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
    //$$     if (!(blockView instanceof ServerLevel world) || CLOPLIB_IGNORED_DIRECTIONS.contains(flowDirection)) {
    //$$         return;
    //$$     }
    //$$
    //$$     final InteractionResult result = FluidEvents.BEFORE_FLOW.invoker().flow(world, fromPos, toPos);
    //$$     if (result == InteractionResult.FAIL) {
    //$$         cir.setReturnValue(false);
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "canFlow", at = @At("HEAD"), cancellable = true)
    //$$ private void canFlowMixin(BlockGetter blockView, BlockPos fromPos, BlockState fluidBlockState, Direction flowDirection, BlockPos toPos,
    //$$     BlockState flowToBlockState, FluidState fluidState, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
    //$$     if (!(blockView instanceof ServerLevel world) || CLOPLIB_IGNORED_DIRECTIONS.contains(flowDirection)) {
    //$$         return;
    //$$     }
    //$$
    //$$     final InteractionResult result = FluidEvents.BEFORE_FLOW.invoker().flow(world, fromPos, toPos);
    //$$     if (result == InteractionResult.FAIL) {
    //$$         cir.setReturnValue(false);
    //$$     }
    //$$ }
    //#endif

}
