package org.role.rPG.Item;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public class StatMapDataType implements PersistentDataType<byte[], Map<String, Double>> {
    @Override
    public @NotNull Class<byte[]> getPrimitiveType() { return byte[].class; }
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Class<Map<String, Double>> getComplexType() { //noinspection rawtypes
        return (Class<Map<String, Double>>) (Class) Map.class; }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull Map<String, Double> complex, @NotNull PersistentDataAdapterContext context) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(complex);
            return baos.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Unable to serialize stat map.", e); }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Map<String, Double> fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(primitive); ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Map<String, Double>) ois.readObject();
        } catch (Exception e) { throw new IllegalStateException("Unable to deserialize stat map.", e); }
    }
}