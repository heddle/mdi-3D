package edu.cnu.mdi.mdi3D.view3D.demo;

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

public class EntropyPlotPanel extends AReadyPlotPanel {
	
	// curve name
	private static final String ENTROPY_CURVE = "Entropy";

	
	private volatile Curve entropyCurve;

	public EntropyPlotPanel(int preferredWidth) {
		super(true);
		Dimension dimension = getPreferredSize();
		dimension.width = preferredWidth;
		setPreferredSize(dimension);
		dataSetup();
	}

	@Override
	public void plotChanged(PlotChangeType event) {
		// TODO Auto-generated method stub

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

		entropyCurve.setCurveDrawingMethod(CurveDrawingMethod.NONE);
		entropyCurve.getStyle().setSymbolType(SymbolType.CIRCLE);
		entropyCurve.getStyle().setSymbolSize(2);
		entropyCurve.getStyle().setFillColor(eColor);
		entropyCurve.getStyle().setBorderColor(null);
	}

}
