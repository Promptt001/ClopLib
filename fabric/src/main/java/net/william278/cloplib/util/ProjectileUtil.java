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

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ProjectileUtil {

    private static final Identifier DISPENSED_KEY = Identifier.fromNamespaceAndPath("cloplib", "dispensed");
    private static final Identifier ORIGIN_KEY = Identifier.fromNamespaceAndPath("cloplib", "origin");

    @Nullable
    public static BlockPos getOrigin(@NotNull Projectile entity) {
        // Check if set contains key
        final Set<String> tags = entity.entityTags();
        if (!tags.contains(DISPENSED_KEY.toString())) {
            return null;
        }

        // Find origin tag (slower lookup, hence the previous check)
        final String tag = tags.stream().filter(t -> t.startsWith(ORIGIN_KEY.toString()))
                .findFirst().orElse(null);
        if (tag == null) {
            return null;
        }

        // Parse BlockPos from key
        try {
            return BlockPos.of(Long.parseLong(tag.split("/")[1]));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    public static void markOrigin(@NotNull Projectile entity, @NotNull BlockPos pos) {
        entity.addTag(DISPENSED_KEY.toString());
        entity.addTag("%s/%s".formatted(ORIGIN_KEY.toString(), pos.asLong()));
    }

}
