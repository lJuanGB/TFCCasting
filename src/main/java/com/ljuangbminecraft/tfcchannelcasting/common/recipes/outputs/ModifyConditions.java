package com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs;

import java.util.Objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.dries007.tfc.util.JsonHelpers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ModifyConditions {
    private static final BiMap<ResourceLocation, ModifyCondition.Serializer<?>> REGISTRY = HashBiMap.create();

    public static void registerItemStackModifierTypes() {
        register("date_range", DateRangeModifyCondition.Serializer.INSTANCE);
        register("has_trait", HasTraitModifyCondition.Serializer.INSTANCE);
    }

    public static synchronized <V extends ModifyCondition, T extends ModifyCondition.Serializer<V>> T register(
            ResourceLocation key, T serializer) {
        if (REGISTRY.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate key: " + key);
        }
        REGISTRY.put(key, serializer);
        return serializer;
    }

    public static ModifyCondition fromJson(JsonElement json) {
        final JsonObject obj = JsonHelpers.convertToJsonObject(json, "condition");
        final String type = JsonHelpers.getAsString(obj, "type");
        final ModifyCondition.Serializer<?> serializer = getSerializer(type);
        return serializer.fromJson(obj);
    }

    public static ModifyCondition fromNetwork(FriendlyByteBuf buffer) {
        final ResourceLocation id = buffer.readResourceLocation();
        final ModifyCondition.Serializer<?> serializer = byId(id);
        return serializer.fromNetwork(buffer);
    }

    private static ModifyCondition.Serializer<?> getSerializer(String type) {
        final ModifyCondition.Serializer<?> serializer = REGISTRY.get(new ResourceLocation(type));
        if (serializer != null) {
            return serializer;
        }
        throw new JsonParseException("Unknown item stack modifier condition type: " + type);
    }

    public static ModifyCondition.Serializer<?> byId(ResourceLocation id) {
        return Objects.requireNonNull(REGISTRY.get(id), () -> "No serializer by id: " + id);
    }

    public static ResourceLocation getId(ModifyCondition.Serializer<?> serializer) {
        return Objects.requireNonNull(REGISTRY.inverse().get(serializer),
                () -> "Unregistered serializer: " + serializer);
    }

    private static void register(String name, ModifyCondition.Serializer<?> serializer) {
        register(new ResourceLocation("tfcchannelcasting", name), serializer);
    }
}
