package map.architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import core.Resources;
import dev.Debug;
import geom.AxisAlignedBBox;
import gl.Camera;
import gl.Render;
import gl.TexturedModel;
import gl.arc.ArcRender;
import gl.light.DynamicLight;
import gl.light.DynamicLightHandler;
import gl.line.LineRender;
import gl.particle.ParticleEmitter;
import gl.res.Texture;
import map.architecture.components.ArcClip;
import map.architecture.components.ArcFace;
import map.architecture.components.ArcHeightmap;
import map.architecture.components.ArcLightCube;
import map.architecture.components.ArcNavNode;
import map.architecture.components.ArcNavigation;
import map.architecture.components.ArcPackedAssets;
import map.architecture.components.ArcRoom;
import map.architecture.components.ArcTextureData;
import map.architecture.components.ArcTriggerClip;
import map.architecture.components.GhostPoi;
import map.architecture.functions.ArcFuncHandler;
import map.architecture.functions.ArcFunction;
import map.architecture.functions.commands.CamView;
import map.architecture.vis.Bsp;
import map.architecture.vis.BspLeaf;
import map.architecture.vis.Pvs;
import scene.Scene;
import scene.entity.Entity;
import scene.entity.util.PhysicsEntity;
import util.Colors;
import util.MathUtil;

public class Architecture {

	private Scene scene;
	private String mapName;
	private byte mapVersion;
	private byte mapCompilerVersion;
	
	public Bsp bsp;
	public Pvs pvs;
	
	private DynamicLightHandler dynamicLightHandler;
	
	private ArcNavigation navigation;
	private Map<Entity, ArcTriggerClip> activeTriggers;
	
	private List<BspLeaf> renderedLeaves = new ArrayList<BspLeaf>();
	private BspLeaf currentLeaf = null;
	
	public Vector3f[] vertices;
	public ArcFace[] faces;
	
	private List<ParticleEmitter> emitters = new ArrayList<ParticleEmitter>();
	private Texture[] mapSpecificTextures;

	private Vector3f sunVector;
	private String[] mapSpecTexRefs;
	
	private ArcPackedAssets packedAssets;
	private ArcFuncHandler funcHandler;
	private Lightmap lightmap;
	public ArcLightCube[] ambientLightCubes;
	public boolean hasSkybox = false;
	
	private LinkedList<BspLeaf> audible = new LinkedList<>();
	
	private List<ArcHeightmap> renderedHeightmaps = new ArrayList<>();
	
	public Architecture(Scene scene) {
		this.scene = scene;	
		funcHandler = new ArcFuncHandler();
		lightmap = new Lightmap();
		activeTriggers = new HashMap<>();
		dynamicLightHandler = new DynamicLightHandler();
	}
	
	public void determineVisibleLeafs(Camera camera) {

		BspLeaf cameraLeaf = bsp.walk(camera.getPosition());
		if (cameraLeaf.clusterId != -1 && cameraLeaf != currentLeaf) {
			currentLeaf = cameraLeaf;
			renderedLeaves.clear();
			renderedHeightmaps.clear();
			
			int[] vis = pvs.getData(cameraLeaf, 0);
			int[] pas = pvs.getData(cameraLeaf, 1);
			audible.clear();
			
			for(int i = 0; i < bsp.leaves.length; i++) {
				BspLeaf leaf = bsp.leaves[i];
				if (leaf.clusterId == -1) continue;
				
				if (pas[leaf.clusterId] != 0) {
					audible.add(leaf);
				}
				
				if (vis[leaf.clusterId] == 0 && !ArcRender.fullRender) {
					continue;
				}
				
				renderedLeaves.add(leaf);
				for(short heightmap : leaf.heightmaps) {
					renderedHeightmaps.add(bsp.heightmaps[heightmap]);
				}
			}	
			
			if (CamView.requestRender) {
				BspLeaf camViewLeaf = bsp.walk(CamView.renderPos);
				if (camViewLeaf.clusterId == -1) return;
				vis = pvs.getData(camViewLeaf, 0);
				
				for(int i = 0; i < bsp.leaves.length; i++) {
					BspLeaf leaf = bsp.leaves[i];
					if (leaf.clusterId == -1) continue;
					if (vis[leaf.clusterId] == 0) continue;
					renderedLeaves.add(leaf);
				}	
			}
			
			//renderedLeaves.clear();
			//renderedLeaves.addAll(renderedNew);
		}
	}
	
