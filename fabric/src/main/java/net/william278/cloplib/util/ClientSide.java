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

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Side checks for code that vanilla runs on both the logical client and the server.
 */
public final class ClientSide {

    private ClientSide() {
    }

    /**
     * Whether a hook should be skipped because it is running for a client world.
     *
     * @param level the level the hook is running in, if known
     * @return {@code true} for client worlds and unknown levels
     */
    public static boolean skip(@Nullable Level level) {
        return level == null || level.isClientSide();
    }

}
