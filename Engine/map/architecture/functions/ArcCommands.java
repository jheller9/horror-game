package map.architecture.functions;

import map.architecture.functions.commands.CamView;
import map.architecture.functions.commands.PathNode;
import map.architecture.functions.commands.SpawnPoint;
import map.architecture.functions.commands.SoundScape;

/**
 * Arc Commands are commands that can be called to trigger certain events on the map globally.
 */
public enum ArcCommands {
	SPAWN_PLAYER(SpawnPoint.class, ArcFuncCallMethod.BY_RANDOM),
	NAVIGATE(PathNode.class, ArcFuncCallMethod.ALL),
	CAMVIEW_RENDER(CamView.class, ArcFuncCallMethod.BY_INDEX),
	TRIGGER_SOUNDSCAPE(SoundScape.class, ArcFuncCallMethod.BY_NEAREST);
	
	private Class<? extends ArcFunction> funcClass;
	private ArcFuncCallMethod preferredCallMethod;
	
	/**
	 * @param funcClass is the associated ArcFunction class that handles this command (must extend ArcFunction)
	 * @param preferredCallMethod is the calling method this function will use when no method is specified (UNSPECIFIED)
	 */
	ArcCommands(Class<? extends ArcFunction> funcClass, ArcFuncCallMethod preferredCallMethod) {
		this.funcClass = funcClass;
		this.preferredCallMethod = preferredCallMethod;
	}

	public Class<? extends ArcFunction> getArcFuncClass() {
		return funcClass;
	}
	
	public ArcFuncCallMethod getPrefCallMethod() {
		return preferredCallMethod;
	}
}
