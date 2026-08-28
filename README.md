# MDI-3D

A JOGL-based 3D add-on for the [MDI](https://github.com/heddle/mdi) scientific-visualization
framework. MDI-3D is an **optional extension**: it depends on the base `mdi` package (for its
Swing/MDI view framework, simulation engine, and utility classes) rather than the other way
around, so applications that don't need 3D never pull it in.

## What it provides

- **`Panel3D`** — a Swing `JPanel` that hosts a JOGL `GLJPanel` and manages a simple interactive
  3D scene: a list of drawable `Item3D` objects, an arcball camera (quaternion orientation, pan,
  zoom), and a two-pass opaque/transparent render loop.
- **`Item3D`** and its subclasses (`Sphere`, `Cube`, `Cylinder`, `Quad3D`, `Line3D`, `PolyLine3D`,
  `Triangle3D`, `Axes3D`, `Trajectory3D`, `ScatterPlot3D`, `LabelSet3D`, ...) — the drawable
  primitives placed on a `Panel3D`.
- **`Support3D`** — a stateless library of lower-level OpenGL drawing routines (strokes, solids,
  tubes, spherical geometry) that item classes are built on, including optional Phong-style
  lighting for spheres, boxes, cylinders, and flat panels.
- **`PlainView3D`** / **`SimulationView3D`** — MDI view base classes. `PlainView3D` is for static
  or purely interactive 3D scenes; `SimulationView3D` is the 3D analogue of `edu.cnu.mdi.sim.ui.SimulationView`
  and wires a `Panel3D` to an `edu.cnu.mdi.sim.SimulationEngine` for time-evolving scenes, reusing
  the base package's full simulation lifecycle (run/pause/resume/stop/cancel) and standard control
  panels.
- **Six demo views**, registered together in `DemoApp3D`:

  | Demo | Package | Drives its updates via |
  |---|---|---|
  | Kinetics (particles in a box) | `view3D.kineticsDemo` | `SimulationEngine` |
  | Aizawa attractor | `view3D.aizawaDemo` | `SimulationEngine` |
  | Rotating MDI logo | `view3D.logoDemo` | `SimulationEngine` (trivial: only advances an angle) |
  | 3D scatter plot | `view3D.scatterDemo` | background data feed + interactive |
  | Geometry slice | `view3D.geoslice` | a slider (`PlainView3D`, no time evolution) |
  | Interactive globe | `view3D.globe` | mouse only (`PlainView3D`, no time evolution) |

## Requirements

- Java 17+
- A working OpenGL 2.x-capable environment. `Panel3D` degrades gracefully (see below) if none is
  available, but the demos are obviously more interesting with real rendering.

## Quick start

```bash
mvn -q compile
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/mdi3d_cp.txt
java -cp "target/classes:$(cat /tmp/mdi3d_cp.txt)" edu.cnu.mdi.mdi3D.app.DemoApp3D
```

`Panel3DDemo` (in `edu.cnu.mdi.mdi3D.panel`) is a smaller, non-MDI standalone harness for
exercising `Panel3D` directly — useful when you want to see a scene without the full MDI
application shell. It also shows the L/R/U/D/J/K/1-4 keyboard shortcuts on screen via
`KeyboardLabel`.

### Keyboard controls

Installed by `KeyBindings3D` on every `Panel3D` (via Swing's `InputMap`/`ActionMap`, so they work
regardless of which child component has focus):

| Keys | Action |
|---|---|
| `L` / `R` | Pan left / right |
| `U` / `D` | Pan up / down |
| `J` / `K` | Dolly in / out |
| `X` / `Y` / `Z` | Rotate about the corresponding axis |
| `1` / `2` / `3` / `4` | Jump to a preset axis-aligned view |
| `Shift` + any of the above | Larger step (pan/dolly), or reversed direction (rotation) |
| `F5` | Force an immediate redraw |

The mouse supports arcball rotation (drag) and zoom (wheel). All of this logic lives in exactly
one place — `KeyAdapter3D.handleVK` — which both the real keyboard bindings and the on-screen
`KeyboardLabel` legend buttons call, so a keystroke and its corresponding legend button can never
disagree about what they do.

## Threading model

`Panel3D` uses `GLJPanel` (not the heavyweight `GLCanvas`), because it is FBO-backed and paints
like an ordinary Swing component, which is what makes it safe to mix with the rest of a Swing UI.
The rule the whole package follows:

- **GL state and `Item3D` scene mutation happen only on the EDT** — either directly (constructing
  items in `createInitialItems()`, mouse/keyboard handlers, menu actions) or, for a
  `SimulationView3D`, inside an `onSimulationRefresh(...)` hook, which `SimulationEngine`
  guarantees is dispatched on the EDT.
- **The simulation worker thread never touches `Item3D` or `Panel3D` directly.** A `Simulation`'s
  `step()` method updates only its own model's state (see `KineticsModel`, `AizawaModel`,
  `LogoSimulation`); the corresponding view reads a published snapshot and applies it to the scene
  from `onSimulationRefresh`.

If you add a new simulation-driven demo, follow that same split: no `Item3D`/`Panel3D` calls from
inside `Simulation.step()`.

## Graceful degradation without OpenGL

If OpenGL cannot be initialized (a headless CI runner, a remote session without GPU passthrough, a
missing or misconfigured driver), `Panel3D` no longer lets the raw JOGL exception crash view
construction. Instead `getGLJPanel()`/`isGLAvailable()` report the failure, and the panel shows an
explanatory message in place of the 3D surface. `getGLInitError()` returns the underlying cause for
diagnostics.

## Adding lighting to a shape

`Sphere`, `Cube`, `Cylinder`, and `Quad3D` all have a `setLighted(true)` opt-in (default `false`,
preserving their historical flat-color appearance). When enabled, the shape is drawn with a fixed
directional light and Phong material via `Support3D`'s `*Shaded*` methods, so its shading visibly
changes as it — or the scene around it — rotates. See the rotating-logo demo
(`view3D.logoDemo.LogoDemoView`) for a minimal, self-contained example.

## Relationship to the base `mdi` package

MDI-3D deliberately reuses base-package utilities rather than re-implementing them: `PropertyUtils`
for view construction properties (including 3D-specific `ANGLE_X/Y/Z`, `DIST_X/Y/Z` keys),
`SimulationEngine`/`SimulationEngineConfig` for the simulation lifecycle,
`sim.ui.IconSimulationControlPanel` for the standard run/pause/stop control panel,
`ViewConfiguration`/`ViewPropertiesBuilder`/`VirtualView` for view registration and layout,
`TakePicture` for the "Save Image..." menu item every `PlainView3D` gets, and
`edu.cnu.mdi.mapping`'s GeoJSON loaders in the globe demo. If you're extending MDI-3D and find
yourself reaching for `JFileChooser`, hex-color parsing, or dialog-centering code directly, check
the base package's utility classes first.

## License

MIT — see [LICENSE](LICENSE).
