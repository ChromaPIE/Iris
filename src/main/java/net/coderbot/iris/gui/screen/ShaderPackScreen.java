package net.coderbot.iris.gui.screen;

import com.google.common.base.Throwables;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.GuiUtil;
import net.coderbot.iris.gui.ScreenStack;
import net.coderbot.iris.gui.element.PropertyDocumentWidget;
import net.coderbot.iris.gui.element.ShaderPackListWidget;
import net.coderbot.iris.gui.property.*;
import net.coderbot.iris.shaderpack.Option;
import net.coderbot.iris.shaderpack.ShaderPack;
import net.coderbot.iris.shaderpack.ShaderPackConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.TickableElement;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ShaderPackScreen extends Screen implements HudHideable {
	private final Screen parent;

	private ShaderPackListWidget shaderPacks;
	private PropertyDocumentWidget shaderProperties;

	private Text addedPackDialog = null;
	private int addedPackDialogTimer = 0;

	public ShaderPackScreen(Screen parent) {
		super(new TranslatableText("options.iris.shaderPackSelection.title"));
		this.parent = parent;
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		if (this.client.world == null) {
			this.renderBackground(matrices);
		} else {
			this.fillGradient(matrices, 0, 0, width, height, 0x4F232323, 0x4F232323);
		}

		this.shaderPacks.render(matrices, mouseX, mouseY, delta);
		this.shaderProperties.render(matrices, mouseX, mouseY, delta);

		GuiUtil.drawDirtTexture(client, 0, 0, -100, width, 32);
		GuiUtil.drawDirtTexture(client, 0, this.height - 58, -100, width, 58);
		drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 16777215);

		if (addedPackDialog != null && addedPackDialogTimer > 0) {
			drawCenteredText(matrices, this.textRenderer, addedPackDialog, (int) (this.width * 0.5), 21, 0xFFFFFF);
		} else {
			drawCenteredText(matrices, this.textRenderer, new TranslatableText("pack.iris.select.title").formatted(Formatting.GRAY, Formatting.ITALIC), (int)(this.width * 0.25), 21, 16777215);
			drawCenteredText(matrices, this.textRenderer, new TranslatableText("pack.iris.configure.title").formatted(Formatting.GRAY, Formatting.ITALIC), (int)(this.width * 0.75), 21, 16777215);
		}

		super.render(matrices, mouseX, mouseY, delta);
	}

	@Override
	protected void init() {
		super.init();
		int bottomCenter = this.width / 2 - 50;
		int topCenter = this.width / 2 - 76;
		boolean inWorld = this.client.world != null;

		this.shaderPacks = new ShaderPackListWidget(this.client, this.width / 2, this.height, 32, this.height - 58, 0, this.width / 2);
		if (inWorld) {
			this.shaderPacks.method_31322(false);
		}
		this.children.add(shaderPacks);

		this.refreshShaderPropertiesWidget();

		this.addButton(new ButtonWidget(bottomCenter + 104, this.height - 27, 100, 20, ScreenTexts.DONE, button -> {
			applyChanges();
			onClose();
		}));
		this.addButton(new ButtonWidget(bottomCenter, this.height - 27, 100, 20, new TranslatableText("options.iris.apply"), button -> this.applyChanges()));
		this.addButton(new ButtonWidget(bottomCenter - 104, this.height - 27, 100, 20, ScreenTexts.CANCEL, button -> this.onClose()));
		this.addButton(new ButtonWidget(topCenter - 78, this.height - 51, 152, 20, new TranslatableText("options.iris.openShaderPackFolder"), button -> Util.getOperatingSystem().open(Iris.SHADERPACK_DIR.toFile())));
		this.addButton(new ButtonWidget(topCenter + 78, this.height - 51, 152, 20, new TranslatableText("options.iris.refreshShaderPacks"), button -> this.shaderPacks.refresh()));
		this.addButton(new IrisConfigScreenButtonWidget(this.width - 26, 6, button -> client.openScreen(new IrisConfigScreen(this))));

		if (parent != null) {
			ScreenStack.push(parent);
		}
	}

	@Override
	public void tick() {
		for (Element e : this.children) {
			if (e instanceof TickableElement) ((TickableElement)e).tick();
		}
		if (this.addedPackDialogTimer > 0) {
			this.addedPackDialogTimer--;
		}
	}

	@Override
	public void filesDragged(List<Path> paths) {
		List<Path> packs = paths.stream().filter(Iris::isValidShaderpack).collect(Collectors.toList());
		for (Path pack : packs) {
			String fileName = pack.getFileName().toString();
			try {
				Files.copy(pack, Iris.SHADERPACK_DIR.resolve(fileName));
			} catch (IOException e) {
				e.printStackTrace();
				this.addedPackDialog = new TranslatableText(
						"options.iris.shaderPackSelection.copyError",
						fileName
				).formatted(Formatting.ITALIC, Formatting.RED);
				this.addedPackDialogTimer = 100;
				this.shaderPacks.refresh();
				return;
			}
		}
		this.shaderPacks.refresh();
		if (packs.size() > 0) {
			if (packs.size() == 1) {
				String packName = packs.get(0).getFileName().toString();
				this.addedPackDialog = new TranslatableText(
						"options.iris.shaderPackSelection.addedPack",
						packName
				).formatted(Formatting.ITALIC, Formatting.YELLOW);
				this.shaderPacks.select(packName);
			} else {
				this.addedPackDialog = new TranslatableText(
						"options.iris.shaderPackSelection.addedPacks",
						packs.size()
				).formatted(Formatting.ITALIC, Formatting.YELLOW);
			}
		} else {
			this.addedPackDialog = new TranslatableText(
					"options.iris.shaderPackSelection.failedAdd"
			).formatted(Formatting.ITALIC, Formatting.RED);
		}
		this.addedPackDialogTimer = 100;
	}

	@Override
	public void onClose() {
		ScreenStack.pull(this.getClass());
		client.openScreen(ScreenStack.pop());
	}

	private void applyChanges() {
		ShaderPackListWidget.BaseEntry base = this.shaderPacks.getSelected();
		if (!(base instanceof ShaderPackListWidget.ShaderPackEntry)) {
			return;
		}
		ShaderPackListWidget.ShaderPackEntry entry = (ShaderPackListWidget.ShaderPackEntry)base;
		String name = entry.getPackName();
		if (name.equals("(off)")) {
			Iris.getIrisConfig().setShadersDisabled();
		} else {
			Iris.getIrisConfig().setShadersEnabled();
			Iris.getIrisConfig().setShaderPackName(name);
		}
		this.shaderProperties.saveProperties();
		try {
			Iris.reload();
		} catch (IOException e) {
			Iris.logger.error("Error while switching Shaders for Iris!", e);

			if (this.client.player != null) {
				this.client.player.sendMessage(new TranslatableText("iris.shaders.reloaded.failure", Throwables.getRootCause(e).getMessage()).formatted(Formatting.RED), false);
			}

			Iris.getIrisConfig().setShadersDisabled();
			// Set the selected shaderpack to off in the gui
			this.shaderPacks.select(0);
		}
		this.reloadShaderConfig();
	}

	private void refreshShaderPropertiesWidget() {
		this.children.remove(shaderProperties);

		float scrollAmount = 0.0f;
		String page = "screen";

		if (this.shaderProperties != null) {
			scrollAmount = (float)this.shaderProperties.getScrollAmount() / this.shaderProperties.getMaxScroll();
			page = this.shaderProperties.getCurrentPage();
		}
		if (shaderProperties != null) this.shaderProperties.saveProperties();
		this.shaderProperties = new PropertyDocumentWidget(this.client, this.width / 2, this.height, 32, this.height - 58, this.width / 2, this.width, 26);
		shaderProperties.onSave(() -> {
			ShaderPack shaderPack = Iris.getCurrentPack().orElse(null);
			if (shaderPack == null) {
				return;
			}

			ShaderPackConfig config = shaderPack.getConfig();
			for (String pageName : shaderProperties.getPages()) {
				PropertyList propertyList = shaderProperties.getPage(pageName);
				propertyList.forEvery(property -> {
					if (property instanceof OptionProperty) {
						String key = ((OptionProperty<?>)property).getKey();
						config.getConfigProperties().setProperty(key, ((OptionProperty<?>)property).getValue().toString());
					}
				});
			}
			try {
				config.save();
				config.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		shaderProperties.onLoad(() -> {
			ShaderPack shaderPack = Iris.getCurrentPack().orElse(null);
			if (shaderPack == null) {
				return;
			}

			ShaderPackConfig config = shaderPack.getConfig();
			for (String pageName : shaderProperties.getPages()) {
				PropertyList propertyList = shaderProperties.getPage(pageName);
				propertyList.forEvery(property -> {
					if (property instanceof OptionProperty) {
						String key = ((OptionProperty<?>)property).getKey();
						if (property instanceof IntOptionProperty) {
							Option<Integer> opt = config.getIntegerOption(key);
							if (opt != null) ((IntOptionProperty)property).setValue(opt.getValue());
						} else if (property instanceof FloatOptionProperty) {
							Option<Float> opt = config.getFloatOption(key);
							if (opt != null) ((FloatOptionProperty)property).setValue(opt.getValue());
						} else if (property instanceof BooleanOptionProperty) {
							Option<Boolean> opt = config.getBooleanOption(key);
							if (opt != null) ((BooleanOptionProperty)property).setValue(opt.getValue());
						}
					}
				});
			}
			try {
				config.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		if (this.client.world != null) this.shaderProperties.method_31322(false);
		this.reloadShaderConfig();

		this.shaderProperties.goTo(page);
		this.shaderProperties.setScrollAmount(this.shaderProperties.getMaxScroll() * scrollAmount);

		this.children.add(shaderProperties);
	}

	private void reloadShaderConfig() {
		ShaderPack shaderPack = Iris.getCurrentPack().orElse(null);
		if (shaderPack == null) {
			this.shaderProperties.setDocument(PropertyDocumentWidget.createShaderpackConfigDocument(this.client.textRenderer, this.width / 2, Iris.getIrisConfig().getShaderPackName(), null, this.shaderProperties), "screen");
			shaderProperties.loadProperties();
			return;
		}
		this.shaderProperties.setDocument(PropertyDocumentWidget.createShaderpackConfigDocument(this.client.textRenderer, this.width / 2, Iris.getIrisConfig().getShaderPackName(), shaderPack, this.shaderProperties), "screen");
		shaderProperties.loadProperties();
	}

	public class IrisConfigScreenButtonWidget extends ButtonWidget {
		public IrisConfigScreenButtonWidget(int x, int y, PressAction press) {
			super(x, y, 20, 20, LiteralText.EMPTY, press);
		}

		@Override
		public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
			MinecraftClient.getInstance().getTextureManager().bindTexture(GuiUtil.WIDGETS_TEXTURE);
			drawTexture(matrices, x, y, isMouseOver(mouseX, mouseY) ? 20 : 0, 0, 20, 20);

			if (isMouseOver(mouseX, mouseY)) {
				renderTooltip(matrices, new TranslatableText("tooltip.iris.config"), mouseX, mouseY);
			}
		}
	}
}
