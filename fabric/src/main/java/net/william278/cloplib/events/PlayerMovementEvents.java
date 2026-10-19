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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class PlayerMovementEvents {

    @NotNull
    public static final Event<BeforeMoveCallback> BEFORE_MOVE = EventFactory.createArrayBacked(
            BeforeMoveCallback.class,
            (callbacks) -> (player, world, from, to) -> {
                for (BeforeMoveCallback listener : callbacks) {
                    final InteractionResult result = listener.move(player, world, from, to);
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    @FunctionalInterface
    public interface BeforeMoveCallback {

        @NotNull
        InteractionResult move(ServerPlayer player, Level world, Vec3 from, Vec3 to);

    }

}
