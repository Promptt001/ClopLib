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

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.william278.cloplib.events.PlayerMovementEvents;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private double firstGoodX;
    @Shadow
    private double firstGoodY;
    @Shadow
    private double firstGoodZ;

    @Shadow
    private double vehicleFirstGoodX;
    @Shadow
    private double vehicleFirstGoodY;
    @Shadow
    private double vehicleFirstGoodZ;

    // 26.3: handleMovePlayer was refactored — the moved-too-quickly/bounding-box logic now lives in the
    // new private handlePlayerPositionChange(DDDFFZZ). Anchor on its INVOKE call site in handleMovePlayer
    // (single callsite, ordinal 0) instead of the old getBoundingBox INVOKE_ASSIGN.
    //#if MC>=260300
    //$$ @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handlePlayerPositionChange(DDDFFZZ)V"), cancellable = true)
    //#else
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/level/ServerPlayer;getBoundingBox()Lnet/minecraft/world/phys/AABB;"), cancellable = true)
    //#endif
    private void onPlayerMoveMixin(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!packet.hasPosition()) {
            return;
        }

        // Determine change in distance
        final Vec3 from = new Vec3(this.firstGoodX, this.firstGoodY, this.firstGoodZ);
        final Vec3 to = new Vec3(packet.getX(from.x()), packet.getY(from.y()), packet.getZ(from.z()));
        double delta = Math.pow(this.firstGoodX - to.x(), 2) +
                       Math.pow(this.firstGoodY - to.y(), 2) +
                       Math.pow(this.firstGoodZ - to.z(), 2);

        // Cancel the action
        if ((delta > 1f / 256) && !this.player.isImmobile()) {
            final InteractionResult result = PlayerMovementEvents.BEFORE_MOVE.invoker().move(
                    player, getServerWorld(player), from, to
            );
            if (result == InteractionResult.FAIL) {
                player.setDeltaMovement(Vec3.ZERO);
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z", ordinal = 0), cancellable = true)
    private void onVehicleMoveMixin(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        // Determine change in distance
        final Vec3 from = new Vec3(this.vehicleFirstGoodX, this.vehicleFirstGoodY, this.vehicleFirstGoodZ);
        // 26.3: ServerboundMoveVehiclePacket.position() -> movingTo().position() (PositionAndRotation).
        // Main project compiles shared src raw: 26.3-only lines carry //$$ (commented raw, uncommented by preprocess).
        //#if MC>=260300
        //$$ final Vec3 to = packet.movingTo().position();
        //#else
        //#if MC>=12104
        final Vec3 to = packet.position();
        //#else
        //$$ final Vec3 to = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        //#endif
        //#endif
        double delta = Math.pow(this.vehicleFirstGoodX - to.x(), 2) +
                       Math.pow(this.vehicleFirstGoodY - to.y(), 2) +
                       Math.pow(this.vehicleFirstGoodZ - to.z(), 2);

        // Cancel the action
        if ((delta > 1f / 256)) {
            final InteractionResult result = PlayerMovementEvents.BEFORE_MOVE.invoker().move(
                    player, getServerWorld(player), from, to
            );
            if (result == InteractionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Unique
    @NotNull
    private ServerLevel getServerWorld(ServerPlayer player) {
//#if MC>=12107
        return player.level();
//#else
//$$    return player.getServerWorld();
//#endif
    }

}
