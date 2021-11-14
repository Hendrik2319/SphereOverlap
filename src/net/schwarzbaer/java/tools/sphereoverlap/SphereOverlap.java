package net.schwarzbaer.java.tools.sphereoverlap;

import java.awt.Color;
import java.io.File;
import java.util.Vector;

import net.schwarzbaer.geometry.spacial.ConstPoint3d;
import net.schwarzbaer.geometry.spacial.PointSphere;
import net.schwarzbaer.vrml.IndexedLineSet;
import net.schwarzbaer.vrml.VrmlTools;

public class SphereOverlap {
	
	// based on the Scan Sphere Problem in SpaceEngineers
	
	private static final double RADIUS = 50000;
	
	private Sphere sphere;
	private final Sphere[] spheres;

	public static void main(String[] args) {
		new SphereOverlap().initialize();
	}
	
	SphereOverlap() {
		spheres = new Sphere[] { 
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
				new Sphere( -12446.87, -11794.27, -107519.69 )
		};
		
		sphere = new Sphere(new ConstPoint3d(0, 0, 0));
	}

	private void initialize() {
		writeToVRML(new File("sphere.wrl"), sphere);
		
		for (int i=0; i<spheres.length; i++) {
			Sphere sp = spheres[i];
			for (int j=0; j<spheres.length; j++) {
				if (i==j) continue;
				Sphere sp1 = spheres[j];
				Vector<SpherePoint> points = sp.points;
				for (int k=0; k<points.size(); k++) {
					SpherePoint p = points.get(k);
					if (p==null) continue;
					if (sp1.isInside(p)) {
						points.remove(k);
						k--;
					}
				}
				
			}
		}
		writeToVRML(new File("spheres.wrl"), spheres);
	}
	
	private static void writeToVRML(File file, Sphere[] spheres) {
		VrmlTools.writeVRML(file, out->{
			
			IndexedLineSet lineSet = new IndexedLineSet("%1.3f", false);
			for (Sphere sphere : spheres)
				for(SpherePoint p : sphere.points)
					if (p!=null)
						lineSet.addAxesCross(p, RADIUS/100);
			lineSet.writeToVRML(out, Color.BLUE);
			
		});
	}

	private static void writeToVRML(File file, Sphere sphere) {
		VrmlTools.writeVRML(file, out->{
			
			IndexedLineSet lineSet = new IndexedLineSet("%1.3f", false);
			for(SpherePoint p : sphere.points) {
				if (p!=null) {
					lineSet.addAxesCross(p, RADIUS/100);
				}
			}
			lineSet.writeToVRML(out, Color.BLUE);
			
		});
	}

	private static class SpherePoint extends ConstPoint3d {

		@SuppressWarnings("unused")
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
			super(center, RADIUS, 4000, SpherePoint::new);
		}
		
	}
}
