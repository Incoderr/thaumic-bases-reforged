package tb;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

final class PortNbt {
    record Input(CompoundTag tag, HolderLookup.Provider registries) {
        <T> Optional<T> read(String key, Codec<T> codec) {
            Tag value = tag.get(key);
            return value == null ? Optional.empty() : codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), value).result();
        }
        int getIntOr(String key, int fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback; }
        boolean getBooleanOr(String key, boolean fallback) { return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback; }
    }
    record Output(CompoundTag tag, HolderLookup.Provider registries) {
        <T> void store(String key, Codec<T> codec, T value) {
            tag.put(key, codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value).getOrThrow());
        }
        void putInt(String key, int value) { tag.putInt(key, value); }
        void putBoolean(String key, boolean value) { tag.putBoolean(key, value); }
    }
}
