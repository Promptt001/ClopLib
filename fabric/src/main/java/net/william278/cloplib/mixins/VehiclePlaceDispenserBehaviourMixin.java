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

import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
//#if MC>=12104
import net.minecraft.core.dispenser.MinecartDispenseItemBehavior;
//#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Direction;
import net.william278.cloplib.events.DispenserEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12104
@Mixin(value = {BoatDispenseItemBehavior.class, MinecartDispenseItemBehavior.class})
//#else
//$$ @Mixin(value = BoatDispenseItemBehavior.class, targets = "net/minecraft/item/MinecartItem$1")
//#endif
public abstract class VehiclePlaceDispenserBehaviourMixin {

    @Unique
    private static final DispenseItemBehavior CLOPLIB_VEHICLE_FALLBACK_BEHAVIOUR = new DefaultDispenseItemBehavior();

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void dispenseSilentlyMixin(BlockSource pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        final Direction direction = pointer.state().getValue(DispenserBlock.FACING);
        final InteractionResult result = DispenserEvents.BEFORE_PLACE.invoker().place(
                pointer.level(), pointer.pos(), pointer.pos().relative(direction)
        );
        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(CLOPLIB_VEHICLE_FALLBACK_BEHAVIOUR.dispense(pointer, stack));
            cir.cancel();
        }
    }

}
