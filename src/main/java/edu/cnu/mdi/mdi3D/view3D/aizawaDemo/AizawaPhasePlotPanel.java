package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import java.awt.Color;
import java.awt.Dimension;

import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.AReadyPlotPanel;
import edu.cnu.mdi.splot.plot.PlotChangeType;

/**
 * 2D phase plot panel for the Aizawa attractor demo.
 * <p>
 * This panel plots the current trajectory in the x-z plane. Each point added is
 * interpreted as one phase-space sample {@code (x, z)}.
 * </p>
 */
@SuppressWarnings("serial")
public class AizawaPhasePlotPanel extends AReadyPlotPanel {

	/** Name of the phase curve. */
	private static final String PHASE_CURVE = "Aizawa Phase";

	/** Default preferred width. */
	private static final int DEFAULT_PREFERRED_WIDTH = 400;

	/** Default preferred height. */
	private static final int DEFAULT_PREFERRED_HEIGHT = 400;

	/** The plot curve used for the phase portrait. */
	private volatile Curve phaseCurve;

	/**
	 * Construct the phase plot panel.
	 */
	public AizawaPhasePlotPanel() {
		super(true);
		setPreferredSize(new Dimension(DEFAULT_PREFERRED_WIDTH, DEFAULT_PREFERRED_HEIGHT));
		dataSetup();
	}

	@Override
	public void plotChanged(PlotChangeType event) {
		// No special action needed for this demo panel.
	}

	@Override
	public void clearData() {
		for (ACurve curve : canvas.getPlotData().getCurves()) {
			((Curve) curve).clearData();
		}
		canvas.repaint();
	}

	/**
	 * Set the phase data from a packed coordinate array.
	 *
	 * @param coords the packed coordinates in the form [x1, y1, z1, x2, y2, z2, ..., xn, yn, zn]
	 */
	public void setPhaseData(float[] coords) {
	    clearData();

	    if (phaseCurve == null || coords == null) {
	        return;
	    }

	    int n = coords.length / 3;
	    for (int i = 0; i < n; i++) {
	        double x = coords[3 * i];
	        double z = coords[3 * i + 2];
	        phaseCurve.add(x, z);
	    }

	    canvas.repaint();
	}
	
	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { PHASE_CURVE };
		return new PlotData(PlotDataType.XYXY, curveNames, null);
	}

	@Override
	protected String getXAxisLabel() {
		return "x";
	}

	@Override
	protected String getYAxisLabel() {
		return "z";
	}

	@Override
	protected String getPlotTitle() {
		return "Aizawa Phase Plot (x vs z)";
	}

	/**
	 * Add one point to the phase plot.
	 *
	 * @param x the x coordinate
	 * @param z the z coordinate
	 */
	public void addPhasePoint(double x, double z) {
		if (phaseCurve != null) {
			phaseCurve.add(x, z);
			canvas.repaint();
		}
	}

	@Override
	public void setParameters() {
		PlotData plotData = canvas.getPlotData();
		phaseCurve = (Curve) plotData.getCurve(PHASE_CURVE);

		Color fill = new Color(40, 120, 220, 120);

		phaseCurve.setCurveDrawingMethod(CurveDrawingMethod.NONE);
		phaseCurve.getStyle().setSymbolType(SymbolType.CIRCLE);
		phaseCurve.getStyle().setSymbolSize(3);
		phaseCurve.getStyle().setFillColor(fill);
		phaseCurve.getStyle().setLineWidth(2);
		phaseCurve.getStyle().setBorderColor(Color.blue.darker());
	}
}