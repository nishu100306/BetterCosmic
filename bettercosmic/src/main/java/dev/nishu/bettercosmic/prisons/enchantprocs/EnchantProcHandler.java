package dev.nishu.bettercosmic.prisons.enchantprocs;

/**
 * Decides how a given enchant proc is presented. Registered per-enchant with
 * {@link EnchantProcManager#register(String, EnchantProcHandler)}; the default handler shows the proc
 * as floating world-space text. Custom handlers may present procs any other way (sound, toast, HUD).
 */
@FunctionalInterface
public interface EnchantProcHandler {
	void handle(EnchantProc proc);
}
