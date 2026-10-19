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
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FabricBlockMoveListener extends FabricListener {

    // Stop fluids from entering claims
    @NotNull
    default InteractionResult onBlockFromTo(@NotNull Level world, @NotNull BlockPos from, @NotNull BlockPos to) {
        final OperationPosition blockPosition = getPosition(from, world);
        if (getHandler().cancelNature(
                blockPosition.getWorld(),
                blockPosition,
                getPosition(to, world))
        ) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    default InteractionResult onPistonActuate(Level world, BlockPos pistonBlock, Direction pistonDirection,
                                         List<BlockPos> affectedBlocks) {
        final OperationPosition pistonPos = getPosition(pistonBlock, world);
        
        if (getHandler().cancelOperation(Operation.of(
                OperationType.REDSTONE_ACTUATE,
                pistonPos,
                true
        ))) {
            return InteractionResult.FAIL;
        }
        
        for (final BlockPos blockPos : affectedBlocks) {
            if (getHandler().cancelNature(
                    pistonPos.getWorld(),
                    pistonPos,
                    getPosition(blockPos.relative(pistonDirection), world)
            )) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

}
