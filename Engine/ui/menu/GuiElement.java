package ui.menu;

import io.Input;

public abstract class GuiElement {
	public int x = 0, y = 0, width = 0, height = 0;
	protected boolean hasFocus = true;
	protected boolean tempDisable = false;

	public void draw() {
		if (!isFocused()) {
			return;
		}

		update();
		tempDisable = false;
	}

	public boolean isFocused() {
		return hasFocus;
	}

	public boolean mouseOver() {
		return Input.getMouseX() > x && Input.getMouseX() < x + width && Input.getMouseY() > y
				&& Input.getMouseY() < y + height;
	}

	public void setFocus(boolean focus) {
		this.hasFocus = focus;
		tempDisable = true;
	}

	public abstract void setPosition(int x, int y);

	protected abstract void update();
}
