package com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.dries007.tfc.common.recipes.outputs.ItemStackModifier;
import net.dries007.tfc.common.recipes.outputs.ItemStackModifiers;
import net.dries007.tfc.util.JsonHelpers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record ConditionalItemStackModifier(ModifyCondition condition, ItemStackModifier[] nestedModifiers,
        ItemStackModifier[] elseNestedModifiers)
        implements ItemStackModifier {

    @Override
    public ItemStack apply(ItemStack stack, ItemStack input) {
        if (this.condition.shouldApply(stack, input)) {
            for (ItemStackModifier mod : nestedModifiers) {
                stack = mod.apply(stack, input);
            }
        } else {
            for (ItemStackModifier mod : elseNestedModifiers) {
                stack = mod.apply(stack, input);
            }
        }
        return stack;
    }

    @Override
    public Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public enum Serializer implements ItemStackModifier.Serializer<ConditionalItemStackModifier> {
        INSTANCE;

        @Override
        public ConditionalItemStackModifier fromJson(JsonObject json) {
            final ModifyCondition condition = ModifyConditions
                    .fromJson(JsonHelpers.getAsJsonObject(json, "condition"));

            final ItemStackModifier[] modifiers = getModifers(JsonHelpers.getAsJsonArray(json, "modifiers"));
            final ItemStackModifier[] elseModifiers = getModifers(
                    JsonHelpers.getAsJsonArray(json, "else_modifiers", new JsonArray()));

            return new ConditionalItemStackModifier(condition, modifiers, elseModifiers);
        }

        private static ItemStackModifier[] getModifers(JsonArray modifiersJson) {
            final ItemStackModifier[] modifiers = new ItemStackModifier[modifiersJson.size()];

            for (int i = 0; i < modifiers.length; i++) {
                modifiers[i] = ItemStackModifiers.fromJson(modifiersJson.get(i));
            }

            return modifiers;
        }

        @Override
        public ConditionalItemStackModifier fromNetwork(FriendlyByteBuf buffer) {
            final ModifyCondition condition = ModifyConditions.fromNetwork(buffer);
            final ItemStackModifier[] modifiers = getModifersFromNetwork(buffer);
            final ItemStackModifier[] elseModifiers = getModifersFromNetwork(buffer);
            return new ConditionalItemStackModifier(condition, modifiers, elseModifiers);
        }

        private static ItemStackModifier[] getModifersFromNetwork(FriendlyByteBuf buffer) {
            final int count = buffer.readVarInt();
            final ItemStackModifier[] modifiers = new ItemStackModifier[count];
            for (int i = 0; i < count; i++) {
                modifiers[i] = ItemStackModifiers.fromNetwork(buffer);
            }

            return modifiers;
        }

        @Override
        public void toNetwork(ConditionalItemStackModifier modifier, FriendlyByteBuf buffer) {
            modifier.condition.toNetwork(buffer);
            modifiersToNetwork(modifier.nestedModifiers, buffer);
            modifiersToNetwork(modifier.elseNestedModifiers, buffer);
        }

        private static void modifiersToNetwork(ItemStackModifier[] modifiers, FriendlyByteBuf buffer) {
            buffer.writeVarInt(modifiers.length);
            for (ItemStackModifier mod : modifiers) {
                mod.toNetwork(buffer);
            }
        }
    }

}