	public void render(Camera camera, Vector4f clipPlane, boolean hasLighting) {
		
		List<ArcHeightmap> heightmaps = new LinkedList<>();
		
		ArcRender.renderHeightmaps(camera, this, renderedHeightmaps, clipPlane, hasLighting, dynamicLightHandler.getLights());

		if (Debug.viewNavNode) {
			ArcNavNode node = navigation.getNodeAt(camera.getPosition(), bsp);
			if (node != null) {
				for(int id : node.getFaceIds()) {
					ArcFace face = bsp.faces[id];
					ArcUtil.drawFaceHighlight(bsp, face, Colors.alertColor());
				}
			}
		}
		
		if (Debug.viewNavPois) {
			for(int i = 1; i < bsp.rooms.length; i++) {
				ArcRoom room = bsp.rooms[i];
				for(GhostPoi poi : room.getGhostPois()) {
					Vector3f pos = Vector3f.add(poi.getPosition(), Vector3f.Y_AXIS);
					LineRender.drawPoint(poi.getPosition());
					LineRender.drawLine(pos, Vector3f.add(pos, MathUtil.eulerToVectorDeg(poi.getRotation().x, poi.getRotation().y)));
				}
			}
		}
		
		ArcRender.renderShadows(camera, renderedLeaves, dynamicLightHandler.getLights());
		
		bsp.objects.render(camera, this);
		
		ArcRender.startRender(camera, clipPlane, hasLighting, dynamicLightHandler.getLights());
		
		for(BspLeaf leaf : renderedLeaves) {
			
			if (leaf.isUnderwater && clipPlane.w == Float.POSITIVE_INFINITY) {
				Render.renderWaterFbos(scene, camera, leaf.max.y);
				ArcRender.renderWater(camera, leaf.max, leaf.min);
			}
			
			if (Debug.showClips) {
				for(short id : leaf.clips ) {
					ArcClip clip = bsp.clips[id];
					LineRender.drawBox(clip.bbox.getCenter(), clip.bbox.getBounds(), clip.id.getColor());
				}
			}
			
			for(TexturedModel visObj : leaf.getMeshes()) {
				
				if (!camera.getFrustum().containsBoundingBox(leaf.max, leaf.min)) {continue;}

				ArcRender.render(camera, this, visObj);
			}
		}
		
		ArcRender.finishRender();
		
		for (ParticleEmitter pe : emitters) {
			pe.generateParticles(camera);
		}
		
		if (Debug.showAmbient) {
			int len = currentLeaf.firstAmbientSample + currentLeaf.numAmbientSamples;
			for(int i = currentLeaf.firstAmbientSample; i < len; i++) {
				ArcLightCube lightCube = ambientLightCubes[i];
				Vector3f pos = lightCube.getPosition(currentLeaf);
				LineRender.drawLine(pos, Vector3f.add(pos, new Vector3f(-1,0,0)), lightCube.getColor(0));
				LineRender.drawLine(pos, Vector3f.add(pos, new Vector3f(1,0,0)), lightCube.getColor(1));
				LineRender.drawLine(pos, Vector3f.add(pos, new Vector3f(0,-1,0)), lightCube.getColor(2));
				LineRender.drawLine(pos, Vector3f.add(pos, new Vector3f(0,1,0)), lightCube.getColor(3));
				LineRender.drawLine(pos, Vector3f.add(pos, new Vector3f(0,0,-1)), lightCube.getColor(4));
				LineRender.drawLine(pos, Vector3f.add(pos, new Vector3f(0,0,1)), lightCube.getColor(5));
			}
		}
		
	}
	
	public void pollTriggers() {
		Iterator<Entity> iter = activeTriggers.keySet().iterator();
		while(iter.hasNext()) {
			Entity entity = iter.next();
			ArcTriggerClip trigger = activeTriggers.get(entity);
			
			if (entity instanceof PhysicsEntity) {
				PhysicsEntity physEnt = (PhysicsEntity)entity;
				if (trigger.bbox.collide(physEnt.getBBox()) == null) {
					trigger.interact(entity, false);
					iter.remove();
				}
			} else {
				if (!trigger.bbox.collide(entity.pos)) {
					trigger.interact(entity, false);
					iter.remove();
				}
			}
		}
	}
	
	public Vector3f[] getLightsAt(Vector3f position) {
		if (lightmap.isActive()) {
			BspLeaf leaf = bsp.walk(position);
			return getLightsAt(position, leaf);
		}
		
		return ArcLightCube.FULLBRIGHT;
		
	}
	
	private Vector3f[] getLightsAt(Vector3f position, BspLeaf leaf) {
		ArcLightCube nearestCube = null;//ambientLightCubes[leaf.firstAmbientSample];
		float nearest = Float.POSITIVE_INFINITY;//Vector3f.distanceSquared(ambientLightCubes[leaf.firstAmbientSample].getPosition(leaf), position);
		
		for(int i = 0; i < leaf.numAmbientSamples; i++) {
			int sampleId = leaf.firstAmbientSample + i;
			ArcLightCube lightCube = ambientLightCubes[sampleId];
			float dist = Vector3f.distanceSquared(lightCube.getPosition(leaf), position);
			
			if (dist < nearest) {
				nearest = dist;
				nearestCube = lightCube;
			}
		}
		
		if (nearestCube == null) {
			return ArcLightCube.NO_LIGHT;
		}
		
		return nearestCube.getLighting();
	}
	
