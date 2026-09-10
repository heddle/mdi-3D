package edu.cnu.mdi.mdi3D.adapter3D;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.IntConsumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A small labeled slider producing a 0-255 alpha value, meant for a
 * "how opaque should translucent geometry be" control on a {@link
 * Panel3D}-based view.
 *
 * <p>
 * This widget only handles the UI: reading its current value ({@link
 * #getAlpha()}) and deciding what that value actually controls -- which
 * items, and how -- is left entirely to the caller. In particular, this
 * widget does <em>not</em> itself touch any {@code Item3D}'s {@code
 * FILLALPHA} property; an item not participating in {@link Panel3D}'s
 * opaque/transparent pass split (see {@code Item3D#setFillAlpha(int)})
 * will not actually blend just because this slider moved. The optional
 * {@link IntConsumer} callback given to the full constructor is the hook
 * for a caller that needs to do that bookkeeping (e.g. propagating the
 * new value to a known set of items) before the resulting {@link
 * Panel3D#refresh()} repaints.
 * </p>
 */
@SuppressWarnings("serial")
public class AlphaSlider extends JPanel implements ChangeListener {

	private static final int SLIDER_WIDTH = 140;
	private static final int INITIAL_VALUE = 24;

	private final Panel3D panel3D;
	private final IntConsumer onChange;
	private final JSlider slider;

	/** A slider with no extra {@code onChange} side effect beyond {@link Panel3D#refresh()}. */
	public AlphaSlider(Panel3D panel3D, String prompt) {
		this(panel3D, prompt, null);
	}

	/**
	 * @param panel3D  the panel to refresh whenever the slider moves
	 * @param prompt   label text shown to the left of the slider
	 * @param onChange called with the new 0-255 value before {@code
	 *                 panel3D.refresh()}, or {@code null} for none
	 */
	public AlphaSlider(Panel3D panel3D, String prompt, IntConsumer onChange) {
		this.panel3D = panel3D;
		this.onChange = onChange;

		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));

		JLabel label = new JLabel(prompt);
		label.setFont(Fonts.smallFont);
		add(label);

		slider = new JSlider(SwingConstants.HORIZONTAL, 0, 255, INITIAL_VALUE);
		slider.setMajorTickSpacing(50);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setFont(Fonts.tinyFont);
		slider.setFocusable(false); // avoid an ugly focus border
		slider.addChangeListener(this);

		Dimension d = slider.getPreferredSize();
		d.width = SLIDER_WIDTH;
		slider.setPreferredSize(d);

		add(slider);
	}

	/** Current alpha value, 0-255. */
	public int getAlpha() {
		return slider.getValue();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (onChange != null) {
			onChange.accept(slider.getValue());
		}
		panel3D.refresh();
	}
}
