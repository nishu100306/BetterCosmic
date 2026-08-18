package dev.nishu.bettercosmic.shared.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Snapshots camera state each frame (at {@link WorldRenderEvents#END_MAIN}) so a 2D overlay can
 * project world positions to the screen using values consistent with what was actually rendered that
 * frame. Content-agnostic: it exposes {@link #worldToScreen} and the camera position; what to draw is
 * up to the caller.
 *
 * <p>FOV capture (zoom-mod safe): {@code GameRendererFovMixin} feeds {@link #captureFov} the effective
 * world-render FOV every frame (already modified by any zoom mod), so waypoints stay correct under
 * zoom. Ported from BetterPrisons (Yarn → Mojang).
 */
public final class WorldSpaceTransform {

	private static double camX, camY, camZ;
	private static double sinYaw, cosYaw, sinPitch, cosPitch;
	private static float fovDeg = 0;
	private static boolean ready = false;

	private WorldSpaceTransform() {}

	public static void register() {
		WorldRenderEvents.END_MAIN.register(WorldSpaceTransform::capture);
	}

	private static void capture(WorldRenderContext ctx) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		Camera camera = client.gameRenderer.getMainCamera();

		double yawRad = Math.toRadians(camera.yRot());
		double pitchRad = Math.toRadians(camera.xRot());
		sinYaw = Math.sin(yawRad);
		cosYaw = Math.cos(yawRad);
		sinPitch = Math.sin(pitchRad);
		cosPitch = Math.cos(pitchRad);

		Vec3 cam = camera.position();
		camX = cam.x;
		camY = cam.y;
		camZ = cam.z;

		// fovDeg is written by captureFov() from GameRendererFovMixin each frame; fall back to the
		// base option value only before the first frame.
		if (fovDeg <= 0) {
			fovDeg = client.options.fov().get();
		}
		ready = true;
	}

	/**
	 * Called by {@code GameRendererFovMixin} with the main world-render FOV (already modified by any
	 * zoom mod).
	 */
	public static void captureFov(float fov) {
		if (fov > 0) {
			fovDeg = fov;
		}
	}

	/**
	 * Projects a world position to GUI-scaled screen coordinates.
	 *
	 * @return {@code float[]{screenX, screenY, camFwd}} — {@code camFwd > 0} is in front of the camera
	 *         (may still be off-screen); {@code camFwd <= 0} is behind (coords pushed far off-screen in
	 *         the correct lateral direction so clamped arrows point the right way); {@code null} only
	 *         before the first captured frame.
	 */
	public static float[] worldToScreen(double wx, double wy, double wz, int screenW, int screenH) {
		if (!ready) {
			return null;
		}

		double dx = wx - camX;
		double dy = wy - camY;
		double dz = wz - camZ;

		double camRight = -cosYaw * dx - sinYaw * dz;
		double camUp = -sinPitch * sinYaw * dx + cosPitch * dy + sinPitch * cosYaw * dz;
		double camFwd = -sinYaw * cosPitch * dx - sinPitch * dy + cosYaw * cosPitch * dz;

		double focal = (screenH / 2.0) / Math.tan(Math.toRadians(fovDeg / 2.0));
		int cx = screenW / 2;
		int cy = screenH / 2;

		float projX, projY;
		if (camFwd > 1e-4) {
			projX = (float) (cx + (camRight / camFwd) * focal);
			projY = (float) (cy - (camUp / camFwd) * focal);
		} else {
			double lateralMag = Math.sqrt(camRight * camRight + camUp * camUp);
			if (lateralMag < 1e-6) {
				projX = cx + screenW * 4f;
				projY = cy;
			} else {
				projX = (float) (cx + (camRight / lateralMag) * screenW * 4);
				projY = (float) (cy - (camUp / lateralMag) * screenH * 4);
			}
		}

		return new float[]{projX, projY, (float) camFwd};
	}

	public static double getCamX() {
		return camX;
	}

	public static double getCamY() {
		return camY;
	}

	public static double getCamZ() {
		return camZ;
	}
}
