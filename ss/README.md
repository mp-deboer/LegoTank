# StateSmith State Machine Designs

This folder contains the visual state machine designs created with **draw.io** for the LEGO tank project.

StateSmith automatically generates clean, type-safe Java code from these diagrams.

## Files
- `Sm_*.drawio.svg` – All state machines
- `build.csx` – dotnet-script that generates the corresponding Java classes in `../src/tankpack/`

## How to Edit & Regenerate

1. Make sure dotnet and dotnet-script are installed (see [StateSmith install requirements](https://github.com/StateSmith/StateSmith/wiki/StateSmith-install-requirements) for instructions).
2. Open any `.drawio.svg` file in either an installed version of [draw.io](https://www.drawio.com/) or in a browser via [diagrams.net](https://app.diagrams.net/).
3. Make changes (see tab '$notes shapes & tips' for correct shapes and labels; see StateSmith [fundamentals](https://statesmith.github.io/fundamentals-1/) or [examples](https://github.com/StateSmith/StateSmith-examples) for further information).
4. Save the file.
5. Regenerate all Java code (run from the `ss` directory):
<pre><code>dotnet-script.exe .\build.csx</code></pre>

The script only regenerates files when the diagram is newer than the generated Java file.

## Notes
- The generated files (`Sm_*_Generated.java`) are **not** edited manually.
- Thin wrapper classes (`Sm_*.java`) in `../src/tankpack/` extend the generated classes and add StateMachine-specific logic.
- `build.csx` automatically inserts the compatibility methods (`getAllowedEvents()`, `dispatchEvent(String)`, etc.) required by the legacy `StateMachine` base class.

This replaces the old LTS/LTSA files (archived earlier).
