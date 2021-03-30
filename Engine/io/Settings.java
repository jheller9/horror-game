package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import audio.AudioHandler;
import dev.Debug;
import gl.Camera;
import gl.Window;
import gl.particle.ParticleHandler;
import gl.shadow.ShadowBox;
import gl.shadow.ShadowRender;
import map.ground.Terrain;
import scene.menu.MainMenuUI;

public class Settings {
	public static File configFile = new File(FileUtils.SETTINGS_FOLDER + "/config.ini");

	private static Map<String, String> settings = new HashMap<String, String>();

	private static void addEntry(String key, boolean value) {
		settings.put(key, Boolean.toString(value));
	}

	private static void addEntry(String key, float value) {
		settings.put(key, Float.toString(value));
	}

	private static void addEntry(String key, int value) {
		settings.put(key, Integer.toString(value));
	}

	private static void addEntry(String key, String value) {
		settings.put(key, value);
	}

	public static void apply() {
		Window.displayWidth = getInt("display_width");
		Window.displayHeight = getInt("display_height");
		Window.fullscreen = getBool("fullscreen");
		Window.hasBorder = getBool("border");
		Camera.fov = getInt("fov");
		Window.maxFramerate = getInt("target_fps");
		AudioHandler.volume = getFloat("volume");
		AudioHandler.sfxVolume = getFloat("sfx_volume");
		AudioHandler.musicVolume = getFloat("mus_volume");
		Camera.mouseSensitivity = getFloat("mouse_sensitivity");
		ParticleHandler.maxParticles = getInt("max_particles");
		ShadowRender.pcfCount = getInt("shadow_quality");
		ShadowBox.shadowDistance = getFloat("shadow_dist");
		ShadowRender.shadowMapSize = getInt("shadow_fbo_size");
		MainMenuUI.disableIntroSplash = getBool("no_splash");
		MainMenuUI.onIntroSplash = MainMenuUI.disableIntroSplash;
		Debug.allowConsole = getBool("dev_enable_console");
		Camera.swayFactor = getFloat("camera_sway_factor");

		AudioHandler.changeMasterVolume();
		// Window.setDisplayMode(Window.getWidth(), Window.getHeight(),
		// Globals.fullscreen);
	}

	public static boolean getBool(String key) {
		return Boolean.parseBoolean(settings.get(key));
	}

	public static float getFloat(String key) {
		return Float.parseFloat(settings.get(key));
	}

	public static int getInt(String key) {
		return Integer.parseInt(settings.get(key));
	}

	public static String getString(String key) {
		return settings.get(key);
	}

	public static void grabData() {
		addEntry("display_width", Window.displayWidth);
		addEntry("display_height", Window.displayHeight);
		addEntry("fullscreen", Window.fullscreen);
		addEntry("border", Window.hasBorder);
		addEntry("fov", Camera.fov);
		addEntry("target_fps", Window.maxFramerate);
		addEntry("volume", AudioHandler.volume);
		addEntry("sfx_volume", AudioHandler.sfxVolume);
		addEntry("mus_volume", AudioHandler.musicVolume);
		addEntry("mouse_sensitivity", Camera.mouseSensitivity);
		addEntry("max_particles", ParticleHandler.maxParticles);
		addEntry("shadow_quality", ShadowRender.pcfCount);
		addEntry("shadow_dist", ShadowBox.shadowDistance);
		addEntry("shadow_fbo_size", ShadowRender.shadowMapSize);
		addEntry("no_splash", MainMenuUI.disableIntroSplash);
		addEntry("dev_enable_console", Debug.allowConsole);
		addEntry("camera_sway_factor", Camera.swayFactor);
	}

	public static void init() {
		/*addEntry("version", "0.1");
		addEntry("display_width", 1920);
		addEntry("display_height", 1080);
		addEntry("fullscreen", false);
		addEntry("border", false);
		addEntry("fov", 90);
		addEntry("target_fps", 120);
		addEntry("volume", 0.5f);
		addEntry("sfx_volume", 1.0f);
		addEntry("mus_volume", 1.0f);
		addEntry("mouse_sensitivity", .5f);
		addEntry("max_particles", 99);
		addEntry("entity_dist", 5);
		addEntry("shadow_quality", 2);
		addEntry("shadow_dist", 16);
		addEntry("shadow_fbo_size", 2048);
		addEntry("no_splash", false);
		addEntry("dev_enable_console", false);*/
		grabData();

		if (configFile.exists()) {
			load();
			apply();
		} else {
			save();
		}
	}

	public static void load() {
		try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
			for (String line; (line = br.readLine()) != null;) {
				final String[] data = line.split("=");
				if (settings.containsKey(data[0])) {
					settings.put(data[0], data[1]);
				}
			}
		} catch (final FileNotFoundException e) {
			return;
		} catch (final IOException e) {
			System.err.println("ERR: malformatted config file.");
		}
	}

	public static void save() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(configFile))) {
			for (final String line : settings.keySet()) {
				bw.write(line + "=" + settings.get(line) + "\n");
			}
		} catch (final IOException e) {
			System.err.println("ERR: malformatted config file.");
		}
	}
}
