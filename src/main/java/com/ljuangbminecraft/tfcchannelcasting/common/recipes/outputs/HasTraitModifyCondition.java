package com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs;

import com.google.gson.JsonObject;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.util.JsonHelpers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record HasTraitModifyCondition(FoodTrait trait) implements ModifyCondition {

    @Override
    public boolean shouldApply(ItemStack stack, ItemStack input) {
        return input.getCapability(FoodCapability.CAPABILITY).map(f -> f.getTraits().contains(trait)).orElse(false);
    }

    @Override
    public Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public enum Serializer implements ModifyCondition.Serializer<HasTraitModifyCondition> {
        INSTANCE;

        @Override
        public HasTraitModifyCondition fromJson(JsonObject json) {
            final FoodTrait trait = FoodTrait
                    .getTraitOrThrow(new ResourceLocation(JsonHelpers.getAsString(json, "trait")));
            return new HasTraitModifyCondition(trait);
        }

        @Override
        public HasTraitModifyCondition fromNetwork(FriendlyByteBuf buffer) {
            final FoodTrait trait = FoodTrait.getTraitOrThrow(new ResourceLocation(buffer.readUtf()));
            return new HasTraitModifyCondition(trait);
        }

        @Override
        public void toNetwork(HasTraitModifyCondition modifier, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(FoodTrait.getId(modifier.trait));
        }
    }

}
