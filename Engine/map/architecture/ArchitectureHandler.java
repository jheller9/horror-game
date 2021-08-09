package map.architecture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.joml.Vector3f;
import org.joml.Vector4f;

import core.Application;
import dev.Console;
import dev.Debug;
import gl.Camera;
import gl.arc.ArcRender;
import gl.line.LineRender;
import map.architecture.components.ArcFace;
import map.architecture.components.ArcNavNode;
import map.architecture.components.ArcNavigation;
import map.architecture.read.ArcLoader;
import map.architecture.vis.Bsp;
import scene.PlayableScene;
import scene.Scene;
import util.Colors;

public class ArchitectureHandler {
	
	private Architecture architecture;
	
	private static boolean changeMap = false;
	public static String[] validMaps = new String[0];
	private static String[] mapSequence = readMapSequenceFile();
	private static int currentMap = 0;

	public static void pollValidMaps() {
		File f = new File("maps/");
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return name.endsWith(".arc");
			}
		};

		// This is how to apply the filter
		String[] pathnames = f.list(filter);
		validMaps = new String[pathnames.length];
		int i = 0;
		for (String pathname : pathnames) {
			validMaps[i] = pathname.replace("maps/", "").replace(".arc", "").toLowerCase();
			i++;
		}
	}

	private static String[] readMapSequenceFile() {
		List<String> lines = new ArrayList<String>();
		Scanner sc;
		
		try {
			sc = new Scanner(new File("src/res/maps/map_sequence.txt"));
			while (sc.hasNextLine()) {
				lines.add(sc.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return lines.toArray(new String[0]);
	}

	public ArchitectureHandler() {
		ArcRender.init();
	}

	public void load(Scene scene, Vector3f vec, String path) {
		architecture = ArcLoader.load(scene, path, vec, true);
	}
	
	public void cleanUp() {
		architecture.cleanUp();
		ArcRender.cleanUp();
	}

	public void render(Camera camera, Vector4f clipPlane) {
		boolean hasLighting = architecture.getLightmap().isActive();
		architecture.pollTriggers();
		architecture.render(camera, clipPlane, hasLighting);
		
		Bsp bsp = ((PlayableScene)Application.scene).getArchitecture().bsp;

		if (Debug.viewNavMesh) {
			ArcNavigation nav = architecture.getNavigation();
			for(ArcNavNode node : nav.getNavFaces()) {
				
				for(int id : node.getFaceIds()) {
					ArcFace face = bsp.faces[id];
					ArcUtil.drawFaceHighlight(bsp, face, Colors.YELLOW);
				}
				
				int i = 0;
				for(short s : node.getNeighbors()) {
					int edge = node.getEdges()[i];
					Vector3f p1 = bsp.vertices[bsp.edges[edge].start];
					Vector3f p2 = bsp.vertices[bsp.edges[edge].end];
					Vector3f center = Vector3f.add(p1, p2).div(2f).add(Vector3f.Y_AXIS);
					Vector3f cross = Vector3f.sub(p2, p1).cross(Vector3f.Y_AXIS).normalize();
					LineRender.drawLine(Vector3f.add(center, cross),
							Vector3f.add(center, Vector3f.negate(cross)), Colors.WHITE);
					i++;
				}
			}
		}
	}

	public Architecture getArchitecture() {
		return architecture;
	}

	public static boolean isValidMap(String map) {
		String maplc = map.toLowerCase();
		for(String validMap : validMaps) {
			if (validMap.equals(maplc))
				return true;
		}
		
		return false;
	}

	public void update(Camera camera) {
		if (changeMap) {
			Console.send("map " + mapSequence[currentMap]);
			changeMap = false;
			return;
		}
		
		architecture.determineVisibleLeafs(camera);
	}
	
	public boolean isSkyboxEnabled() {
		return architecture.hasSkybox;
	}
	
	public static void nextMap() {
		currentMap++;
		changeMap = true;
		
	}
}
