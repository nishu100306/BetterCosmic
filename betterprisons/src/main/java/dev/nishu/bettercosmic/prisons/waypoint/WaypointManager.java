package dev.nishu.bettercosmic.prisons.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-world store of {@link CustomWaypoint}s (user-created plus auto-added event waypoints), persisted
 * to {@code config/bettercosmic/betterprisons/waypoints.json}. Renderers read {@link #getEnabled()}
 * (user waypoints for the current world); the Events HUD mirrors its events in here via the
 * {@code *EventWaypoint} helpers so they also show on the Waypoints screen. Ported from BetterPrisons'
 * {@code waypoint/WaypointManager} (Yarn → Mojang: {@code world.getRegistryKey().getValue()} →
 * {@code level.dimension().identifier()}).
 */
public class WaypointManager {

	private static final File WAYPOINTS_FILE =
			new File("config/bettercosmic/betterprisons/waypoints.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** World key → waypoints for that world. */
	private final Map<String, List<CustomWaypoint>> worldWaypoints = new LinkedHashMap<>();

	/** Key of the world the player is currently in. */
	private String currentWorld = "unknown";

	// ---- World management ----

	/** The dimension key string of the world the client is currently in. */
	public static String detectWorldKey() {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null) {
			return client.level.dimension().identifier().toString();
		}
		return "unknown";
	}

	public void setCurrentWorld(String world) {
		this.currentWorld = world;
		worldWaypoints.computeIfAbsent(world, k -> new ArrayList<>());
	}

	public String getCurrentWorld() {
		return currentWorld;
	}

	/**
	 * Worlds that actually have at least one waypoint, sorted. Empty entries — which
	 * {@code computeIfAbsent} leaves behind just from visiting a world or rendering — are excluded, so
	 * the Waypoints screen never lists a world you have no waypoints in.
	 */
	public List<String> getWorlds() {
		List<String> worlds = new ArrayList<>();
		for (Map.Entry<String, List<CustomWaypoint>> entry : worldWaypoints.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				worlds.add(entry.getKey());
			}
		}
		Collections.sort(worlds);
		return worlds;
	}

	// ---- Persistence ----

	public void load() {
		worldWaypoints.clear();
		if (!WAYPOINTS_FILE.exists()) {
			return;
		}
		try (FileReader reader = new FileReader(WAYPOINTS_FILE)) {
			Type type = new TypeToken<Map<String, List<CustomWaypoint>>>() {}.getType();
			Map<String, List<CustomWaypoint>> loaded = GSON.fromJson(reader, type);
			if (loaded != null) {
				worldWaypoints.putAll(loaded);
			}
		} catch (Exception e) {
			// File corrupt or unreadable — start fresh
		}
	}

	/**
	 * Persists atomically (temp file + move) so a crash mid-write can't truncate the store, and writes
	 * only worlds that still have waypoints — {@code computeIfAbsent} leaves empty lists behind just from
	 * visiting or rendering a world, and those shouldn't clutter the file.
	 */
	public void save() {
		WAYPOINTS_FILE.getParentFile().mkdirs();
		Map<String, List<CustomWaypoint>> toWrite = new LinkedHashMap<>();
		for (Map.Entry<String, List<CustomWaypoint>> entry : worldWaypoints.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				toWrite.put(entry.getKey(), entry.getValue());
			}
		}
		try {
			Path target = WAYPOINTS_FILE.toPath();
			Path tmp = Files.createTempFile(target.getParent(), "waypoints", ".json.tmp");
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(toWrite, writer);
			}
			try {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException ex) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			// Best-effort persistence — a failed save must not crash the client
		}
	}

	// ---- CRUD ----

	private List<CustomWaypoint> listFor(String world) {
		return worldWaypoints.computeIfAbsent(world, k -> new ArrayList<>());
	}

	public void removeWorld(String world) {
		worldWaypoints.remove(world);
		save();
	}

	public void add(CustomWaypoint wp) {
		listFor(currentWorld).add(wp);
		save();
	}

	public void remove(int index, String world) {
		List<CustomWaypoint> wps = listFor(world);
		if (index >= 0 && index < wps.size()) {
			wps.remove(index);
			save();
		}
	}

	public void remove(int index) {
		remove(index, currentWorld);
	}

	public void update(int index, CustomWaypoint wp, String world) {
		List<CustomWaypoint> wps = listFor(world);
		if (index >= 0 && index < wps.size()) {
			wps.set(index, wp);
			save();
		}
	}

	public void update(int index, CustomWaypoint wp) {
		update(index, wp, currentWorld);
	}

	public List<CustomWaypoint> getAll(String world) {
		return listFor(world);
	}

	public List<CustomWaypoint> getAll() {
		return listFor(currentWorld);
	}

	/** Enabled, user-created waypoints for the current world (event waypoints excluded). */
	public List<CustomWaypoint> getEnabled() {
		List<CustomWaypoint> result = new ArrayList<>();
		for (CustomWaypoint wp : listFor(currentWorld)) {
			if (wp.enabled && !wp.isEvent()) {
				result.add(wp);
			}
		}
		return result;
	}

	// ---- Event waypoint helpers (auto-added from the Events HUD) ----

	private static final String OVERWORLD = dev.nishu.bettercosmic.prisons.PrisonWorlds.OVERWORLD;

	/** Removes all event waypoints from every world (called on world join to clear stale entries). */
	public void clearAllEventWaypoints() {
		for (List<CustomWaypoint> list : worldWaypoints.values()) {
			list.removeIf(CustomWaypoint::isEvent);
		}
		save();
	}

	/** Adds an event waypoint to the overworld list unless one already exists at those coordinates. */
	public void addEventWaypoint(int x, int y, int z, int color, String name, String eventKey) {
		List<CustomWaypoint> wps = listFor(OVERWORLD);
		for (CustomWaypoint wp : wps) {
			if (wp.x == x && wp.y == y && wp.z == z) {
				return; // already present
			}
		}
		CustomWaypoint wp = new CustomWaypoint(name, x, y, z, color);
		wp.eventKey = eventKey;
		wps.add(wp);
		save();
	}

	/** Removes the auto-added event waypoint at the given overworld coordinates. */
	public void removeEventWaypoint(int x, int y, int z) {
		List<CustomWaypoint> wps = listFor(OVERWORLD);
		wps.removeIf(wp -> wp.isEvent() && wp.x == x && wp.y == y && wp.z == z);
		save();
	}

	/** Whether an event waypoint exists at the given overworld coordinates. */
	public boolean hasEventWaypoint(int x, int y, int z) {
		for (CustomWaypoint wp : listFor(OVERWORLD)) {
			if (wp.isEvent() && wp.x == x && wp.y == y && wp.z == z) {
				return true;
			}
		}
		return false;
	}
}
