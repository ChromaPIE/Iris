package net.coderbot.iris.gui.option;

import net.coderbot.iris.gui.ShaderPackScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Option;
import net.minecraft.text.TranslatableText;

public class ShaderPackScreenButtonOption extends Option {
	private final Screen parent;
	private final MinecraftClient client;

	public ShaderPackScreenButtonOption(Screen parent, MinecraftClient client) {
		super("options.iris.shaderPackSelection");
		this.parent = parent;
		this.client = client;
	}

	@Override
	public AbstractButtonWidget createButton(GameOptions options, int x, int y, int width) {
		return new ButtonWidget(x, y, width, 20, new TranslatableText("options.iris.shaderPackSelection"), (buttonWidget) -> client.openScreen(new ShaderPackScreen(this.parent)));
	}
}
