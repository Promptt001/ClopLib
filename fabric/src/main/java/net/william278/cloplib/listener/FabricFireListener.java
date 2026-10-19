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

package net.william278.cloplib.listener;

import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;

public interface FabricFireListener extends FabricListener {

    @NotNull
    default InteractionResult onFireSpread(@NotNull Level world, @NotNull BlockPos pos) {
        if (getHandler().cancelOperation(Operation.of(
                OperationType.FIRE_SPREAD,
                getPosition(pos, world)
        ))) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    default InteractionResult onBlockBurn(@NotNull Level world, @NotNull BlockPos pos) {
        if (getHandler().cancelOperation(Operation.of(
                OperationType.FIRE_BURN,
                getPosition(pos, world)
        ))) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

}
