# SphereOverlap
`SphereOverlap` is a geometry project, that should compute for a set of spheres the overlaps bvetween each of them.  
It computes overlaps for 6 hardcoded sets of spheres and writes the result in a VRML97 file for each set.

### Usage
You can download a release [here](https://github.com/Hendrik2319/SphereOverlap/releases).  
You will need a JAVA 17 VM.

### Development
`SphereOverlap` is configured as a Eclipse project.  
It depends on following libraries:
* [`JavaLib_Common_Essentials`](https://github.com/Hendrik2319/JavaLib_Common_Essentials)
* [`JavaLib_Common_VRML`](https://github.com/Hendrik2319/JavaLib_Common_VRML)
* [`JavaLib_Common_Geometry`](https://github.com/Hendrik2319/JavaLib_Common_Geometry)

These libraries are imported as "project imports" in Eclipse. 
If you want to develop for your own and
* you use Eclipse as IDE,
	* then you should clone the projects above too and add them to the same workspace as the `SphereOverlap` project.
* you use another IDE (e.q. VS Code)
	* then you should clone the said projects, build JAR files of them and add the JAR files as libraries.

### Screenshots
Example 1
![Example 1](/github/Example1.png)

Example 2
![Example 2](/github/Example2.png)

