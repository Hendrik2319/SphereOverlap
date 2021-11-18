package net.schwarzbaer.java.tools.sphereoverlap;

import java.util.Vector;

import net.schwarzbaer.geometry.spacial.AxesCross;
import net.schwarzbaer.geometry.spacial.ConstPoint3d;
import net.schwarzbaer.java.tools.sphereoverlap.SphereOverlap.Sphere;
import net.schwarzbaer.vrml.IndexedLineSet;

class OverlapEdgeCircle {
	
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

	@Override public String toString() {
		return String.format("OverlapEdgeCircle [pos=%s, radius=%s, isFullCircle=%s, %d parts]", pos, radius, isFullCircle, parts.size());
	}

	static IndexedLineSet compute(Sphere[] spheres, String pointCoordFormat) {
		IndexedLineSet lineSet = new IndexedLineSet(pointCoordFormat, true);
		SphereOverlap.forEachSpherePair(spheres, true, (sp1,sp2)->{
			compute(sp1, sp2, spheres, lineSet);
		});
		return lineSet;
	}

	private static void compute(Sphere sp1, Sphere sp2, Sphere[] spheres, IndexedLineSet lineSet) {
		OverlapEdgeCircle circle = compute(sp1,sp2);
		if (circle==null) return;
		checkCircleOnSpheres(sp1, sp2, circle);
		
		for (int i=0; i<spheres.length; i++) {
			Sphere sp = spheres[i];
			if (sp!=sp1 && sp!=sp2)
				circle.cutOut(sp);
		}
		circle.addTo(lineSet);
	}

	private static void checkCircleOnSpheres(Sphere sp1, Sphere sp2, OverlapEdgeCircle circle) {
		boolean isOnSphere1 = true;
		boolean isOnSphere2 = true;
		
		for (int i=0; i<5; i++) {
			
			double dist, angle = Math.random()*2*Math.PI;
			ConstPoint3d p = ConstPoint3d.computePointOnCircle(angle, circle.radius, circle.pos, circle.axesCross.yAxis, circle.axesCross.zAxis);
			
			dist = sp1.center.getDistance(p);
			if (dist < sp1.radius*0.9 || sp1.radius*1.1 < dist)
				isOnSphere1 = false;
			
			dist = sp2.center.getDistance(p);
			if (dist < sp2.radius*0.9 || sp2.radius*1.1 < dist)
				isOnSphere2 = false;
		}
		if (!isOnSphere1 || !isOnSphere2)
			System.err.printf("OverlapEdgeCircle not on %s:   Circle: %s   Sphere1: %s   Sphere2: %s%n", isOnSphere1 && isOnSphere2 ? "Sphere1 & Sphere2" : isOnSphere1 ? "Sphere1" : "Sphere2", circle, sp1, sp2);
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
			if (Double.isNaN(min)) throw new IllegalArgumentException();
			if (Double.isNaN(max)) throw new IllegalArgumentException();
			if (min> max) throw new IllegalArgumentException();
			if (min==max) throw new IllegalArgumentException();
			this.min = min;
			this.max = max;
		}

		@Override public String toString() {
			return String.format("Arc [min=%s, max=%s]", min, max);
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
		
		@Override public String toString() {
			return String.format("CircleOverlap [fullCoverage=%s, noOverlap=%s, height=%s, pos=%s]", fullCoverage, noOverlap, height, pos);
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
			if (distance<d2 && d1<d2) {
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
		
		@Override public String toString() {
			return String.format("ArcSubstractionResult [removeArc=%s, changeNothing=%s, result1=%s, result2=%s]", removeArc, changeNothing, result1, result2);
		}
		
		static ArcSubstractionResult compute(Arc base, Arc other) {
			if (base==null) throw new IllegalArgumentException();
			if (other==null) throw new IllegalArgumentException();
			
			double otherMax = other.max;
			double otherMin = other.min;
			
			if        (otherMax <= base.min) {
				//          |base|
				// |other|
				while (otherMax <= base.min) {
					otherMin += 2*Math.PI;
					otherMax += 2*Math.PI;
				}
				if (base.max <= otherMin)
					// |base|
					//         |other|
					return new ArcSubstractionResult(false, true);
				
			} else if (base.max <= otherMin) {
				// |base|
				//         |other|
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
					if (otherMin+2*Math.PI < base.max)
						//    |     base     |
						// |  other |      | other' |
						return new ArcSubstractionResult(new Arc(otherMax, otherMin + 2*Math.PI), null);
					else
						//    |  base  |
						// |  other |
						return new ArcSubstractionResult(new Arc(otherMax, base.max), null);
				}
				
			} else {
				// base.min < otherMin
				if (base.max <= otherMax) {
					if (base.min < otherMax-2*Math.PI)
						//         |  base  |
						// |  other' |    |  other |
						return new ArcSubstractionResult(new Arc(otherMax - 2*Math.PI, otherMin), null);
					else
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