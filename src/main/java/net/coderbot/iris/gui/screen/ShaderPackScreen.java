package net.coderbot.iris.gui.screen;

import com.google.common.base.Throwables;
import net.coderbot.iris.Iris;
import net.coderbot.iris.config.IrisConfig;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ShaderPackScreen extends Screen implements HudHideable {
	private final Screen parent;

	private ShaderPackListWidget shaderPackList;
	private PropertyDocumentWidget shaderProperties;

	private Text addedPackDialog = null;
	private int addedPackDialogTimer = 0;

	private boolean dropChanges = false;

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

		this.shaderPackList.render(matrices, mouseX, mouseY, delta);
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

		this.shaderPackList = new ShaderPackListWidget(this.client, this.width / 2, this.height, 32, this.height - 58, 0, this.width / 2);

		if (inWorld) {
			this.shaderPackList.method_31322(false);
		}

		this.children.add(shaderPackList);

		this.refreshShaderPropertiesWidget();

		this.addButton(new ButtonWidget(bottomCenter + 104, this.height - 27, 100, 20,
			ScreenTexts.DONE, button -> onClose()));

		this.addButton(new ButtonWidget(bottomCenter, this.height - 27, 100, 20,
			new TranslatableText("options.iris.apply"), button -> this.applyChanges()));

		this.addButton(new ButtonWidget(bottomCenter - 104, this.height - 27, 100, 20,
			ScreenTexts.CANCEL, button -> this.dropChangesAndClose()));

		this.addButton(new ButtonWidget(topCenter - 78, this.height - 51, 152, 20,
			new TranslatableText("options.iris.openShaderPackFolder"), button -> openShaderPackFolder()));

		this.addButton(new ButtonWidget(topCenter + 78, this.height - 51, 152, 20,
			new TranslatableText("options.iris.refreshShaderPacks"), button -> this.shaderPackList.refresh()));

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
				Files.copy(pack, Iris.SHADERPACKS_DIRECTORY.resolve(fileName));
			} catch (FileAlreadyExistsException e) {
				this.addedPackDialog = new TranslatableText(
						"options.iris.shaderPackSelection.copyErrorAlreadyExists",
						fileName
				).formatted(Formatting.ITALIC, Formatting.RED);

				this.addedPackDialogTimer = 100;
				this.shaderPackList.refresh();

				return;
			} catch (IOException e) {
				Iris.logger.warn("Error copying dragged shader pack", e);

				this.addedPackDialog = new TranslatableText(
						"options.iris.shaderPackSelection.copyError",
						fileName
				).formatted(Formatting.ITALIC, Formatting.RED);

				this.addedPackDialogTimer = 100;
				this.shaderPackList.refresh();

				return;
			}
		}

		// After copying the relevant files over to the folder, make sure to refresh the shader pack list.
		this.shaderPackList.refresh();

		if (packs.size() == 0) {
			// If zero packs were added, then notify the user that the files that they added weren't actually shader
			// packs.

			if (paths.size() == 1) {
				// If a single pack could not be added, provide a message with that pack in the file name
				String fileName = paths.get(0).getFileName().toString();

				this.addedPackDialog = new TranslatableText(
					"options.iris.shaderPackSelection.failedAddSingle",
					fileName
				).formatted(Formatting.ITALIC, Formatting.RED);
			} else {
				// Otherwise, show a generic message.

				this.addedPackDialog = new TranslatableText(
					"options.iris.shaderPackSelection.failedAdd"
				).formatted(Formatting.ITALIC, Formatting.RED);
			}

		} else if (packs.size() == 1) {
			// In most cases, users will drag a single pack into the selection menu. So, let's special case it.
			String packName = packs.get(0).getFileName().toString();

			this.addedPackDialog = new TranslatableText(
					"options.iris.shaderPackSelection.addedPack",
					packName
			).formatted(Formatting.ITALIC, Formatting.YELLOW);

			// Select the pack that the user just added, since if a user just dragged a pack in, they'll probably want
			// to actually use that pack afterwards.
			this.shaderPackList.select(packName);
		} else {
			// We also support multiple packs being dragged and dropped at a time. Just show a generic success message
			// in that case.
			this.addedPackDialog = new TranslatableText(
					"options.iris.shaderPackSelection.addedPacks",
					packs.size()
			).formatted(Formatting.ITALIC, Formatting.YELLOW);
		}

		// Show the relevant message for 5 seconds (100 ticks)
		this.addedPackDialogTimer = 100;
	}

	@Override
	public void onClose() {
		if (!dropChanges) {
			applyChanges();
		}

		ScreenStack.pull(this.getClass());
		client.openScreen(ScreenStack.pop());
	}

	private void dropChangesAndClose() {
		dropChanges = true;
		onClose();
	}

	private void applyChanges() {
		ShaderPackListWidget.BaseEntry base = this.shaderPackList.getSelected();

		if (!(base instanceof ShaderPackListWidget.ShaderPackEntry)) {
			return;
		}

		ShaderPackListWidget.ShaderPackEntry entry = (ShaderPackListWidget.ShaderPackEntry)base;
		IrisConfig config = Iris.getIrisConfig();

		String name = entry.getPackName();
		if (name.equals("(off)")) {
			if (!config.areShadersEnabled()) return;

			config.setShadersDisabled();
		} else {
			boolean changed = this.shaderProperties.saveProperties();
			if (config.areShadersEnabled() && name.equals(config.getShaderPackName()) && !changed) return;

			config.setShadersEnabled();
			config.setShaderPackName(name);
		}

		try {
			Iris.reload();
		} catch (IOException e) {
			Iris.logger.error("Error reloading shader pack while applying changes!");
			Iris.logger.catching(e);

			if (this.client.player != null) {
				this.client.player.sendMessage(new TranslatableText("iris.shaders.reloaded.failure", Throwables.getRootCause(e).getMessage()).formatted(Formatting.RED), false);
			}

			Iris.getIrisConfig().setShadersDisabled();
			// Set the selected shaderpack to off in the gui
			this.shaderPackList.select(0);
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
				return false;
			}

			AtomicBoolean propertiesChanged = new AtomicBoolean(false);

			ShaderPackConfig config = shaderPack.getConfig();
			for (String pageName : shaderProperties.getPages()) {
				PropertyList propertyList = shaderProperties.getPage(pageName);
				propertyList.forEvery(property -> {
					if (property instanceof OptionProperty) {
						String key = ((OptionProperty<?>)property).getKey();
						if (property instanceof IntOptionProperty) {
							Option<Integer> opt = config.getIntegerOption(key);
							if (opt != null && !opt.getValue().equals(((IntOptionProperty) property).getValue())) {
								opt.setValue(((IntOptionProperty) property).getValue());
								opt.save(config.getConfigProperties());
								propertiesChanged.set(true);
							}
						} else if (property instanceof FloatOptionProperty) {
							Option<Float> opt = config.getFloatOption(key);
							if (opt != null && !opt.getValue().equals(((OptionProperty<?>) property).getValue())) {
								opt.setValue(((FloatOptionProperty) property).getValue());
								opt.save(config.getConfigProperties());
								propertiesChanged.set(true);
							}
						} else if (property instanceof BooleanOptionProperty) {
							Option<Boolean> opt = config.getBooleanOption(key);
							if (opt != null && !opt.getValue().equals(((BooleanOptionProperty) property).getValue())) {
								opt.setValue(((BooleanOptionProperty) property).getValue());
								opt.save(config.getConfigProperties());
								propertiesChanged.set(true);
							}
						}
					}
				});
			}
			try {
				config.save();
				config.load();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return propertiesChanged.get();
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
			this.shaderProperties.setDocument(PropertyDocumentWidget.createShaderpackConfigDocument(this.client.textRenderer, this.width / 2, "Shaders Disabled", null, this.shaderProperties), "screen");
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

	private void openShaderPackFolder() {
		Util.getOperatingSystem().open(Iris.SHADERPACKS_DIRECTORY.toFile());
	}
}
