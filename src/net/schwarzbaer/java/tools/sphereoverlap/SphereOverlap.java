package net.schwarzbaer.java.tools.sphereoverlap;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
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
	}

	private void initialize() {
		for (TestCase tc : testCases) {
			Consumer<PrintWriter> extra = null;
			if (tc.spheres.length > 1) {
				removeOverlap(tc.spheres);
				IndexedLineSet lineSet = OverlapEdgeCircle.compute(tc.spheres, tc.pointCoordFormat);
				if (lineSet!=null)
					extra = out->lineSet.writeToVRML(out, Color.BLACK);
			}
			writeToVRMLasPointFaces(new File(tc.label+".wrl"), tc.spheres, tc.pointSize, tc.pointCoordFormat, tc.diffuseColor, extra);
		}
	}
	
	private static class OverlapEdgeCircle {
		
		static IndexedLineSet compute(Sphere[] spheres, String pointCoordFormat) {
			IndexedLineSet lineSet = new IndexedLineSet(pointCoordFormat, true);
			forEachSpherePair(spheres, true, (sp1,sp2)->{
				OverlapEdgeCircle circle = OverlapEdgeCircle.compute(sp1,sp2);
				if (circle==null) return;
				for(Sphere sp : spheres)
					if (sp!=sp1 && sp!=sp2)
						circle.cutOut(sp);
				circle.addTo(lineSet);
			});
			return lineSet;
		}

		static OverlapEdgeCircle compute(Sphere sp1, Sphere sp2) {
			double distance = sp1.center.getDistance(sp2.center);
			if (distance >= sp1.radius+sp2.radius) return null;
			// d = sqrt( r1^2 - x^2 ) + sqrt( r1^2 - x^2 )
			// x -> radius of circle
			
			// TODO Auto-generated method stub
			return null;
		}

		private void cutOut(Sphere sp) {
			// TODO Auto-generated method stub
		}

		private void addTo(IndexedLineSet lineSet) {
			// TODO Auto-generated method stub
		}
	}

	private static void forEachSpherePair(Sphere[] spheres, boolean onlyUniquePairs, BiConsumer<Sphere,Sphere> action) {
		for (int i=0; i<spheres.length; i++)
			for (int j=onlyUniquePairs ? i+1 : 0; j<spheres.length; j++)
				if (i!=j)
					action.accept(spheres[i], spheres[j]);
	}

	private static void removeOverlap(Sphere[] spheres) {
		forEachSpherePair(spheres, false, (sp,sp1)->{
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
	
	private static void writeToVRMLasPointFaces(File file, Sphere[] spheres, double pointSize, String pointCoordFormat, Color diffuseColor, Consumer<PrintWriter> writeExtra) {
		VrmlTools.writeVRML(file, out->{
			
			IndexedFaceSet faceSet = new IndexedFaceSet(pointCoordFormat, false, false);
			for (Sphere sphere : spheres)
				for(SpherePoint p : sphere.points)
					if (p!=null) {
						faceSet.addPointFace(p, p.normal, pointSize);
					}
			faceSet.writeToVRML(out, false, diffuseColor, Color.WHITE, null);
			
			if (writeExtra!=null) writeExtra.accept(out);
		});
	}

	private static class SpherePoint extends ConstPoint3d {

		private final ConstPoint3d normal;

		public SpherePoint(ConstPoint3d center, double x, double y, double z) {
			super(x, y, z);
			normal = this.sub(center).normalize();
		}
	}
	
	private static class Sphere extends PointSphere<SpherePoint> {

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
		}
		
	}
}
