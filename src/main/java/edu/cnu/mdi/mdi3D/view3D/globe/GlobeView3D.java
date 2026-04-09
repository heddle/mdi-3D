package edu.cnu.mdi.mdi3D.view3D.globe;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import edu.cnu.mdi.mapping.GeoJsonCityLoader;
import edu.cnu.mdi.mapping.GeoJsonCityLoader.CityFeature;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mapping.MapResources;
import edu.cnu.mdi.mdi3D.item3D.GlobeCountryLines3D;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.item3D.LabelSet3D;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.item3D.Sphere;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.ViewConfiguration;
import edu.cnu.mdi.view.ViewPropertiesBuilder;
import edu.cnu.mdi.view.VirtualView;

/**
 * A lightweight, interactive 3D globe view.
 * <p>
 * This view demonstrates reuse of the {@code edu.cnu.mdi.mapping} GeoJSON
 * loaders and the GeoJSON resources that live in the main 2D {@code mdi}
 * project in a true 3D rendering context. No map projection is used:
 * longitude/latitude coordinates are mapped directly to points on a sphere.
 * </p>
 * <p>
 * This class deliberately extends {@link PlainView3D} rather than
 * {@code SimulationView3D} because the globe is not time-stepped; it is an
 * interactive visualization that redraws in response to user input.
 * </p>
 *
 * <h2>Dateline seam</h2>
 * <p>
 * Country rings that cross the dateline can produce longitude discontinuities
 * near {@code +/-pi}. The rendering item {@link GlobeCountryLines3D} handles
 * this by splitting polyline strips when a segment exhibits a large longitude
 * jump.
 * </p>
 */
@SuppressWarnings("serial")
public class GlobeView3D extends PlainView3D {

	/** Default frame title for the globe view. */
	private static final String TITLE = "3D Globe";

	/** Default globe radius in scene units. */
	public static final float DEFAULT_RADIUS = 1.0f;

	// Sensible initial camera/rotation defaults for a globe demo.
	private static final float DEFAULT_THETA_X = -25f;
	private static final float DEFAULT_THETA_Y = 35f;
	private static final float DEFAULT_THETA_Z = 0f;

	private static final float DEFAULT_DIST_X = 0f;
	private static final float DEFAULT_DIST_Y = 0f;
	private static final float DEFAULT_DIST_Z = -3.0f;

	private final String countriesResource = Environment.MDI_RESOURCE_PATH + MapResources.COUNTRIES_GEOJSON;
	private final float radius = DEFAULT_RADIUS;

	/**
	 * Construct a globe view using its canonical default properties.
	 */
	public GlobeView3D() {
		this(createDefaultProperties());
	}

	/**
	 * Construct a globe view using the supplied properties.
	 *
	 * @param props the properties used to configure the view
	 */
	public GlobeView3D(Properties props) {
		super(props);
	}

	/**
	 * Legacy compatibility constructor using alternating key/value pairs.
	 *
	 * @param keyVals alternating property key/value pairs
	 */
	public GlobeView3D(Object... keyVals) {
		this(PropertyUtils.fromKeyValues(keyVals));
	}

	/**
	 * Create the default properties for this view.
	 *
	 * @return the default view properties
	 */
	private static Properties createDefaultProperties() {
		return new ViewPropertiesBuilder()
				.title(TITLE)
				.put(PropertyUtils.ANGLE_X, DEFAULT_THETA_X)
				.put(PropertyUtils.ANGLE_Y, DEFAULT_THETA_Y)
				.put(PropertyUtils.ANGLE_Z, DEFAULT_THETA_Z)
				.put(PropertyUtils.DIST_X, DEFAULT_DIST_X)
				.put(PropertyUtils.DIST_Y, DEFAULT_DIST_Y)
				.put(PropertyUtils.DIST_Z, DEFAULT_DIST_Z)
				.fraction(0.85)
				.aspect(1.6)
				.useContainer(false)
				.build();
		
		
	}

	/**
	 * Get the lazy-creation configuration for this view.
	 *
	 * @return the view configuration for lazy creation
	 */
	public static ViewConfiguration<GlobeView3D> getConfiguration() {
		return ViewConfiguration.lazy(TITLE, GlobeView3D::new, 1, 0, 0, VirtualView.CENTER);
	}

