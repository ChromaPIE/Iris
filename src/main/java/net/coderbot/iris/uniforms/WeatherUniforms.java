package net.coderbot.iris.uniforms;

import static net.coderbot.iris.gl.uniform.UniformUpdateFrequency.ONCE;
import static net.coderbot.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;
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

	public static void addWeatherUniforms(UniformHolder uniforms) {
		uniforms
			.uniform1f(PER_TICK, "rainStrength", WeatherUniforms::getRainStrength)
			// TODO: Parse const float wetnessHalflife value from the shaderpack
			.uniform1f(PER_TICK, "wetness", new SmoothedFloat(600.0f, WeatherUniforms::getRainStrength));
        }

	private static float getRainStrength() {
		if (client.world == null) {
			return 0f;
		}

		return client.world.getRainGradient(CapturedRenderingState.INSTANCE.getTickDelta());
	}
}
