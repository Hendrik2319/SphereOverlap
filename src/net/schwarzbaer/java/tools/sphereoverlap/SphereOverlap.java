package net.schwarzbaer.java.tools.sphereoverlap;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.schwarzbaer.geometry.spacial.ConstPoint3d;
import net.schwarzbaer.geometry.spacial.PointSphere;
import net.schwarzbaer.vrml.IndexedFaceSet;
import net.schwarzbaer.vrml.IndexedLineSet;
import net.schwarzbaer.vrml.VrmlTools;

public class SphereOverlap {
	
	// based on the Scan Sphere Problem in SpaceEngineers
	
	private static final double RADIUS = 50000;

	public static void main(String[] args) {
		new SphereOverlap().initialize();
	}
	
	private final Vector<TestCase> testCases;
	
	SphereOverlap() {
		testCases = new Vector<>();
		testCases.add( new TestCase("Single Sphere", RADIUS/100, "%1.1f", Color.RED, new Sphere[] { 
				new Sphere(new ConstPoint3d(RADIUS*1.6, 0, 0))
		}));
		testCases.add( new TestCase("SpaceEngineer", RADIUS/100, "%1.1f", Color.ORANGE, new Sphere[] { 
				new Sphere(   -374.04,    309.45,    -450.11 ),
				new Sphere(   6067.64,  18269.06,   25806.78 ),
				new Sphere(  36924.51,  17250.54,     100.49 ),
				new Sphere(  31152.43, -14553.81,  -27841.96 ),
				new Sphere( -18964.20, -32550.94,  -26076.28 ),
				new Sphere(  39803.03,   7282.90,   21970.95 ),
				new Sphere( -30000.08,  29999.87,   29999.98 ),
				new Sphere(  -6421.75,  14191.34,  -53819.49 ),
				new Sphere(  47744.67,  54773.03,  -13383.40 ),
				new Sphere( -24134.06, -17430.58,  -69033.38 ),
				new Sphere( -12446.87, -11794.27, -107519.69 ),
		}));
		testCases.add( new TestCase("Tetraeder", 1, "%1.2f", Color.GREEN, new Sphere[] { // 50 / 57.73502692 / 61.2372 
				new Sphere(  0.0000,  0.0000,  0.0000, 57.9, 4000),
				new Sphere(100.0000,  0.0000,  0.0000, 57.9, 4000),
				new Sphere( 50.0000, 86.6025,  0.0000, 57.9, 4000),
				new Sphere( 50.0000, 28.8675, 81.6497, 57.9, 4000),
		}));
		testCases.add( new TestCase( "Random", 1.00, "%1.2f", Color.ORANGE,
				Sphere.createRandomSpheres(20, 20,50, 200,200,100, 4000)
		));
		testCases.add(new TestCase( "DebugCase 1", 1.00, "%1.2f", new Color(0xFFFFC800, true), new Sphere[] {
				new Sphere( 40.85, -97.31, 32.23, 31.48, 1586),
				new Sphere( -51.46, 93.45, -26.41, 26.34, 1110),
				new Sphere( 24.64, 98.40, -13.29, 20.27, 657),
				new Sphere( 96.58, -0.98, 32.03, 31.56, 1593),
				new Sphere( -51.39, -51.92, -19.66, 35.37, 2001),
				new Sphere( -73.20, -31.60, 44.99, 45.79, 3355),
				new Sphere( 12.68, -52.36, 40.34, 39.94, 2553),
				new Sphere( 7.71, -76.20, -3.19, 43.54, 3033),
				new Sphere( -86.34, -61.38, 20.32, 46.35, 3437),
				new Sphere( 46.32, 10.40, 36.43, 21.23, 721),
				new Sphere( 22.44, 76.58, -9.66, 21.49, 739),
				new Sphere( -31.54, -6.85, -13.79, 33.26, 1770),
				new Sphere( -58.12, -60.18, 16.98, 39.66, 2516),
				new Sphere( 48.18, 34.62, -14.28, 42.60, 2904),
				new Sphere( 10.71, -63.87, -19.09, 24.79, 983),
				new Sphere( -58.36, -5.53, 22.95, 38.13, 2326),
				new Sphere( 74.98, -55.97, 21.34, 24.68, 975),
				new Sphere( -94.68, -80.55, -33.46, 45.83, 3360),
				new Sphere( -33.38, -64.22, 41.81, 40.49, 2624),
				new Sphere( -99.80, -38.62, 5.82, 33.38, 1782),
		}));
		
		
	}
	
	private static class TestCase {
		private final String label;
		private final Sphere[] spheres;
		private final double pointSize;
		private final Color diffuseColor;
		private final String pointCoordFormat;

