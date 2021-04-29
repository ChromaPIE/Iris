package net.coderbot.iris.gui.element;

import com.google.common.collect.ImmutableList;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ShaderPackListWidget extends ShaderScreenEntryListWidget<ShaderPackListWidget.BaseEntry> {
	public static final List<String> BUILTIN_PACKS = ImmutableList.of("(off)", "(internal)");
	private static final Text PACK_LIST_LABEL = new TranslatableText("pack.iris.list.label").formatted(Formatting.ITALIC, Formatting.GRAY);

	public ShaderPackListWidget(MinecraftClient client, int width, int height, int top, int bottom, int left, int right) {
		super(client, width, height, top, bottom, left, right, 20);
		refresh();
	}

	@Override
	public int getRowWidth() {
		return width - 4;
	}

	@Override
	protected int getRowTop(int index) {
		return super.getRowTop(index) + 2;
	}

	public void refresh() {
		this.clearEntries();
		try {
			Path path = Iris.SHADERPACK_DIR;
			int index = -1;

			for (String pack : BUILTIN_PACKS) {
				index++;
				addEntry(index, pack);
			}

			Collection<Path> folders = Files.walk(path, 1).filter(Iris::isValidShaderpack).collect(Collectors.toList());

			for (Path folder : folders) {
				String name = folder.getFileName().toString();
				if (!BUILTIN_PACKS.contains(name)) {
					index++;
					addEntry(index, name);
				}
			}

			this.addEntry(new LabelEntry(PACK_LIST_LABEL));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		super.render(matrices, mouseX, mouseY, delta);
		GuiUtil.drawCompactScrollBar(this.width - 2, this.top + 2, this.bottom - 2, this.getMaxScroll(), this.getScrollAmount(), this.getMaxPosition(), Math.max(0, Math.min(3, this.scrollbarFade + (hovered ? delta : -delta))) / 3);
		this.hovered = this.isMouseOver(mouseX, mouseY);
	}

	public void addEntry(int index, String name) {
		ShaderPackEntry entry = new ShaderPackEntry(index, this, name);
		if (Iris.getIrisConfig().areShadersEnabled()) {
			if (Iris.getIrisConfig().getShaderPackName().equals(name)) {
				this.setSelected(entry);
			}
		} else {
			if (name.equals("(off)")) {
				this.setSelected(entry);
			}
		}
		this.addEntry(entry);
	}

	public static abstract class BaseEntry extends AlwaysSelectedEntryListWidget.Entry<BaseEntry> {
		protected BaseEntry() {}
	}

	public static class ShaderPackEntry extends BaseEntry {
		private final String packName;
		private final ShaderPackListWidget list;
		private final int index;

		public ShaderPackEntry(int index, ShaderPackListWidget list, String packName) {
			this.packName = packName;
			this.list = list;
			this.index = index;
		}

		public boolean isSelected() {
			return list.getSelected() == this;
		}

		public String getPackName() {
			return packName;
		}

		@Override
		public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
			int color = 0xFFFFFF;
			String name = packName;
			if (textRenderer.getWidth(new LiteralText(name).formatted(Formatting.BOLD)) > this.list.getRowWidth() - 3) {
				name = textRenderer.trimToWidth(name, this.list.getRowWidth() - 8) + "...";
			}
			MutableText text = new LiteralText(name);
			if (this.isMouseOver(mouseX, mouseY)) {
				text = text.formatted(Formatting.BOLD);
			}
			if (this.isSelected()) {
				color = 0xFFF263;
			}
			drawCenteredText(matrices, textRenderer, text, (x + entryWidth / 2) - 2, y + (entryHeight - 11) / 2, color);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!this.isSelected() && button == 0) {
				this.list.select(this.index);
				return true;
			}
			return false;
		}
	}

	public static class LabelEntry extends BaseEntry {
		private final Text label;

		public LabelEntry(Text label) {
			this.label = label;
		}

		@Override
		public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, label, (x + entryWidth / 2) - 2, y + (entryHeight - 11) / 2, 0xC2C2C2);
		}
	}
}
