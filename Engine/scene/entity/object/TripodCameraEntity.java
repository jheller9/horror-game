package scene.entity.object;

import org.joml.Vector3f;

import geom.AxisAlignedBBox;
import gl.post.NightVisionShader;
import scene.PlayableScene;
import scene.entity.Entity;
import scene.mapscene.item.TripodCameraItem;

public class TripodCameraEntity extends Entity {

	private Vector3f normal, viewPos;
	private boolean damaged;
	
	private AxisAlignedBBox bbox;

	public TripodCameraEntity(Vector3f pos, Vector3f normal, float yRot) {
		super("sensor");
		this.pos = pos;
		this.normal = normal;
		this.rot.y = yRot;
		this.setModel("cam_tripod");
		this.setTexture("camera");
		
		viewPos = Vector3f.add(pos, Vector3f.mul(Vector3f.Y_AXIS, 11));
		viewPos = Vector3f.add(viewPos, Vector3f.mul(normal, 1f));
		
		bbox = new AxisAlignedBBox(pos.x, pos.y + 5.5f, pos.z, 3f, 5.5f, 3f);
	}

	@Override
	public void update(PlayableScene scene) {
		super.update(scene);
		
	}

	public Vector3f getViewPos() {
		return viewPos;
	}

	public boolean isDamaged() {
		return damaged;
	}

	public void breakTripod() {
		this.damaged = true;
		NightVisionShader.noiseAmplifier = TripodCameraItem.STATIC_BROKEN_AMPLIFIER;
	}
}