		TestCase(String label, double pointSize, String pointCoordFormat, Color diffuseColor, Sphere[] spheres) {
			this.label = label;
			this.pointSize = pointSize;
			this.pointCoordFormat = pointCoordFormat;
			this.diffuseColor = diffuseColor;
			this.spheres = spheres;
		}

		public void writeConfigToVRML(PrintWriter out) {
			out.printf(Locale.ENGLISH, "#new TestCase( \"%s\", "+pointCoordFormat+", \"%s\", new Color(0x%08X, true), new Sphere[] {%n", label, pointSize, pointCoordFormat, diffuseColor.getRGB());
			for(Sphere sp : spheres)
				out.printf(Locale.ENGLISH, "#\t\t%s, # %d points%n", sp.toConstructorString(pointCoordFormat), sp.points.size());
			out.println("#})");
		}
	}

	private void initialize() {
		for (TestCase tc : testCases) {
			boolean debug = tc.label.startsWith("DebugCase");
			Consumer<PrintWriter> extra = tc::writeConfigToVRML;
			if (tc.spheres.length > 1) {
				removeOverlap(tc.spheres,
						null //debug ? Arrays.asList(tc.spheres[12]/* ,tc.spheres[8],tc.spheres[4] */) : null
				);
				if (debug) {
					IndexedLineSet[] lineSets = OverlapEdgeCircle.compute_debug(tc.spheres, tc.pointCoordFormat);
					if (lineSets!=null) {
						Consumer<PrintWriter> oldExtra = extra;
						extra = out->{
							if (oldExtra!=null) oldExtra.accept(out);
							for (int i=0; i<lineSets.length; i++) {
								Color color = Color.RED;
								switch (i%4) {
								case 0 : color = Color.RED  ; break;
								case 1 : color = Color.GREEN; break;
								case 2 : color = Color.BLUE ; break;
								case 3 : color = Color.MAGENTA; break;
								}
								
								//if (i<=32) // is OK
									color = Color.BLACK;
								
								boolean isBuggy = false;  // TODO: buggy lines
								switch (i) {
								case  6: isBuggy = true; color = Color.BLUE; break;
								//case 19: isBuggy = true; color = Color.GREEN; break;
								case 32: isBuggy = true; color = Color.RED; break;
								}
								
								IndexedLineSet lineSet = lineSets[i];
								if (!lineSet.isEmpty()) {
									out.printf("%n# LineSet %d", i);
									if (isBuggy) out.print(" ######################################################");
									out.println();
									lineSet.writeToVRML(out, color);
								}
							}
						};
					}
					
				} else {
					IndexedLineSet lineSet = OverlapEdgeCircle.compute(tc.spheres, tc.pointCoordFormat);
					if (lineSet!=null) {
						Consumer<PrintWriter> oldExtra = extra;
						extra = out->{
							if (oldExtra!=null) oldExtra.accept(out);
							lineSet.writeToVRML(out, darker(tc.diffuseColor, 0.5f));
						};
					}
				}
			}
			writeToVRMLasPointFaces(new File(tc.label+".wrl"), debug, tc.spheres, tc.pointSize, tc.pointCoordFormat, tc.diffuseColor, extra);
		}
	}
	
	private static Color darker(Color c, float ratio) {
		float r = c.getRed  ()/255f * ratio;
		float g = c.getGreen()/255f * ratio;
		float b = c.getBlue ()/255f * ratio;
		return new Color(r,g,b);
	}
	
	static void forEachSpherePair(Sphere[] spheres, boolean onlyUniquePairs, BiConsumer<Sphere,Sphere> action) {
		for (int i=0; i<spheres.length; i++)
			for (int j=onlyUniquePairs ? i+1 : 0; j<spheres.length; j++)
				if (i!=j)
					action.accept(spheres[i], spheres[j]);
	}

	private static void removeOverlap(Sphere[] spheres, List<Sphere> excludeSpheres) {
		forEachSpherePair(spheres, false, (sp,sp1)->{
			if (excludeSpheres!=null && excludeSpheres.contains(sp)) return;
			Vector<SpherePoint> points = sp.points;
			for (int k=0; k<points.size(); k++) {
				SpherePoint p = points.get(k);
				if (p==null) continue;
				if (sp1.isInside(p)) {
					points.remove(k);
					k--;
				}
			}
		});
	}
	
	@SuppressWarnings("unused")
	private static void writeToVRML(File file, Sphere[] spheres) {
		writeToVRMLasPointCrosses(file, spheres, RADIUS/100, Color.BLUE);
	}

