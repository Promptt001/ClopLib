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
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ProjectileItem.DispenseConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.william278.cloplib.util.ProjectileUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileDispenseBehavior.class)
public abstract class ProjectileDispenserBehaviourMixin {

    @Final
    @Shadow
    private ProjectileItem projectileItem;

    @Final
    @Shadow
    private DispenseConfig dispenseConfig;

    @Inject(method = "execute", at = @At(value = "HEAD"), cancellable = true)
    private void dispenseSilentlyMixin(BlockSource pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        ServerLevel serverWorld = pointer.level();
        Direction direction = pointer.state().getValue(DispenserBlock.FACING);
        Position position = this.dispenseConfig.positionFunction().getDispensePosition(pointer, direction);
        final Projectile entity = Projectile.spawnProjectile(this.projectileItem.asProjectile(serverWorld, position, stack, direction), serverWorld, stack, p -> p.shoot(direction.getStepX(), direction.getStepY(), direction.getStepZ(), this.dispenseConfig.power(), this.dispenseConfig.uncertainty()));
        ProjectileUtil.markOrigin(entity, pointer.pos());
        stack.shrink(1);
        cir.setReturnValue(stack);
        cir.cancel();
    }

}
