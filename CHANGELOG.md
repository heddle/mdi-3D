# Changelog

All notable changes to MDI 3D (the JOGL-based 3D add-on for the MDI
scientific visualization framework) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

No changelog was kept during development prior to this file, so the entry
below has been reconstructed from the commit history since the `v1.0.0c`
tag — effectively the whole feature-development arc of this project to date.

## [Unreleased]

## [1.0.1] - 2026-08-31

### Added

- Phong lighting for the built-in 3D primitives (Sphere, Cube, Cylinder,
  Quad3D).
- A rotating MDI-logo demo view, with independently-tilted, precessing
  spin-axis panes.
- A 3D scatter plot demo view.
- An Aizawa strange-attractor demo view.
- A 2D drawing view added alongside the 3D demos, to demonstrate 2D and 3D
  views coexisting in the same MDI application.
- `AbstractViewInfo` added for the remaining five 3D demo views, for
  consistent "about this view" info dialogs.
- A Save/Copy Image menu for 3D views, built on the new
  `BaseView.getImageComponent()` hook — added to MDI itself (1.2.2)
  specifically to unblock this feature.
- Graceful fallback when OpenGL is unavailable, instead of failing outright.
- Opted into `BaseMDIApplication.exitOnClose()`, so the native window close
  button behaves the same as Quit.

### Changed

- `createInitialItems()` is now deferred to the first GL `init()` call
  rather than running in the constructor.
- Keyboard handling consolidated into one source of truth.
- Removed the unused `basic/` package.
- README and LICENSE brought up to date, including the switch to the MIT
  license.
- Now depends on the released MDI 1.2.2 (tracking MDI's own releases
  throughout development: 1.0.1 → 1.1.0 → 1.2.1 → 1.2.2).

### Fixed

- `RenderStyle.SPHERES` now actually renders lit spheres.
- A `TextRenderer` OpenGL resource leak.
- Item add/remove atomicity hardened.
- A point-clearing race condition and control-panel clipping issue in the
  scatter-plot demo.
- `KineticsModel.reset()` now properly supports a new count/length.
- A missing explanation-text item in `DrawingDemoView`; the explanation text
  is now centered and configured like an ordinary item.
- Sizing on small/Linux laptop displays.
- A double-initialization bug.

### Documentation & Testing

- Complete Javadoc coverage and new unit tests added as part of 1.1.0
  release prep.

[Unreleased]: https://github.com/heddle/mdi-3D/compare/v1.0.1...develop
[1.0.1]: https://github.com/heddle/mdi-3D/compare/v1.0.0c...v1.0.1
