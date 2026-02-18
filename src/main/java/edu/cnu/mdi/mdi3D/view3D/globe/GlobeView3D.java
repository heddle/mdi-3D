package edu.cnu.mdi.mdi3D.view3D.globe;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.List;

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
import edu.cnu.mdi.util.Environment;

/**
 * A lightweight, interactive 3D globe view.
 * <p>
 * This view demonstrates reuse of the {@code edu.cnu.mdi.mapping} GeoJSON
 * loaders (and the GeoJSON resources that live in the main 2D {@code mdi}
 * project) in a true 3D rendering context. No map projection is used:
 * longitude/latitude coordinates are mapped directly to points on a sphere.
 * <p>
 * This class deliberately extends {@link PlainView3D} (not
 * {@code SimulationView3D}) because the globe is not time-stepped; it is an
 * interactive visualization that redraws in response to user input (mouse
 * drag/arcball rotation, wheel zoom, etc.) provided by {@link Panel3D} and its
 * adapters.
 *
 * <h2>Data reuse</h2> The globe uses {@link GeoJsonCountryLoader} to load
 * country polygon rings from the same GeoJSON resource used by the 2D map demo:
 * <ul>
 * <li>{@value #DEFAULT_COUNTRIES_RESOURCE}</li>
 * </ul>
 *
 * <h2>Dateline seam</h2> Country rings that cross the dateline can produce
 * longitude discontinuities near {@code +/-pi}. The rendering item
 * {@link GlobeCountryLines3D} handles this by splitting polyline strips when a
 * segment exhibits a large longitude jump (a simple, robust approach
 * appropriate for a demo).
 */
@SuppressWarnings("serial")
public class GlobeView3D extends PlainView3D {

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
	 * Create a globe view with a specified countries resource and sphere radius.
	 *
	 * @param countriesResource classpath resource pointing to a countries GeoJSON
	 *                          file
	 * @param radius            globe radius in scene units
	 * @param keyVals           property key/value pairs passed through to
	 *                          {@link PlainView3D}
	 */
	public GlobeView3D(Object... keyVals) {
		super(keyVals);
	}

	/**
	 * Factory used by demo apps (mirrors the pattern in other 3D demo views).
	 *
	 * @return a new {@code GlobeView3D} with a reasonable initial camera pose
	 */
	public static GlobeView3D createGlobeView() {
		return new GlobeView3D(PropertyUtils.TITLE, "3D Globe", PropertyUtils.ANGLE_X, DEFAULT_THETA_X,
				PropertyUtils.ANGLE_Y, DEFAULT_THETA_Y, PropertyUtils.ANGLE_Z, DEFAULT_THETA_Z, PropertyUtils.DIST_X,
				DEFAULT_DIST_X, PropertyUtils.DIST_Y, DEFAULT_DIST_Y, PropertyUtils.DIST_Z, DEFAULT_DIST_Z,
				PropertyUtils.FRACTION, 0.85, PropertyUtils.ASPECT, 1.6);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {

		// Dark background reads nicely for a globe and linework.
		Panel3D panel = new Panel3D(angleX, angleY, angleZ, xDist, yDist, zDist, 0f, 0f, 0f, false) {

			@Override
			public void createInitialItems() {
				// Base sphere (optionally add gridlines later).
				Sphere globe = new Sphere(this, 0f, 0f, 0f, radius, new Color(20, 35, 70));
				globe.setResolution(48, 24);

				globe.setGridColor(new Color(50, 70, 120, 150)); // semi-transparent bluish gridlines
				globe.setGridlines(buildThetaLatLines(15, true), // parallels (latitudes)
						buildPhiLonLines(15) // meridians (longitudes)
				);

				addItem(globe);

				try {
					List<CountryFeature> features = GeoJsonCountryLoader.loadFromResource(countriesResource);
					GlobeCountryLines3D lines = new GlobeCountryLines3D(this, features, radius);
					lines.setLineColor(new Color(230, 230, 230));
					lines.setLineWidth(1.0f);
					lines.setRadialLift(0.0025f); // small lift above sphere surface
					addItem(lines);
				} catch (IOException e) {

					// If you have a preferred logging facility in mdi-3D, swap this out.
					System.err.println("GlobeView3D: failed to load countries from " + countriesResource);
					e.printStackTrace(System.err);
				}


				try {
				    var cities = GeoJsonCityLoader
				        .loadFromResource(Environment.MDI_RESOURCE_PATH + MapResources.CITIES_GEOJSON);

				    // Filter: keep only prominent cities (scalerank small) OR big population
				    var filtered = cities.stream()
				        .filter(c -> c.getScalerank() >= 0 && c.getScalerank() <= 3
				                  || c.getPopulation() >= 2_000_000)
				        .toList();

				    float[] coords = buildCityCoords(filtered, radius, 0.01f);
				    var cityPoints = new PointSet3D(
				        this, coords, new Color(255, 210, 120), 4.0f, true
				    );
				    addItem(cityPoints);

				    // Add labels for the cities. Note that LabelSet3D can reuse the same coordinates as the PointSet3D.
				    String[] cityNames = cityNames(filtered);
				    LabelSet3D cityLabels = new LabelSet3D(this, coords, cityNames);
				    cityLabels.put(Item3D.TEXT_COLOR, new Color(255, 230, 160));
				    cityLabels.put(Item3D.FONT, new Font("SansSerif", Font.PLAIN, 12));
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

	// Helper method to convert city longitude/latitude to 3D coordinates on the globe, with a small radial lift to reduce z-fighting.
	private static float[] buildCityCoords(
	        List<CityFeature> cities,
	        float radius,
	        float radialLift) {

	    float r = radius + radialLift;
	    float[] coords = new float[cities.size() * 3];
	    int i = 0;

	    for (var c : cities) {
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
		// theta = π/2 - lat
		// avoid poles (theta=0,π) because they degenerate
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
