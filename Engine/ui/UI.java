package ui;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import audio.Source;
import core.Globals;
import core.Resources;
import core.res.Model;
import scene.Scene;
import ui.render.UIShader;

public class UI {
	private static UIShader shader;
	public static final Model quad = Resources.QUAD2D;
	public static final int DEPTH_SEQUENTIAL = 0;
	private static List<Component> components = new ArrayList<Component>();
	private static float opacity = 1f;
	
	private static Source source = new Source();

	public static void addComponent(Component component) {
		final int depth = component.getDepth();

		if (depth == UI.DEPTH_SEQUENTIAL) {
			int highestDepth = 0;

			for (int i = components.size() - 1; i >= 0; --i) {
				final int compDepth = components.get(i).getDepth();
				if (compDepth >= 0) {
					if (compDepth > highestDepth) {
						highestDepth = compDepth;
					} else {
						break;
					}
				}
			}
			component.setDepth(highestDepth);
		}

		if (depth < 0) {
			for (int i = components.size() - 1; i >= 0; --i) {
				final int compDepth = components.get(i).getDepth();
				if (compDepth > depth) {
					components.add(i + 1, component);
					return;
				}
			}
		} else {
			for (int i = 0; i < components.size(); ++i) {
				final int compDepth = components.get(i).getDepth();
				if (compDepth < 0 || compDepth > depth) {
					components.add(i, component);
					return;
				}
			}
		}

		components.add(component);
	}

	public static void cleanUp() {
		shader.cleanUp();
		source.delete();
	}
	
	public static Source getSource() {
		return source;
	}

	public static void clear() {
		components.clear();
	}

	public static void drawImage(Image image) {
		addComponent(image);
		// image.setOpacity(opacity);
		image.markAsTemporary();
	}

	public static Image drawImage(String texture, int x, int y) {
		final Image img = new Image(texture, x, y);
		img.setOpacity(opacity);
		img.markAsTemporary();
		addComponent(img);
		return img;
	}

	public static Image drawImage(String texture, int x, int y, int w, int h) {
		final Image img = new Image(texture, x, y);
		img.w = w;
		img.h = h;
		img.setOpacity(opacity);
		img.markAsTemporary();
		addComponent(img);
		return img;
	}
	
	public static Image drawImage(String texture, int x, int y, int w, int h, Vector3f color) {
		final Image img = new Image(texture, x, y);
		img.w = w;
		img.h = h;
		img.setColor(color);
		img.setOpacity(opacity);
		img.markAsTemporary();
		addComponent(img);
		return img;
	}

	public static Image drawRect(int x, int y, int width, int height, Vector3f color) {
		return drawImage("none", x, y, width, height, color);
	}

	public static Text drawString(Font font, String text, int x, int y, float fontSize, int lineWidth, boolean centered,
			int... offsets) {
		final Text txt = new Text(font, text, x, y, fontSize, lineWidth, centered, offsets);
		txt.setOpacity(opacity);
		txt.markAsTemporary();
		addComponent(txt);
		return txt;
	}

	public static Text drawString(String text, int x, int y) {
		final Text txt = new Text(text, x, y);
		txt.setOpacity(opacity);
		txt.markAsTemporary();
		addComponent(txt);
		return txt;
	}

	public static Text drawString(String text, int x, int y, boolean centered) {
		final Text txt = new Text(Font.defaultFont, text, x, y, Font.defaultSize, Globals.displayWidth / 2 - 40,
				centered);
		txt.setOpacity(opacity);
		txt.markAsTemporary();
		addComponent(txt);
		return txt;
	}

	public static Text drawString(String text, int x, int y, float fontSize, boolean centered) {
		final Text txt = new Text(Font.defaultFont, text, x, y, fontSize, Globals.displayWidth / 2 - 40, centered);
		txt.setOpacity(opacity);
		txt.markAsTemporary();
		addComponent(txt);
		return txt;
	}

	public static Text drawString(String text, int x, int y, float fontSize, int lineWidth, boolean centered) {
		final Text txt = new Text(Font.defaultFont, text, x, y, fontSize, lineWidth, centered);
		txt.setOpacity(opacity);
		txt.markAsTemporary();
		addComponent(txt);
		return txt;
	}

	public static Text drawString(Text text) {
		addComponent(text);
		text.markAsTemporary();
		return text;
	}

	public static float getOpacity() {
		return opacity;
	}

	public static void init() {
		shader = new UIShader();
	}

	public static void removeComponent(Component component) {
		components.remove(component);
	}

	public static void render(Scene scene) {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		shader.start();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);

		final List<Component> temporaryComponents = new ArrayList<Component>();

		quad.bind(0, 1);

		for (final Component component : components) {
			if (component instanceof Image) {
				final Image image = (Image) component;
				image.gfx.bind(0);
				shader.color.loadVec3(image.getColor());
				shader.opacity.loadFloat(image.getOpacity());
				shader.translation.loadVec4(image.getTransform());
				shader.offset.loadVec4(image.getUvOffset());
				shader.centered.loadBoolean(image.isCentered());
				shader.rotation.loadFloat(image.getRotation());
				GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
			} else {
				// GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);
				final Text text = (Text) component;
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, text.getFont().getTexture().id);
				for (int i = 0; i < text.getLetters().length; i++) {
					final Image image = text.getLetters()[i];
					shader.color.loadVec3(image.getColor());
					shader.opacity.loadFloat(text.getOpacity());
					shader.translation.loadVec4(image.getTransform());
					shader.offset.loadVec4(image.getUvOffset());
					shader.centered.loadBoolean(image.isCentered());
					shader.rotation.loadFloat(image.getRotation());
					GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
				} // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}

			if (component.isTemporary()) {
				temporaryComponents.add(component);
			}
		}

		quad.unbind(0, 1);

		shader.stop();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);

		components.removeAll(temporaryComponents);
	}

	public static void setOpacity(float newOpacity) {
		opacity = newOpacity;
	}

	public static void updateDepth(Component component) {
		if (components.contains(component)) {
			components.remove(component);
			addComponent(component);
		}
	}
}