	public boolean isLeafAudible(BspLeaf leaf) {
		return audible.contains(leaf);
	}

	public void passAssetsToOpenGL() {
		packedAssets.passToOpenGL();
		packedAssets = null;
		callCommand("spawn_player");
	}
	
	public void cleanUp() {
		bsp.cleanUp();
		for(int i = 0; i < mapSpecTexRefs.length; i++) {
			Resources.removeTexture(mapSpecTexRefs[i++]);
		}
		lightmap.delete();
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

	public void setProperties(String mapName, byte mapVersion, byte mapCompilerVersion) {
		this.mapName = mapName;
		this.mapVersion = mapVersion;
		this.mapCompilerVersion = mapCompilerVersion;
	}

	public String getMapName() {
		return mapName;
	}

	public byte getMapVersion() {
		return mapVersion;
	}

	public byte getMapCompilerVersion() {
		return mapCompilerVersion;
	}

	public void setMapSpecificTextures(Texture[] mapSpecificTextures, String[] mapSpecTexRefs) {
		this.mapSpecificTextures = mapSpecificTextures;
		this.mapSpecTexRefs = mapSpecTexRefs;
	}

	public List<BspLeaf> getRenderedLeaves() {
		return renderedLeaves;
	}
	
	public Vector3f getSunVector() {
		return sunVector;
	}

	public void setSunVector(Vector3f sunVector) {
		this.sunVector = sunVector;
	}

	public void setPackedAssets(ArcPackedAssets packedAssets) {
		this.packedAssets = packedAssets;
	}
	
	public void addFunction(ArcFunction func) {
		funcHandler.add(func);
	}
	
	public void callCommand(Entity caller, String cmd) {
		funcHandler.callCommand(caller.pos, cmd);
	}
	
	public void callCommand(String cmd) {
		funcHandler.callCommand(Vector3f.ZERO, cmd);
	}

	public void createLightmap(byte[] rgb, ArcFace[] faces) {
		lightmap.create(rgb, faces);
	}

	public void setNavigation(ArcNavigation navigation) {
		this.navigation = navigation;
	}
	
	public ArcNavigation getNavigation() {
		return navigation;
	}

	public ArcTriggerClip getActiveTrigger(Entity entity) {
		return activeTriggers.get(entity);
	}

	public void setTriggerActive(Entity entity, ArcTriggerClip clip) {
		activeTriggers.put(entity, clip);
	}

	public String[] getMapTextureRefs() {
		return this.mapSpecTexRefs;
	}

	public ArcPackedAssets getPackedAssets() {
		return packedAssets;
	}

	public ArcTextureData[] getTexData() {
		return packedAssets.getTextureData();
	}
	
	public Texture[] getReferencedTextures() {
		return this.mapSpecificTextures;
	}
	
	public String[] getReferencedTexNames() {
		return mapSpecTexRefs;
	}

	public Lightmap getLightmap() {
		return lightmap;
	}
	
	public void changeMipmapBias() {
		for (Texture texture : this.getReferencedTextures()) {
			if (texture == null) continue;
			texture.bind(0);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, Render.defaultBias);
		}
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	public void removeLight(DynamicLight light) {
		this.dynamicLightHandler.removeLight(light);
	}
	
	public DynamicLight addLight(Vector3f pos, Vector3f rot, float strength) {
		return this.dynamicLightHandler.addLight(pos, rot, strength);
	}

	/** Raycasts against the visible geometry
	 * @param pos the ray's origin
	 * @param dir the ray's direction
	 * @return the distance the ray traveled
	 */
	public float raycast(Vector3f org, Vector3f dir) {
		float shortestDist = Float.POSITIVE_INFINITY;

		for(BspLeaf leaf : renderedLeaves) {
			
			Vector3f bounds = Vector3f.sub(leaf.max, leaf.min).div(2f);
			Vector3f center = Vector3f.add(leaf.min, bounds);
			
			float distLeaf = new AxisAlignedBBox(center, bounds).collide(org, dir);
			
			if (Float.isNaN(distLeaf))
				continue;
			
			if (distLeaf < shortestDist) {
				
				ArcFace[] faces = bsp.getFaces(leaf);
				for(ArcFace face : faces) {
					
					final float distPlane = (bsp.planes[face.planeId].collide(org, dir));
					
					if (distPlane < shortestDist) {

						Vector3f posOnPlane = Vector3f.add(org, Vector3f.mul(dir, distPlane));
						if (ArcUtil.faceContainsPoint(bsp, face, posOnPlane)) {
							shortestDist = distPlane;
						}
					}
				}
			}
		}
		return shortestDist;
	}

	/*public List<BspLeaf> getLoadedLeafs() {
		return loaded;
	}

	public List<BspLeaf> getUnloadedLeafs() {
		return unloaded;
	}*/
}
