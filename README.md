# Convert Kml to GeoJSON format 

This simple application "KmlToGeojson", converts Kml or Kmz files into a GeoJSON representation. 

[Kml](https://developers.google.com/kml/documentation/kmlreference) is used in Google Earth to display 
various geographic elements, such as; images, place marks, polygon shapes, 3D models, etc...

Similarly [GeoJSON](http://geojson.org/) is a format for encoding a variety of geographic data structures.

This scala application uses the [scalakml library](https://github.com/workingDog/scalakml) and 
the [play-geojson library](https://github.com/jroper/play-geojson) to convert Kml to GeoJSON format.
 
## Kml to GeoJSON mapping

Currently the following mapping is implemented.

    Kml object -> list of GeoJSON object
    Kml Folder -> GeoJSON FeatureCollection
    Kml Document -> GeoJSON FeatureCollection
    Kml MultiGeometry -> GeoJSON GeometryCollection
    Kml Placemark -> GeoJSON Feature
    Kml Point -> GeoJSON Point
    Kml LineString -> GeoJSON LineString
    Kml LinearRing -> GeoJSON LineString
    Kml Polygon -> GeoJSON Polygon
    Kml LatLonAltBox -> GeoJSON bbox

The GeoJSON Feature "properties" are generated from the following Kml elements:

    name, description, address, phoneNumber, styleUrl, visibility, open, 
    timeSpan (begin and end), timeStamp (when),
    extendedData (displayName, name, value), altitudeMode, extrude
 
The Kml "id" attribute is converted to the GeoJSON Feature "id".

Everything else is ignored.
 
Only [WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System) coordinate reference system 
is supported and all longitudes and latitudes are in decimal degrees.
 
## References
 
1) OGC 07-147r2 Version: 2.2.0, Category: OGC Standard, Editor: Tim Wilson, at http://www.opengeospatial.org/standards/kml

2) Google developers KML Reference, at https://developers.google.com/kml/documentation/kmlreference

3) GeoJSON reference document, at http://geojson.org/geojson-spec.html

## Dependencies

Depends on the scala [scalakml library](https://github.com/workingDog/scalakml)
and its companion library [scalaxal](https://github.com/workingDog/scalaxal), 
and on the scala [play-geojson library](https://github.com/jroper/play-geojson).

For convenience, some of these libraries are included here in the lib directory.

## Installation and packaging

To use as a library, add the following dependency to build.sbt:

    libraryDependencies += "com.github.workingDog" %% "kmltogeojson" % "1.0"

The easiest way to compile and package the application from source is to use [SBT](http://www.scala-sbt.org/).
To assemble the application and all its dependencies into a single jar file type:

    sbt assembly

This will produce "KmlToGeojson-1.0.jar" in the "./target/scala-2.11" directory.

## Usage

Once "KmlToGeojson-1.0.jar" has been generated simply type at the prompt:
 
    java -jar KmlToGeojson-1.0.jar kml_file.kml geojson_file.geojson
 
where "kml_file.kml" is the Kml file you want to convert, and "geojson_file.geojson" is the destination file 
with the [GeoJSON](http://geojson.org/) format results. If the "geojson_file.geojson" is absent, the output is directed to console.
 
You can also use the "KmlConverter.scala" class in your code, such as: 

    val geojson = KmlConverter().toGeoJson(kml)
    
This gives you a [play-geojson object](https://github.com/jroper/play-geojson) that 
can easily be converted to [GeoJSON](http://geojson.org/) format using:
  
    geojson.foreach(obj => println(Json.prettyPrint(Json.toJson(obj))))
  
"KmlConverter.scala" has one generic method "toGeoJson()" that takes any of the implemented Kml objects. 
See also "TestGeoJson".

## Status

stable

Using scala 2.11.8, java 8 and SBT-0.13.11.


Ringo Wathelet
