package org.mcreater.disk.utils;

import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.ByteTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.EndTag;
import net.querz.nbt.tag.FloatTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.LongTag;
import net.querz.nbt.tag.ShortTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TagUtils {
    public static Object toNativeType(Tag<?> tag){
        if (tag instanceof ByteArrayTag) {
            Vector<Byte> bytes = new Vector<>();
            for (byte b : ((ByteArrayTag) tag).getValue()) {
                bytes.add(b);
            }
            return bytes;
        }
        else if (tag instanceof ByteTag) {
            return ((ByteTag) tag).asByte();
        }
        else if (tag instanceof CompoundTag) {
            Map<String, Object> value = new HashMap<>();
            CompoundTag tag1 = (CompoundTag) tag;
            tag1.forEach((s, tag2) -> value.put(s, toNativeType(tag2)));
            return value;
        }
        else if (tag instanceof DoubleTag) {
            return ((DoubleTag) tag).asDouble();
        }
        else if (tag instanceof EndTag) {
            return "";
        }
        else if (tag instanceof FloatTag) {
            return ((FloatTag) tag).asFloat();
        }
        else if (tag instanceof IntArrayTag) {
            Vector<Integer> ints = new Vector<>();
            for (int b : ((IntArrayTag) tag).getValue()) {
                ints.add(b);
            }
            return ints;
        }
        else if (tag instanceof IntTag) {
            return ((IntTag)tag).asInt();
        }
        else if (tag instanceof ListTag<?>) {
            Vector<Object> vector = new Vector<>();
            ((ListTag<?>) tag).iterator().forEachRemaining((Consumer<Object>) o -> {
                try {
                    vector.add(toNativeType((Tag<?>) o));
                }
                catch (ClassCastException ignored){}
            });
            return vector;
        }
        else if (tag instanceof LongArrayTag) {
            Vector<Long> longs = new Vector<>();
            for (long b : ((LongArrayTag) tag).getValue()) {
                longs.add(b);
            }
            return longs;
        }
        else if (tag instanceof LongTag) {
            return ((LongTag) tag).asLong();
        }
        else if (tag instanceof ShortTag) {
            return ((ShortTag) tag).asShort();
        }
        else if (tag instanceof StringTag) {
            return ((StringTag) tag).getValue();
        }
        else {
            return tag.toString();
        }
    }
}
