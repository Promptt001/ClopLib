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

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface FabricBreakListener extends FabricListener {

    @NotNull
    Map<UUID, BlockPos> getLastBreakPositions();

    // When a player starts breaking a block
    default InteractionResult onPlayerStartBreakBlock(Player playerEntity, Level world, InteractionHand ignoredHand,
                                                 BlockPos pos, Direction ignoredDirection) {
        final BlockState state = world.getBlockState(pos);
        final boolean isBlock = state != null && !Blocks.AIR.defaultBlockState().equals(state);
        final BlockPos lastPos = getLastBreakPositions().get(playerEntity.getUUID());
        if (isBlock && getHandler().cancelOperation(Operation.of(
                getUser(playerEntity),
                getChecker().isFarmMaterial(FabricListener.getId(state.getBlock()))
                        ? OperationType.FARM_BLOCK_BREAK : OperationType.BLOCK_BREAK,
                getPosition(pos, world),
                pos.equals(lastPos)
        ))) {
            getLastBreakPositions().put(playerEntity.getUUID(), pos);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    // After a block is broken
    default boolean onPlayerBreakBlock(Level world, Player playerEntity, BlockPos pos,
                                       @Nullable BlockState block, @Nullable BlockEntity tileEntity) {
        if (block != null && getHandler().cancelOperation(Operation.of(
                getUser(playerEntity),
                getChecker().isFarmMaterial(FabricListener.getId(block.getBlock()))
                        ? OperationType.FARM_BLOCK_BREAK : OperationType.BLOCK_BREAK,
                getPosition(pos, world)
        ))) {
            this.sendTileEntityUpdate(tileEntity, playerEntity);
            return false;
        }
        return true;
    }

    // Send an update packet to the client when breaking blocks w/ tile entities to fix desync
    private void sendTileEntityUpdate(@Nullable BlockEntity tileEntity, @NotNull Player playerEntity) {
        if (tileEntity == null || !(playerEntity instanceof ServerPlayer player)) {
            return;
        }
        final Packet<ClientGamePacketListener> tileUpdate = tileEntity.getUpdatePacket();
        if (tileUpdate != null) {
            player.connection.send(tileUpdate);
        }
    }

}
