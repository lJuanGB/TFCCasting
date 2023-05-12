package com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public interface ModifyCondition {

    public boolean shouldApply(ItemStack stack, ItemStack input);

    Serializer<?> serializer();

    @SuppressWarnings("unchecked")
    default void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(ModifyConditions.getId(serializer()));
        ((Serializer<ModifyCondition>) serializer()).toNetwork(this, buffer);
    }

    interface Serializer<T extends ModifyCondition> {
        T fromJson(JsonObject json);

        T fromNetwork(FriendlyByteBuf buffer);

        void toNetwork(T modifier, FriendlyByteBuf buffer);
    }
}
