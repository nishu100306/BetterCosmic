package dev.nishu.bettercosmic.shared.dev;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.slf4j.LoggerFactory;

/**
 * A <b>development-only</b> Log4j2 filter that drops log messages containing any of a set of substrings.
 *
 * <p>Purpose: some mods installed alongside BetterCosmic during testing are extremely chatty. The
 * official <em>CosmicPrisonsMod</em>, for example, prints hundreds of lines per session to
 * {@code System.out} (captured by Minecraft under the {@code (Minecraft) [STDOUT]:} logger), which
 * buries our own output. Because that noise is stdout — not a named logger — it can't be quieted with a
 * per-logger level; a message-content filter is the right tool.
 *
 * <p>This installs a context-wide filter that {@link Result#DENY}s any event whose message contains a
 * configured marker, and {@link Result#NEUTRAL} (i.e. don't interfere) otherwise. It is a hard no-op
 * outside a Fabric development environment, so it <b>never affects a shipped build / real players'
 * logs</b> — it only cleans up our own {@code runClient} console. Failures fail soft (logged once).
 */
public final class DevLogFilter extends AbstractFilter {

	private final String[] needles;

	private DevLogFilter(String[] needles) {
		super(Result.DENY, Result.NEUTRAL);
		this.needles = needles;
	}

	/**
	 * Installs the filter to drop any log message containing one of {@code needles}. No-op outside a
	 * development environment. Call once, early, from a client initializer.
	 */
	public static void suppressContaining(String... needles) {
		if (needles == null || needles.length == 0) {
			return;
		}
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return; // never touch logging in a shipped build
		}
		try {
			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			Configuration cfg = ctx.getConfiguration();
			cfg.addFilter(new DevLogFilter(needles.clone()));
			ctx.updateLoggers();
			LoggerFactory.getLogger("bettercosmicshared")
					.info("Dev log filter installed (suppressing {} marker(s)): {}", needles.length,
							String.join(", ", needles));
		} catch (Throwable t) {
			// Log4j internals differ across versions; if anything about this API changed, just skip it.
			LoggerFactory.getLogger("bettercosmicshared")
					.warn("Dev log filter not installed ({}); noisy mod logs will not be suppressed.", t.toString());
		}
	}

	private Result eval(String message) {
		if (message != null) {
			for (String needle : needles) {
				if (message.contains(needle)) {
					return Result.DENY;
				}
			}
		}
		return Result.NEUTRAL;
	}

	// Log4j evaluates a context-wide filter through several overloads depending on the call path
	// (stdout capture, parameterized logs, pre-built events). Route the ones that carry the message
	// text through the same check; anything not covered simply falls through as NEUTRAL.

	@Override
	public Result filter(LogEvent event) {
		return event == null || event.getMessage() == null
				? Result.NEUTRAL : eval(event.getMessage().getFormattedMessage());
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
		return msg == null ? Result.NEUTRAL : eval(msg.getFormattedMessage());
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
		return msg == null ? Result.NEUTRAL : eval(String.valueOf(msg));
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
		return eval(msg);
	}
}