	private static void writeToVRMLasPointCrosses(File file, Sphere[] spheres, double pointSize, Color color) {
		VrmlTools.writeVRML(file, out->{
			
			IndexedLineSet lineSet = new IndexedLineSet("%1.3f", false);
			for (Sphere sphere : spheres)
				for(SpherePoint p : sphere.points)
					if (p!=null) {
						lineSet.addAxesCross(p, pointSize);
					}
			lineSet.writeToVRML(out, color);
			
		});
	}
	
	private static void writeToVRMLasPointFaces(File file, boolean debug, Sphere[] spheres, double pointSize, String pointCoordFormat, Color diffuseColor, Consumer<PrintWriter> writeExtra) {
		VrmlTools.writeVRML(file, out->{
			
			if (writeExtra!=null) writeExtra.accept(out);
			
			if (debug) {
				for (int i=0; i<spheres.length; i++) {
					Color color = Color.RED;
					switch (i%4) {
					case 0 : color = Color.RED  ; break;
					case 1 : color = Color.GREEN; break;
					case 2 : color = Color.BLUE ; break;
					case 3 : color = Color.MAGENTA; break;
					}
					
					//if (i<=19)
						color = Color.BLACK;
					
					boolean isBuggy = false; // TODO: buggy spheres
					switch (i) {
					case  5: isBuggy = true; color = Color.RED  ; break;
					//case  7: isBuggy = true; color = Color.GREEN; break;
					case 12: isBuggy = true; color = Color.BLUE ; break;
					}
					
					Sphere sphere = spheres[i];
					IndexedFaceSet faceSet = new IndexedFaceSet(pointCoordFormat, false, false);
					addToFaceset(faceSet, pointSize, sphere);
					out.printf("%n# FaceSet %d", i);
					if (isBuggy) out.print(" ###################################################");
					out.println();
					faceSet.writeToVRML(out, false, color, Color.WHITE, null);
				}
				
			} else {
				IndexedFaceSet faceSet = new IndexedFaceSet(pointCoordFormat, false, false);
				for (Sphere sphere : spheres)
					addToFaceset(faceSet, pointSize, sphere);
				faceSet.writeToVRML(out, false, diffuseColor, Color.WHITE, null);
			}
			
		});
	}

	private static void addToFaceset(IndexedFaceSet faceSet, double pointSize, Sphere sphere) {
		for(SpherePoint p : sphere.points)
			if (p!=null) {
				faceSet.addPointFace(p, p.normal, pointSize);
			}
	}

	private static class SpherePoint extends ConstPoint3d {

		private final ConstPoint3d normal;

		public SpherePoint(ConstPoint3d center, double x, double y, double z) {
			super(x, y, z);
			normal = this.sub(center).normalize();
		}

		@Override
		public String toString() {
			return String.format("SpherePoint [x=%s, y=%s, z=%s, normal=%s]", x, y, z, normal);
		}
	}
	
	static class Sphere extends PointSphere<SpherePoint> {

		final int nPoints;
		
		public Sphere(double x, double y, double z) {
			this(new ConstPoint3d(x,y,z));
		}
		public Sphere(ConstPoint3d center) {
			this(center, RADIUS, 4000);
		}
		public Sphere(double x, double y, double z, double radius, int nPoints) {
			this(new ConstPoint3d(x,y,z), radius, nPoints);
		}
		public Sphere(ConstPoint3d center, double radius, int nPoints) {
			super(center, radius, nPoints, SpherePoint::new);
			this.nPoints = nPoints;
		}
		
		@Override
		public String toString() {
			return String.format("Sphere [center=%s, radius=%s, nPoints=%s]", center, radius, nPoints);
		}
		
		public String toConstructorString(String coordFormat) {
			return String.format(Locale.ENGLISH, "new Sphere( "+coordFormat+", "+coordFormat+", "+coordFormat+", "+coordFormat+", %d)", center.x, center.y, center.z, radius, nPoints);
		}
		public static Sphere[] createRandomSpheres(int nSpheres, double minRadius, double maxRadius, double xSize, double ySize, double zSize, int nPoints) {
			Sphere[] spheres = new Sphere[nSpheres];
			for (int i=0; i<nSpheres; i++) {
				double x = (Math.random()-0.5)*xSize;
				double y = (Math.random()-0.5)*ySize;
				double z = (Math.random()-0.5)*zSize;
				double r = Math.random()*(maxRadius-minRadius) + minRadius;
				int n = (int) Math.round( nPoints * r*r/maxRadius/maxRadius );
				spheres[i] = new Sphere(x, y, z, r, n);
			}
			return spheres;
		}
	}
}
