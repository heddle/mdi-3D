package edu.cnu.mdi.mdi3D.view3D.geoslice;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View information for the Geometry Slice 3D view.
 *
 * <p>
 * This information panel explains the purpose of the 3D companion view. The
 * view exists primarily to show the full synthetic 3D model that is sliced by
 * the corresponding 2D Geometry Slice Demo View.
 * </p>
 *
 * <p>
 * The important design point is that the 2D and 3D views do not maintain
 * separate copies of the geometry. They use the same model classes, so the
 * 3D rendering and the 2D slice are guaranteed to be different views of the
 * same synthetic object.
 * </p>
 */
public class GeometrySlice3DViewInfo extends AbstractViewInfo {

    @Override
    public String getTitle() {
        return "Geometry Slice 3D View";
    }

    @Override
    public String getPurpose() {
        return """
                This view displays the full synthetic three-dimensional model used
                by the Geometry Slice Demo View. It is the companion view to the
                2D slice display.

                The 3D view makes the geometry understandable: it shows the region
                shells, the wire-like line segments, the fake-hit wires, and the
                current constant-phi slice plane in their full spatial context.
                The corresponding 2D view then shows the diagnostic cross section
                produced by intersecting this same model with the slice plane.

                This is not a separately constructed illustration. The 3D view and
                the 2D slice view use the same GeometrySliceModel, Shell3D, and
                Wire3D objects. That means the two views are guaranteed to be
                showing the same synthetic geometry, just in different forms.
                """;
    }

    @Override
    public List<String> getUsageBullets() {
        return List.of(
                "Use the phi slider to rotate the translucent constant-phi slice plane.",
                "Use the joystick, mouse, wheel, and keyboard controls to rotate, pan, and zoom the 3D scene.",
                "The black wireframes show the synthetic 3D region shells.",
                "The blue line segments show the synthetic 3D wires.",
                "The red line segments mark the same fake-hit wires that appear as red points in the 2D slice view.",
                "Compare this view with the 2D Geometry Slice Demo View to see how a 3D model becomes an item-based 2D diagnostic slice."
        );
    }

    @Override
    public String getTechnicalNotes() {
        return """
                This view is intentionally paired with the 2D Geometry Slice Demo
                View. Both views are backed by the same synthetic model classes:
                GeometrySliceModel creates the region shells and wires, Shell3D
                describes each region envelope, and Wire3D describes each finite
                3D wire segment and its fake-hit metadata.

                In the 3D view, those model objects are rendered directly as 3D
                lines, shell wireframes, and a translucent constant-phi plane.
                In the 2D view, the same model objects are intersected with that
                plane and then projected into ordinary 2D MDI items.

                The comparison illustrates an important MDI design principle:
                different views can share the same model while presenting it in
                very different ways. The 3D view explains the spatial object; the
                2D slice view makes the selected cross section easier to inspect,
                hit-test, and annotate.
                """;
    }

    @Override
    public String getFooter() {
        return "MDI-3D geometry demo: same model, full 3D context.";
    }

    @Override
    protected String getAccentColorHex() {
        return "#7c5cc4";
    }
}