package com.symbiote.evolution.client;

import com.symbiote.evolution.SymbioteEvolution;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SymbioteClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static KeyBinding formKey;
    private static KeyBinding talkKey;
    private static KeyBinding abilityKey;

    public static final net.minecraft.client.render.entity.model.EntityModelLayer MODEL_LAYER =
            new net.minecraft.client.render.entity.model.EntityModelLayer(
                    new net.minecraft.util.Identifier(SymbioteEvolution.MOD_ID, "symbiote"), "main");

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.symbiote_evolution.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
                "category.symbiote_evolution"));
        formKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.symbiote_evolution.form", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H,
                "category.symbiote_evolution"));
        talkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.symbiote_evolution.talk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y,
                "category.symbiote_evolution"));
        abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.symbiote_evolution.ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V,
                "category.symbiote_evolution"));

        EntityModelLayerRegistry.registerModelLayer(MODEL_LAYER, SymbioteModel::getTexturedModelData);
        EntityRendererRegistry.register(SymbioteEvolution.SYMBIOTE, SymbioteRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(
                        SymbioteEvolution.TOGGLE_PACKET, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }
            while (formKey.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(
                        SymbioteEvolution.FORM_PACKET, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }
            while (talkKey.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(
                        SymbioteEvolution.TALK_PACKET, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }
            while (abilityKey.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(
                        SymbioteEvolution.ABILITY_PACKET, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }
        });
    }
}
