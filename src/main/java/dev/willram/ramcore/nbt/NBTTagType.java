/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package dev.willram.ramcore.nbt;

import dev.willram.ramcore.shadows.nbt.NBTBase;
import dev.willram.ramcore.shadows.nbt.NBTNumber;
import dev.willram.ramcore.shadows.nbt.*;

/**
 * An enumeration of NBT tag types.
 */
public enum NBTTagType {

    END(NBTTagEnd.class, (byte) 0),
    BYTE(NBTTagByte.class, (byte) 1, true),
    SHORT(NBTTagShort.class, (byte) 2, true),
    INT(NBTTagInt.class, (byte) 3, true),
    LONG(NBTTagLong.class, (byte) 4, true),
    FLOAT(NBTTagFloat.class, (byte) 5, true),
    DOUBLE(NBTTagDouble.class, (byte) 6, true),
    BYTE_ARRAY(NBTTagByteArray.class, (byte) 7),
    STRING(NBTTagString.class, (byte) 8),
    LIST(NBTTagList.class, (byte) 9),
    COMPOUND(NBTTagCompound.class, (byte) 10),
    INT_ARRAY(NBTTagIntArray.class, (byte) 11),
    LONG_ARRAY(NBTTagLongArray.class, (byte) 12);

    private static final NBTTagType[] TYPES = values();

    /**
     * Gets the tag type for the specified id.
     *
     * @param id the id
     * @return the tag type
     * @throws ArrayIndexOutOfBoundsException if the id is not without bounds
     */
    public static NBTTagType of(final byte id) {
        return TYPES[id];
    }

    private final Class<? extends NBTBase> shadowClass;

    /**
     * The byte id of this tag type.
     */
    private final byte id;

    /**
     * If this tag type is a {@link NBTNumber number} type.
     */
    private final boolean number;

    NBTTagType(Class<? extends NBTBase> shadowClass, byte id) {
        this(shadowClass, id, false);
    }

    NBTTagType(Class<? extends NBTBase> shadowClass, byte id, boolean number) {
        this.shadowClass = shadowClass;
        this.id = id;
        this.number = number;
    }

    /**
     * Gets the shadow class of this tag type.
     *
     * @return the shadow class
     */
    public Class<? extends NBTBase> shadowClass() {
        return this.shadowClass;
    }

    /**
     * Gets the byte id of this tag type.
     *
     * @return the byte id
     */
    public byte id() {
        return this.id;
    }

    /**
     * Checks if this tag type is a {@link NBTNumber number} type.
     *
     * @return {@code true} if a number type, {@code false} if not
     */
    public boolean number() {
        return this.number;
    }

}
