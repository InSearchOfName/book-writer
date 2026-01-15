package name.insearchof.screen;

import name.insearchof.event.BookWriterEvents;
import name.insearchof.util.InventoryUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class WriteScreen extends Screen {
	private static final int CHARS_PER_PAGE = 256;
	private static final int MAX_PAGES_PER_BOOK = 100;
	private static final int TITLE_MAX_LENGTH = 128;
	private static final int TEXT_MAX_LENGTH = 1000000;
	private static final int PADDING = 20;
	private static final int FIELD_HEIGHT = 20;
	private static final int TITLE_Y_OFFSET = 0;
	private static final int CONTENT_Y_OFFSET = 50;
	private static final int LABEL_OFFSET = 8;
	private static final int INPUT_OFFSET = 12;

	private static String storedText = "";
	private static String storedTitle = "Book";

	private TextFieldWidget titleField;
	private TextFieldWidget textField;

	public WriteScreen() {
		super(Text.literal("Book Writer"));
	}

	public static String getStoredText() {
		return storedText;
	}

	public static void setStoredText(String text) {
		storedText = text;
	}

	@Override
	protected void init() {
		super.init();
		initializeFields();
		initializeButtons();
	}

	private void initializeFields() {
		// Title label
		TextWidget titleLabel = new TextWidget(PADDING, PADDING + TITLE_Y_OFFSET - LABEL_OFFSET, 100, FIELD_HEIGHT, 
			Text.literal("Book Title:"), this.textRenderer);
		this.addDrawableChild(titleLabel);

		// Title field
		this.titleField = new TextFieldWidget(
			this.textRenderer,
			PADDING, PADDING + TITLE_Y_OFFSET + INPUT_OFFSET,
			this.width - 2 * PADDING, FIELD_HEIGHT,
			Text.literal("Book Title")
		);
		this.titleField.setMaxLength(TITLE_MAX_LENGTH);
		this.titleField.setText(storedTitle);
		this.addDrawableChild(this.titleField);

		// Content label
		TextWidget contentLabel = new TextWidget(PADDING, PADDING + CONTENT_Y_OFFSET - LABEL_OFFSET, 100, FIELD_HEIGHT,
			Text.literal("Content:"), this.textRenderer);
		this.addDrawableChild(contentLabel);

		// Content field
		this.textField = new TextFieldWidget(
			this.textRenderer,
			PADDING, PADDING + CONTENT_Y_OFFSET + INPUT_OFFSET,
			this.width - 2 * PADDING, FIELD_HEIGHT,
			Text.literal("Paste text here...")
		);
		this.textField.setMaxLength(TEXT_MAX_LENGTH);
		this.textField.setText(storedText);
		this.addDrawableChild(this.textField);
	}

	private void initializeButtons() {
		int buttonY = this.height - 40;
		int buttonWidth = 100;
		int buttonHeight = 20;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Write to Book"), button -> onWriteClick())
			.dimensions(this.width / 2 - 210, buttonY, buttonWidth, buttonHeight)
			.build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Calculate"), button -> onCalculateClick())
			.dimensions(this.width / 2 - 55, buttonY, buttonWidth, buttonHeight)
			.build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), button -> onClearClick())
			.dimensions(this.width / 2 + 105, buttonY, buttonWidth, buttonHeight)
			.build());
	}

	private void onWriteClick() {
		String text = this.textField.getText();
		String title = this.titleField.getText();

		if (text.isEmpty()) {
			if (this.client != null && this.client.player != null) {
				this.client.player.sendMessage(Text.literal("§cNo text to write!"), true);
			}
			this.close();
			return;
		}

		if (this.client == null || this.client.player == null) {
			return;
		}

		int booksNeeded = calculateBooksNeeded(text.length());
		int booksAvailable = InventoryUtils.countWritableBooks(this.client.player);

		if (booksAvailable < booksNeeded) {
			String message = String.format("§c§lNot enough books! §r§7Need: %d, Have: %d", booksNeeded, booksAvailable);
			this.client.player.sendMessage(Text.literal(message), true);
			this.close();
			return;
		}

		BookWriterEvents.WRITE_BOOK.invoker().onWriteBook(this.client.player, title, text);
		storedText = text;
		storedTitle = title;
		this.close();
	}

	private void onCalculateClick() {
		String text = this.textField.getText();
		int totalPages = calculateTotalPages(text.length());
		int booksNeeded = calculateBooksNeeded(text.length());

		if (this.client == null || this.client.player == null) {
			return;
		}

		int booksAvailable = InventoryUtils.countWritableBooks(this.client.player);
		String message = formatCalculateMessage(booksNeeded, booksAvailable, totalPages);
		this.client.player.sendMessage(Text.literal(message), true);
		this.close();
	}

	private void onClearClick() {
		this.textField.setText("");
		this.titleField.setText("Book");
	}

	private int calculateTotalPages(int charCount) {
		int pages = (int) Math.ceil((double) charCount / CHARS_PER_PAGE);
		return Math.max(pages, 1);
	}

	private int calculateBooksNeeded(int charCount) {
		int totalPages = calculateTotalPages(charCount);
		return (int) Math.ceil((double) totalPages / MAX_PAGES_PER_BOOK);
	}

	private String formatCalculateMessage(int booksNeeded, int booksAvailable, int totalPages) {
		if (booksAvailable < booksNeeded) {
			return String.format("§c§lNot enough books! §r§7Need: %d, Have: %d", booksNeeded, booksAvailable);
		}
		return String.format("§a§lBooks Needed: %d §r§7(%d pages)", booksNeeded, totalPages);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return this.textField.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0xAA000000);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);

		super.render(context, mouseX, mouseY, delta);

		String charCount = "Characters: " + this.textField.getText().length();
		int countX = this.width - PADDING - this.textRenderer.getWidth(charCount);
		context.drawTextWithShadow(this.textRenderer, charCount, countX, PADDING + CONTENT_Y_OFFSET, 0xAAAAAA);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
