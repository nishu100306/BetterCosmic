# Sky dev-run companion mods

Drop any companion mod `.jar` files you want loaded **only in the Sky test client** into this
folder. Fabric's dev launcher loads them alongside the BetterCosmic dev build when you run:

```
./gradlew :bettercosmic:runSkyClient
```

This instance uses the `bettercosmic/run-sky/` directory for its saves, config, and logs, kept
separate from the Prisons instance (`bettercosmic/run-prisons/`). Only this `README.md` is tracked
by git; the jars you add and everything else the run generates are ignored.
