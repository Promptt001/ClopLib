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

package net.william278.cloplib.util;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.william278.cloplib.events.LecternEvents;
import org.jetbrains.annotations.NotNull;

public class TrackedLecternScreenHandler extends LecternMenu {

    private final Level lecternWorld;
    private final BlockPos lecternPos;

    public TrackedLecternScreenHandler(int syncId, @NotNull Container inventory,
                                       @NotNull ContainerData propertyDelegate,
                                       @NotNull BlockEntity blockEntity) {
        super(syncId, inventory, propertyDelegate);
        this.lecternWorld = blockEntity.getLevel();
        this.lecternPos = blockEntity.getBlockPos();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TAKE_BOOK) {
            final InteractionResult result = LecternEvents.BEFORE_BOOK_TAKEN.invoker().bookTaken(lecternWorld, lecternPos, player);
            if (result == InteractionResult.FAIL) {
                return false;
            }
        }
        return super.clickMenuButton(player, id);
    }

}
