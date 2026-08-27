package dev.nishu.bettercosmic.prisons.waypoint;

/**
 * A single waypoint: a named world position with a beam color/opacity and per-marker icon scales.
 * User-created waypoints have a {@code null} {@link #eventKey}; auto-added event waypoints (meteors,
 * merchants, ...) carry a non-null key so they can be told apart and cleared. Ported verbatim from
 * BetterPrisons' {@code waypoint/CustomWaypoint}.
 */
public class CustomWaypoint {
	public String name;
	public int x, y, z;
	public int color;           // 0xRRGGBB
	public int opacity = 255;   // 0-255 beacon beam opacity
	public float onScreenScale = 1.0f;  // icon scale when the waypoint projects on-screen
	public float offScreenScale = 1.0f; // icon scale for the edge indicator when off-screen
	public boolean enabled;
	/** Non-null for auto-added event entries (e.g. "METEOR_NATURAL"); null for user-created waypoints. */
	public String eventKey;

	public CustomWaypoint() {} // for Gson

	public CustomWaypoint(String name, int x, int y, int z, int color) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.color = color;
		this.opacity = 255;
		this.onScreenScale = 1.0f;
		this.offScreenScale = 1.0f;
		this.enabled = true;
	}

	public boolean isEvent() {
		return eventKey != null;
	}
}
