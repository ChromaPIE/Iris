package net.coderbot.iris.uniforms;

import static net.coderbot.iris.gl.uniform.UniformUpdateFrequency.PER_TICK;

import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.uniforms.transforms.SmoothedFloat;

import net.minecraft.client.MinecraftClient;

/**
 * @see <a href="https://github.com/IrisShaders/ShaderDoc/blob/master/uniforms.md#weather">Uniforms: Weather</a>
 */
public class WeatherUniforms {
	private static final MinecraftClient client = MinecraftClient.getInstance();

	private WeatherUniforms() {
	}

	public static void addWeatherUniforms(UniformHolder uniforms, FrameUpdateNotifier updateNotifier) {
		uniforms
			.uniform1f(PER_TICK, "rainStrength", WeatherUniforms::getRainStrength)
			// TODO: Parse the value of const float wetnessHalflife from the shaderpacks' fragment configuration
			.uniform1f(PER_TICK, "wetness", new SmoothedFloat(600f, WeatherUniforms::getRainStrength, updateNotifier));
  	}

	private static float getRainStrength() {
		if (client.world == null) {
			return 0f;
		}

		return client.world.getRainGradient(CapturedRenderingState.INSTANCE.getTickDelta());
	}
}
