# Prisons dev-run companion mods

Drop any companion mod `.jar` files you want loaded **only in the Prisons test client** into
this folder. Fabric's dev launcher loads them alongside the BetterCosmic dev build when you run:

```
./gradlew :bettercosmic:runPrisonsClient
```

This instance uses the `bettercosmic/run-prisons/` directory for its saves, config, and logs, kept
separate from the Sky instance (`bettercosmic/run-sky/`). Only this `README.md` is tracked by git;
the jars you add and everything else the run generates are ignored.
