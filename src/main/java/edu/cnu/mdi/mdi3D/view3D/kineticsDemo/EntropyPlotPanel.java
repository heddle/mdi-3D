package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

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
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * An sPlot-based entropy-vs-time strip chart for the kinetics demo, built
 * from a single connected {@link Curve} that new points are appended to as
 * the simulation runs.
 */
@SuppressWarnings("serial")
public class EntropyPlotPanel extends AReadyPlotPanel {

	// curve name
	private static final String ENTROPY_CURVE = "Entropy";

	// default preferred width for the plot panel
	private static final int DEFAULT_PREFERRED_WIDTH = 400;
	private static final int DEFAULT_PREFERRED_HEIGHT = 400;

	private volatile Curve entropyCurve;

	/**
	 * Create an EntropyPlotPanel with default preferred width.
	 */
	public EntropyPlotPanel() {
		super(true);
		Dimension dimension = new Dimension(DEFAULT_PREFERRED_WIDTH,
				DEFAULT_PREFERRED_HEIGHT);
		setPreferredSize(dimension);
		dataSetup();
	}

	@Override
	public void plotChanged(PlotChangeType event) {
	}

	@Override
	public void clearData() {
		for (ACurve curve : canvas.getPlotData().getCurves()) {
			((Curve)curve).clearData();
		}
		canvas.repaint();
	}

	@Override
	protected PlotData createPlotData() throws PlotDataException {
		String[] curveNames = { ENTROPY_CURVE };
		return new PlotData(PlotDataType.XYXY, curveNames, null);
	}

	@Override
	protected String getXAxisLabel() {
		return "Time";
	}

	@Override
	protected String getYAxisLabel() {
		return "Entropy";
	}

	@Override
	protected String getPlotTitle() {
		return "Entropy vs Time";
	}

	/**
	 * Append one entropy sample to the plot and repaint.
	 * <p>
	 * A no-op if called before {@link #setParameters()} has run and installed
	 * {@link #entropyCurve}.
	 * </p>
	 *
	 * @param x time value
	 * @param y entropy value
	 */
	public void addEntropy(double x, double y) {
		if (entropyCurve != null) {
			entropyCurve.add(x, y);
			canvas.repaint();
		}
	}

	@Override
	public void setParameters() {
		PlotData plotData = canvas.getPlotData();
		Color eColor = X11Colors.getX11Color("red", 128);

		entropyCurve = (Curve) plotData.getCurve(ENTROPY_CURVE);

		entropyCurve.setCurveDrawingMethod(CurveDrawingMethod.CONNECT);
		entropyCurve.getStyle().setSymbolType(SymbolType.CIRCLE);
		entropyCurve.getStyle().setSymbolSize(4);
		entropyCurve.getStyle().setFillColor(eColor);
		entropyCurve.getStyle().setLineWidth(2);
		entropyCurve.getStyle().setBorderColor(Color.red.darker());
	}

}
