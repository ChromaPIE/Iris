package net.coderbot.iris.mixin;

import net.coderbot.iris.gui.screen.HudHideable;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.GameRenderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public class MixinGameRenderer {
	@Shadow @Final private MinecraftClient client;
	// TODO: This probably won't be compatible with mods that directly mess with the GL projection matrix.
	// https://github.com/jellysquid3/sodium-fabric/blob/1df506fd39dac56bb410725c245e6e51208ec732/src/main/java/me/jellysquid/mods/sodium/client/render/chunk/shader/ChunkProgram.java#L56
	@Inject(method = "loadProjectionMatrix(Lnet/minecraft/util/math/Matrix4f;)V", at = @At("HEAD"))
	private void iris$captureProjectionMatrix(Matrix4f projectionMatrix, CallbackInfo callback) {
		CapturedRenderingState.INSTANCE.setGbufferProjection(projectionMatrix);
	}

	@Inject(method = "render(FJZ)V", at = @At("HEAD"))
	private void iris$beginFrame(float tickDelta, long startTime, boolean tick, CallbackInfo callback) {
		SystemTimeUniforms.COUNTER.beginFrame();
		SystemTimeUniforms.TIMER.beginFrame(startTime);
	}

	@Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
	public void iris$handleHudHidingScreens(CallbackInfoReturnable<Boolean> cir) {
		if (this.client.currentScreen instanceof HudHideable) cir.setReturnValue(false);
	}
}
