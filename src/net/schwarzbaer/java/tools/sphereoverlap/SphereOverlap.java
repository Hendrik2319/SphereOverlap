package net.schwarzbaer.java.tools.sphereoverlap;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.schwarzbaer.geometry.spacial.AxesCross;
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
					extra = out->lineSet.writeToVRML(out, darker(tc.diffuseColor, 0.5f));
			}
			writeToVRMLasPointFaces(new File(tc.label+".wrl"), tc.spheres, tc.pointSize, tc.pointCoordFormat, tc.diffuseColor, extra);
		}
	}
	
	private static Color darker(Color c, float ratio) {
		float r = c.getRed  ()/255f * ratio;
		float g = c.getGreen()/255f * ratio;
		float b = c.getBlue ()/255f * ratio;
		return new Color(r,g,b);
	}
	
	private static class OverlapEdgeCircle {
		
		final ConstPoint3d pos;
		final AxesCross axesCross;
		final double radius;
		private boolean isFullCircle;
		final Vector<Arc> parts;

		private OverlapEdgeCircle(ConstPoint3d pos, ConstPoint3d normal, double radius) {
			this.pos = pos;
			this.radius = radius;
			axesCross = AxesCross.compute(normal);
			parts = new Vector<>();
			isFullCircle = true;
		}

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
			double r1 = sp1.radius;
			double r2 = sp2.radius;
			
			CircleOverlap circleOverlap = CircleOverlap.compute(distance, r1, r2);
			if (circleOverlap.fullCoverage || circleOverlap.noOverlap) return null;
			
			ConstPoint3d n = sp2.center.sub(sp1.center).normalize();
			ConstPoint3d pos = sp1.center.add(n.mul(circleOverlap.pos));
			
			return new OverlapEdgeCircle(pos,n,circleOverlap.height);
		}
		
		private void cutOut(Sphere sphere) {
			if (!isFullCircle && parts.isEmpty())
				return; // empty circle
			
			ConstPoint3d sphereCenter_local = axesCross.toLocal(sphere.center.sub(pos));
			double distToPlane = Math.abs( sphereCenter_local.x );
			if (sphere.radius <= distToPlane)
				return; // sphere doesn't intersect plane 
			
			double intersectionCircleRadius = Math.sqrt(sphere.radius*sphere.radius - distToPlane*distToPlane);
			double distance = Math.sqrt(sphereCenter_local.y*sphereCenter_local.y + sphereCenter_local.z*sphereCenter_local.z);
			
			CircleOverlap circleOverlap = CircleOverlap.compute(distance, radius, intersectionCircleRadius);
			if (circleOverlap.noOverlap) return;
			if (circleOverlap.fullCoverage) {
				if (radius <= intersectionCircleRadius) {
					// intersectionCircle covers this circle -> empty this circle
					isFullCircle = false;
					parts.clear();
				} // else --> this circle covers intersectionCircle --> do nothing
				return;
			}
			
			double angleMid = Math.atan2(sphereCenter_local.z, sphereCenter_local.y);
			double angleAdd = Math.acos(circleOverlap.pos/radius);
			
			Arc overlapArc = new Arc(angleMid-angleAdd, angleMid+angleAdd);
			
			if (isFullCircle) {
				isFullCircle = false;
				parts.clear();
				parts.add(new Arc(overlapArc.max, overlapArc.min+2*Math.PI));
				
			} else
				for (int i=0; i<parts.size();) {
					Arc arc = parts.get(i);
					
					ArcSubstractionResult result = ArcSubstractionResult.compute(arc, overlapArc);
					
					if (result.removeArc)
						parts.remove(i);
					
					else if (result.changeNothing)
						i++;
					
					else {
						if (result.result1==null) throw new IllegalStateException();
						
						if (result.result2==null) {
							// replace this arc with result1 
							parts.set(i, result.result1);
							i++;
							
						} else {
							// replace this arc with result1 and result2
							parts.set(i, result.result2);
							parts.insertElementAt(result.result1, i);
							i+=2;
						}
					}
				}
		}

		private void addTo(IndexedLineSet lineSet) {
			if (isFullCircle)
				lineSet.addFullCircleTo(32, radius, pos, axesCross.yAxis, axesCross.zAxis);
			else
				for(Arc arc : parts)
					lineSet.addArcTo(32, radius, arc.min, arc.max, pos, axesCross.yAxis, axesCross.zAxis);
		}
		
		private static class Arc {
			
			final double min,max;
			
			Arc(double min, double max) {
				if (min> max) throw new IllegalArgumentException();
				if (min==max) throw new IllegalArgumentException();
				this.min = min;
				this.max = max;
			}
		}
		
		private static class CircleOverlap {
			
			final boolean fullCoverage;
			final boolean noOverlap;
			final double height;
			final double pos;
		
			CircleOverlap(boolean noOverlap, boolean fullCoverage) {
				this(noOverlap, fullCoverage, Double.NaN, Double.NaN);
			}
			CircleOverlap(double height, double pos) {
				this(false, false, height, pos);
			}
		
			private CircleOverlap(boolean noOverlap, boolean fullCoverage, double height, double pos) {
				if (noOverlap && fullCoverage) throw new IllegalArgumentException();
				if (!noOverlap && !fullCoverage && (Double.isNaN(height) || Double.isNaN(pos))) throw new IllegalArgumentException();
				if ((noOverlap || fullCoverage) && !Double.isNaN(height) && !Double.isNaN(pos)) throw new IllegalArgumentException();
				this.noOverlap = noOverlap;
				this.fullCoverage = fullCoverage;
				this.height = height;
				this.pos = pos;
			}
			
			static CircleOverlap compute(double distance, double r1, double r2) {
				if (distance >= r1+r2) return new CircleOverlap(true, false);
				if (distance <= Math.abs( r1-r2 )) return new CircleOverlap(false, true);
				// d = (a)d1+d2 || (b)d1-d2 || (c)d2-d1 
				// d = sqrt( r1^2 - x^2 ) + sqrt( r1^2 - x^2 )
				// x -> radius of circle
				// x = (a|b|c) r2² - (d²+r2²-r1²)² / 4d²
				double x = Math.sqrt( r2*r2 - (distance*distance + r2*r2 - r1*r1) * (distance*distance + r2*r2 - r1*r1) / 4 / distance / distance );
				double d1 = Math.sqrt( r1*r1 - x*x );
				double d2 = Math.sqrt( r2*r2 - x*x );
				
				double pos_scalar;
				if (d2>distance) {
					// (c) circle is before center1 (view center1 -> center2)
					pos_scalar = distance-d2;
					
				} else {
					// (b) circle is behind center2 (view center1 -> center2)
					// (a) circle is between center1 and center2
					//     --> behind center1
					pos_scalar = d1;
				}
				
				return new CircleOverlap(x,pos_scalar);
			}
		}

		private static class ArcSubstractionResult {
			
			final boolean removeArc;
			final boolean changeNothing;
			final Arc result1;
			final Arc result2;

			ArcSubstractionResult(boolean removeArc, boolean changeNothing) {
				this(removeArc, changeNothing, null, null);
			}
			ArcSubstractionResult(Arc result1, Arc result2) {
				this(false, false, result1, result2);
			}
			ArcSubstractionResult(boolean removeArc, boolean changeNothing, Arc result1, Arc result2) {
				if (!removeArc && !changeNothing && result1==null && result2==null) throw new IllegalArgumentException();
				if ((removeArc || changeNothing) && (result1!=null || result2!=null)) throw new IllegalArgumentException();
				if (result1==null && result2!=null) throw new IllegalArgumentException();
				this.removeArc = removeArc;
				this.changeNothing = changeNothing;
				this.result1 = result1;
				this.result2 = result2;
			}
			
			static ArcSubstractionResult compute(Arc base, Arc other) {
				if (base==null) throw new IllegalArgumentException();
				if (other==null) throw new IllegalArgumentException();
				
				double otherMax = other.max;
				double otherMin = other.min;
				
				if        (otherMax <= base.min) {
					while (otherMax <= base.min) {
						otherMin += 2*Math.PI;
						otherMax += 2*Math.PI;
					}
					if (base.max <= otherMin)
						// |base|
						//         |other|
						return new ArcSubstractionResult(false, true);
					
				} else if (base.max <= otherMin) {
					while (base.max <= otherMin) {
						otherMin -= 2*Math.PI;
						otherMax -= 2*Math.PI;
					}
					if (otherMax <= base.min)
						//          |base|
						// |other|
						return new ArcSubstractionResult(false, true);
				}
				// -->  (base.min < otherMax)
				//   && (otherMin < base.max)
				
				if (otherMin <= base.min) {
					if (base.max <= otherMax) {
						//    |base|
						// |   other   |
						return new ArcSubstractionResult(true, false);
					} else {
						// otherMax < base.max
						//    |  base  |
						// |  other |
						return new ArcSubstractionResult(new Arc(otherMax, base.max), null);
					}
					
				} else {
					// base.min < otherMin
					if (base.max <= otherMax) {
						// |  base  |
						//    |  other |
						return new ArcSubstractionResult(new Arc(base.min, otherMin), null);
						
					} else {
						// otherMax < base.max
						// |   base    |
						//    |other|
						return new ArcSubstractionResult(new Arc(base.min, otherMin), new Arc(otherMax, base.max));
					}
				}
			}
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
