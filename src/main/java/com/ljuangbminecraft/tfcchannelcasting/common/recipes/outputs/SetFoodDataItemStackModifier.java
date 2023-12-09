package com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs;

import com.google.gson.JsonObject;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.dries007.tfc.common.recipes.outputs.ItemStackModifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record SetFoodDataItemStackModifier(FoodData foodData)
        implements ItemStackModifier {

    @Override
    public ItemStack apply(ItemStack stack, ItemStack input) {
        stack.getCapability(FoodCapability.CAPABILITY).ifPresent(food -> {
            if (food instanceof FoodHandler.Dynamic handler) {
                handler.setFood(foodData);
                handler.setCreationDate(FoodCapability.getRoundedCreationDate());
            }
        });
        return stack;
    }

    @Override
    public Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public enum Serializer implements ItemStackModifier.Serializer<SetFoodDataItemStackModifier> {
        INSTANCE;

        @Override
        public SetFoodDataItemStackModifier fromJson(JsonObject json) {
            return new SetFoodDataItemStackModifier(FoodData.read(json));
        }

        @Override
        public SetFoodDataItemStackModifier fromNetwork(FriendlyByteBuf buffer) {
            return new SetFoodDataItemStackModifier(FoodData.decode(buffer));
        }

        @Override
        public void toNetwork(SetFoodDataItemStackModifier modifier, FriendlyByteBuf buffer) {
            modifier.foodData.encode(buffer);
        }
    }

}