	/**
	 * Compatibility factory used by demo apps.
	 * <p>
	 * Newer code should generally prefer {@code new GlobeView3D()}.
	 * </p>
	 *
	 * @return a new default-configured globe view
	 */
	public static GlobeView3D createGlobeView() {
		return new GlobeView3D();
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {

		// Dark background reads nicely for a globe and linework.
		Panel3D panel = new Panel3D(angleX, angleY, angleZ, xDist, yDist, zDist, 0f, 0f, 0f, false) {
			
			@Override
			public void createInitialItems() {
				Sphere globe = new Sphere(this, 0f, 0f, 0f, radius, new Color(20, 35, 70));
				globe.setResolution(48, 24);

				globe.setGridColor(new Color(50, 70, 120, 150));
				globe.setGridlines(buildThetaLatLines(15, true), buildPhiLonLines(15));

				addItem(globe);

				try {
					List<CountryFeature> features = GeoJsonCountryLoader.loadFromResource(countriesResource);
					GlobeCountryLines3D lines = new GlobeCountryLines3D(this, features, radius);
					lines.setLineColor(new Color(230, 230, 230));
					lines.setLineWidth(1.0f);
					lines.setRadialLift(0.0025f);
					addItem(lines);
				} catch (IOException e) {
					System.err.println("GlobeView3D: failed to load countries from " + countriesResource);
					e.printStackTrace(System.err);
				}

				try {
					var cities = GeoJsonCityLoader
							.loadFromResource(Environment.MDI_RESOURCE_PATH + MapResources.CITIES_GEOJSON);

					var filtered = cities.stream()
							.filter(c -> (c.getScalerank() >= 0 && c.getScalerank() <= 3)
									|| c.getPopulation() >= 2_000_000)
							.toList();

					float[] coords = buildCityCoords(filtered, radius, 0.01f);
					PointSet3D cityPoints = new PointSet3D(this, coords, new Color(255, 210, 120), 4.0f, true);
					addItem(cityPoints);

					String[] cityNames = cityNames(filtered);
					LabelSet3D cityLabels = new LabelSet3D(this, coords, cityNames);
					cityLabels.put(Item3D.TEXT_COLOR, new Color(255, 230, 160));
					cityLabels.put(Item3D.FONT, Fonts.plainFontDelta(-2));
					cityLabels.setCull(LabelSet3D.frontHemisphereCull(0f, 0f, 0f));
					addItem(cityLabels);

				} catch (IOException e) {
					System.err.println("GlobeView3D: failed to load cities");
					e.printStackTrace(System.err);
				}
			}
		};

		return panel;
	}

	private static String[] cityNames(List<CityFeature> cities) {
		String[] names = new String[cities.size()];
		for (int i = 0; i < cities.size(); i++) {
			names[i] = cities.get(i).getName();
		}
		return names;
	}

	// Helper method to convert city longitude/latitude to 3D coordinates on the globe,
	// with a small radial lift to reduce z-fighting.
	private static float[] buildCityCoords(List<CityFeature> cities, float radius, float radialLift) {

		float r = radius + radialLift;
		float[] coords = new float[cities.size() * 3];
		int i = 0;

		for (CityFeature c : cities) {
			double lon = c.getLongitude();
			double lat = c.getLatitude();

			double cosLat = Math.cos(lat);
			coords[i++] = (float) (r * cosLat * Math.cos(lon));
			coords[i++] = (float) (r * cosLat * Math.sin(lon));
			coords[i++] = (float) (r * Math.sin(lat));
		}
		return coords;
	}

	// Helper methods to build gridline arrays for the sphere item. These are public
	// static so they can be reused by the demo app's menu actions.
	private static float[] buildThetaLatLines(int degStep, boolean includeEquator) {
		List<Float> list = new java.util.ArrayList<>();
		for (int latDeg = -90 + degStep; latDeg <= 90 - degStep; latDeg += degStep) {
			if (!includeEquator && latDeg == 0) {
				continue;
			}
			double lat = Math.toRadians(latDeg);
			float theta = (float) (Math.PI / 2.0 - lat);
			list.add(theta);
		}
		float[] out = new float[list.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = list.get(i);
		}
		return out;
	}

	private static float[] buildPhiLonLines(int degStep) {
		List<Float> list = new java.util.ArrayList<>();
		for (int lonDeg = -180; lonDeg < 180; lonDeg += degStep) {
			list.add((float) Math.toRadians(lonDeg));
		}
		float[] out = new float[list.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = list.get(i);
		}
		return out;
	}
}