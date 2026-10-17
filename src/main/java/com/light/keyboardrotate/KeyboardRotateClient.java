package com.light.keyboardrotate;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent; // Событие каждого кадра отрисовки
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "keyboardrotate", dist = Dist.CLIENT)
public class KeyboardRotateClient {

    public static final KeyMapping KEY_LOOK_UP = new KeyMapping(
            "key.keyboardrotate.up", InputConstants.KEY_UP, "category.keyboardrotate");
    public static final KeyMapping KEY_LOOK_DOWN = new KeyMapping(
            "key.keyboardrotate.down", InputConstants.KEY_DOWN, "category.keyboardrotate");
    public static final KeyMapping KEY_LOOK_LEFT = new KeyMapping(
            "key.keyboardrotate.left", InputConstants.KEY_LEFT, "category.keyboardrotate");
    public static final KeyMapping KEY_LOOK_RIGHT = new KeyMapping(
            "key.keyboardrotate.right", InputConstants.KEY_RIGHT, "category.keyboardrotate");

    // ЧУВСТВИТЕЛЬНОСТЬ (ГРАДУСЫ В СЕКУНДУ)

    // Это скорость поворота в градусах за одну реальную секунду.
    // 180.0F = полный оборот (360°) за 2 секунды (комфортно, как обычная мышь).
    // 360.0F = полный оборот за 1 секунду (быстро, как в шутерах).
    // 90.0F = полный оборот за 4 секунды (медленно, для точного прицеливания).
    private static final float SENSITIVITY_DEGREES_PER_SECOND = 185.0F;

    // Переменная для точного расчета времени между кадрами
    private long lastTimeNanos = System.nanoTime();

    public KeyboardRotateClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerKeyMappings);
        // Подписываемся на событие КАЖДОГО КАДРА отрисовки (не логического тика)
        NeoForge.EVENT_BUS.addListener(this::onRenderFrame);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_LOOK_UP);
        event.register(KEY_LOOK_DOWN);
        event.register(KEY_LOOK_LEFT);
        event.register(KEY_LOOK_RIGHT);
    }

    private void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        // Если мы не в мире или игра на паузе - сбрасываем таймер и выходим
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            lastTimeNanos = System.nanoTime();
            return;
        }

        // 1. Считаем реальное время, прошедшее с прошлого кадра (в секундах)
        long currentTimeNanos = System.nanoTime();
        float deltaTime = (currentTimeNanos - lastTimeNanos) / 1_000_000_000.0F;
        lastTimeNanos = currentTimeNanos;

        // Защита от резкой смены кадра, если игра микро-зависла (например, на загрузке чанков)
        // Максимум считаем 50мс (один логический тик), чтобы камера не улетала
        if (deltaTime > 0.05F) {
            deltaTime = 0.05F;
        }

        // 2. Вычисляем, на сколько градусов нужно повернуть камеру именно за этот кадр
        float yawDelta = 0;
        float pitchDelta = 0;

        if (KEY_LOOK_LEFT.isDown()) yawDelta -= SENSITIVITY_DEGREES_PER_SECOND * deltaTime;
        if (KEY_LOOK_RIGHT.isDown()) yawDelta += SENSITIVITY_DEGREES_PER_SECOND * deltaTime;
        if (KEY_LOOK_UP.isDown()) pitchDelta -= SENSITIVITY_DEGREES_PER_SECOND * deltaTime;
        if (KEY_LOOK_DOWN.isDown()) pitchDelta += SENSITIVITY_DEGREES_PER_SECOND * deltaTime;

        // 3. Применяем изменение
        if (yawDelta != 0 || pitchDelta != 0) {
            float newYaw = mc.player.getYRot() + yawDelta;
            float newPitch = mc.player.getXRot() + pitchDelta;

            // Ограничиваем наклон вверх/вниз (защита от перелома шеи :) )
            newPitch = Mth.clamp(newPitch, -90.0F, 90.0F);
            // Зацикливаем горизонтальный поворот
            newYaw = Mth.wrapDegrees(newYaw);

            // Обновляем реальные углы игрока
            mc.player.setYRot(newYaw);
            mc.player.setXRot(newPitch);

            mc.player.yRotO = newYaw;
            mc.player.xRotO = newPitch;
        }
    }
}