package com.jumpreset;

import com.jumpreset.config.ModConfig;
import com.jumpreset.state.JumpResetTracker;
import com.jumpreset.ui.ConfigScreen;
import com.jumpreset.ui.CrosshairIndicator;
import com.jumpreset.ui.JumpResetHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class JumpResetMod implements ClientModInitializer {

    public static final String MOD_ID = "jumpreset";

    // Singleton instances — initialized once in onInitializeClient()
    public static JumpResetTracker  tracker;
    public static JumpResetHud      hud;
    public static CrosshairIndicator crosshairIndicator;

    /**
     * Pre-physics vy captured by mixin at HEAD of move().
     * Written by the mixin; read + cleared in END_CLIENT_TICK.
     */
    public static volatile double  preMoveVelocityY = 0.0;

    /**
     * Set TRUE by the mixin's velocity-signature check inside move():
     *   vy ∈ [0.38, 0.85] + MovementType.SELF + isOnGround()
     * This fires only for real ground jumps. Knockback never satisfies
     * all three conditions simultaneously.
     * Read + cleared atomically at the start of END_CLIENT_TICK.
     */
    public static volatile boolean jumpingThisTick = false;

    private static KeyBinding openConfigKey;
    private static KeyBinding toggleFeedbackKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        tracker            = new JumpResetTracker();
        hud                = new JumpResetHud();
        crosshairIndicator = new CrosshairIndicator();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.jumpreset.config",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, cat));

        toggleFeedbackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.jumpreset.toggle",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, cat));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // jumpingThisTick is still written by the mixin (kept for
            // compatibility) but jump detection now uses delta-vy in the tracker.
            jumpingThisTick = false;

            if (client.player != null && client.world != null) {
                tracker.tick(client);
            }

            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null)
                    client.setScreen(new ConfigScreen(null));
            }
            while (toggleFeedbackKey.wasPressed()) {
                boolean nowEnabled = !ModConfig.get().enabled;
                ModConfig.get().enabled = nowEnabled;
                if (client.player != null) {
                    client.player.sendMessage(
                        net.minecraft.text.Text.literal(
                            "[JumpReset] " + (nowEnabled ? "§aEnabled" : "§cDisabled")),
                        true);
                }
            }
        });

        // Main timing HUD (result panel)
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                Identifier.of(MOD_ID, "timing_hud"),
                (ctx, tickCounter) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null
                            && !(client.currentScreen instanceof ConfigScreen)) {
                        hud.render(ctx, client);
                    }
                }
        );

        // Crosshair indicator (triangle above crosshair)
        // Registered after the main HUD so it draws on top.
        HudElementRegistry.attachElementAfter(
                Identifier.of(MOD_ID, "timing_hud"),
                Identifier.of(MOD_ID, "crosshair_indicator"),
                (ctx, tickCounter) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null
                            && !(client.currentScreen instanceof ConfigScreen)) {
                        crosshairIndicator.render(ctx, client);
                    }
                }
        );
    }
}
