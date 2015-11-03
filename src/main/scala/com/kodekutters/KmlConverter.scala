package com.kodekutters

import com.scalakml.kml._
import com.scalakml.{kml => KML}

import play.extras.{geojson => GEOJS}
import play.api.libs.json._
import play.extras.geojson._

import scala.collection.mutable
import scala.collection.mutable.MutableList

import scala.collection.immutable.Seq

/**
  * convert Kml objects into GeoJson objects
  *
  * @author R. Wathelet
  *
  *         ref: https://github.com/workingDog/scalakml
  *         ref: https://github.com/jroper/play-geojson
  */
object KmlConverter {
  def apply() = new KmlConverter()
}

/**
  * convert Kml objects into GeoJson objects
  *
  *
  * Kml object -> GeoJson object
  * Kml Folder -> GeoJson FeatureCollection
  * Kml Document -> GeoJson FeatureCollection
  * Kml MultiGeometry -> GeoJson GeometryCollection
  * Kml Placemark -> GeoJson Feature
  * Kml Point -> GeoJson Point
  * Kml LineString -> GeoJson LineString
  * Kml LinearRing -> GeoJson LineString
  * Kml Polygon -> GeoJson Polygon
  * Kml Feature (Placemark, Document, Folder) -> GeoJson object equivalent
  * Kml sequence of Feature -> GeoJson FeatureCollection
  *
  *
  * ref: https://github.com/workingDog/scalakml
  * ref: https://github.com/jroper/play-geojson
  */
class KmlConverter() {

  // implicit to convert a KML Coordinate to a LatLngAlt
  implicit class CoordinateToLalLngAlt(coord: Coordinate) {
    def toLatLngAlt(): LatLngAlt = {
      assert(coord.latitude.nonEmpty)
      assert(coord.longitude.nonEmpty)
      LatLngAlt(coord.latitude.get, coord.longitude.get, coord.altitude)
    }
  }

  /**
    * convert a Kml object into a list of GeoJson objects
    * @return a list GeoJson objects
    */
  def toGeoJson(kmlOpt: Option[Kml]): Option[List[GEOJS.GeoJson[LatLngAlt]]] = kmlOpt.map(kml => (for (f <- kml.feature) yield toGeoJson(f)).flatten.toList)

  /**
    * convert a Kml object into a list of GeoJson objects
    * @return a list GeoJson objects
    */
  def toGeoJson(kml: Kml): Option[List[GEOJS.GeoJson[LatLngAlt]]] = toGeoJson(Option(kml))

  /**
    * create a list of GeoJSON properties from a Kml FeaturePart
    * @param fp a Kml FeaturePart object
    * @return a list of (key,value) of properties
    */
  private def properties(fp: KML.FeaturePart): mutable.ListMap[String, JsValue] = {
    // the list of properties (key,value)
    val props = new mutable.ListMap[String, JsValue]()
    // the properties
    fp.name.map(x => props += "name" -> JsString(x))
    fp.description.map(x => props += "description" -> JsString(x))
    fp.address.map(x => props += "address" -> JsString(x))
    fp.phoneNumber.map(x => props += "phoneNumber" -> JsString(x))
    fp.styleUrl.map(x => props += "styleUrl" -> JsString(x))
    fp.visibility.map(x => props += "visibility" -> JsBoolean(x))
    fp.open.map(x => props += "open" -> JsBoolean(x))
    fp.timePrimitive match {
      case Some(timex) if timex.isInstanceOf[TimeStamp] =>
        props += "timeStamp" -> JsString(timex.asInstanceOf[TimeStamp].when.getOrElse(""))

      case Some(timex) if timex.isInstanceOf[TimeSpan] =>
        props += "timeBegin" -> JsString(timex.asInstanceOf[TimeSpan].begin.getOrElse(""))
        props += "timeEnd" -> JsString(timex.asInstanceOf[TimeSpan].end.getOrElse(""))

      case None => None
    }
    fp.extendedData.map(_.data.foreach(data => {
        data.displayName.map(d => props += "extended_displayName" -> JsString(d))
        data.name.map(d => props += "extended_name" -> JsString(d))
        data.value.map(d => props += "extended_value" -> JsString(d))
      }))

    // other properties from FeaturePart  todo
    props
  }

  /**
    * convert a Kml Feature into an equivalent GeoJson object
    * @param feature the input Kml feature
    * @return a GeoJson object representation of the Kml Feature
    */
  def toGeoJson(feature: KML.Feature): Option[GeoJson[LatLngAlt]] = {
    feature match {
      case f: Placemark => toGeoJson(f)
      case f: Document => toGeoJson(f)
      case f: Folder => toGeoJson(f)
      case f: PhotoOverlay => None // todo
      case f: ScreenOverlay => None // todo
      case f: GroundOverlay => None // todo
      // case f: Tour => None  // gx  todo
      case _ => None
    }
  }

  /**
    * convert a sequence of Kml Features into a GeoJson FeatureCollection
    * @param featureSet the set of Kml Features
    * @return a GeoJson FeatureCollection
    */
  def toGeoJson(featureSet: Seq[KML.Feature]): Option[GEOJS.GeoJson[LatLngAlt]] = {
    // this list may contain GEOJS.FeatureCollection which will need to be expanded into a list of GEOJS.Feature
    val geoList = for (f <- featureSet) yield toGeoJson(f)
    geoList.flatten.toList match {
      // don't process empty list
      case theList if theList.isEmpty => None
      case theList =>
        // to store the individual GEOJS.Feature
        val featureList = new MutableList[GEOJS.Feature[LatLngAlt]]()
        for (geoObj <- theList) {
          geoObj match {
            // expand any FeatureCollection into a list of GEOJS.Feature
            case f: GEOJS.FeatureCollection[LatLngAlt] => for (ft <- f.features) featureList += ft.asInstanceOf[GEOJS.Feature[LatLngAlt]]
            case f => featureList += f.asInstanceOf[GEOJS.Feature[LatLngAlt]]
          }
        }
        Option(GEOJS.FeatureCollection[LatLngAlt](featureList.toList.toSeq))
    }
  }

  /**
    * convert a Kml Folder to a GeoJson FeatureCollection object
    * @param folder the Kml input Folder object
    * @return a GeoJson FeatureCollection representation of the Kml Folder
    */
  def toGeoJson(folder: KML.Folder): Option[GEOJS.GeoJson[LatLngAlt]] = toGeoJson(folder.features.toList)

  /**
    * convert a Kml Document into a GeoJson FeatureCollection
    * @param doc the Kml Document object
    * @return a GeoJson FeatureCollection representation of the Kml Document
    */
  def toGeoJson(doc: KML.Document): Option[GEOJS.GeoJson[LatLngAlt]] = toGeoJson(doc.features.toList)

  /**
    * convert a Kml Placemark into a GeoJson Feature
    * @param placemark the Kml placemark object
    * @return a GeoJson Feature representation of the Kml Placemark
    */
  def toGeoJson(placemark: Placemark): Option[GEOJS.GeoJson[LatLngAlt]] = {
    val pid = placemark.id.flatMap(x => Option(Json.toJson(x)))
    val props = properties(placemark.featurePart)

    def addToProps(altMode: Option[KML.AltitudeMode], extrude: Option[Boolean]) = {
      altMode.map(x => props += "altitudeMode" -> JsString(x.toString))
      extrude.map(x => props += "extrude" -> JsBoolean(x))
    }

    val geojson: Option[GEOJS.GeoJson[LatLngAlt]] = placemark.geometry.flatMap( {
      case p: KML.Point => addToProps(p.altitudeMode, p.extrude); toGeoJson(p, bbox(placemark))
      case p: KML.LineString => addToProps(p.altitudeMode, p.extrude); toGeoJson(p, bbox(placemark))
      case p: KML.LinearRing => addToProps(p.altitudeMode, p.extrude); toGeoJson(p, bbox(placemark))
      case p: KML.Polygon => addToProps(p.altitudeMode, p.extrude); toGeoJson(p, bbox(placemark))
      case p: KML.MultiGeometry => toGeoJson(p, bbox(placemark))
      case p: KML.Model => None //  COLLADA todo
    })

    geojson.asInstanceOf[Option[GEOJS.Geometry[LatLngAlt]]].flatMap(p =>
      Option(GEOJS.Feature[LatLngAlt](p, properties = Option(JsObject(props)), id = pid)))

  }

  /**
    * create a bbox from the input Kml Placemark Region latLonAltBox
    * @param placemark the input Kml Placemark
    * @return a bounding box, i.e. a Tuple of (LatLngAlt,LatLngAlt)
    */
  private def bbox(placemark: Placemark): Option[(LatLngAlt, LatLngAlt)] = {
    // north Specifies the latitude of the north edge of the bounding box
    // south Specifies the latitude of the south edge of the bounding box
    // east Specifies the longitude of the east edge of the bounding box
    // west Specifies the longitude of the west edge of the bounding box
    val bbox = placemark.featurePart.region.map(reg => reg.latLonAltBox.map(llb => {
      assert(llb.north.nonEmpty)
      assert(llb.east.nonEmpty)
      assert(llb.south.nonEmpty)
      assert(llb.west.nonEmpty)
      (new LatLngAlt(llb.north.get, llb.east.get, llb.minAltitude), new LatLngAlt(llb.south.get, llb.west.get, llb.maxAltitude))
    }))

    bbox.flatten
  }

  /**
    * convert a Kml Point into a GeoJson Point object
    * @param p the Kml Point input
    * @param bbox the bounding box
    * @return a GeoJson Point object
    */
  def toGeoJson(p: KML.Point, bbox: Option[(LatLngAlt, LatLngAlt)]): Option[GEOJS.GeoJson[LatLngAlt]] = p.coordinates.flatMap(c => Option(GEOJS.Point(c.toLatLngAlt(), bbox)))

  /**
    * convert a Kml LineString into a GeoJson LineString object
    * @param ls the Kml LineString input
    * @param bbox the bounding box
    * @return a GeoJson LineString object
    */
  def toGeoJson(ls: KML.LineString, bbox: Option[(LatLngAlt, LatLngAlt)]): Option[GEOJS.GeoJson[LatLngAlt]] = toLineString(ls.coordinates, bbox)

  /**
    * convert a Kml LinearRing into a GeoJson LineString object
    * @param lr the Kml LinearRing input
    * @param bbox the bounding box
    * @return a GeoJson LineString object
    */
  def toGeoJson(lr: KML.LinearRing, bbox: Option[(LatLngAlt, LatLngAlt)]): Option[GEOJS.GeoJson[LatLngAlt]] = toLineString(lr.coordinates, bbox)

  /**
    * create a GeoJson LineString given the list of Kml Coordinates
    * @param coords the coordinate of the LineString
    * @param bbox the bounding box
    * @return a GeoJson LineString
    */
  private def toLineString(coords: Option[scala.Seq[Coordinate]], bbox: Option[(LatLngAlt, LatLngAlt)]): Option[GEOJS.GeoJson[LatLngAlt]] = {
    val laloList = for (loc <- coords.getOrElse(List.empty)) yield loc.toLatLngAlt()
    Option(GEOJS.LineString(laloList.toList.toSeq, bbox))
  }

  /**
    * convert a Kml Polygon into a GeoJson Polygon object
    * @param poly the Kml Polygon input
    * @param bbox the bounding box
    * @return a GeoJson Polygon object
    */
  def toGeoJson(poly: KML.Polygon, bbox: Option[(LatLngAlt, LatLngAlt)]): Option[GEOJS.GeoJson[LatLngAlt]] = {
    val locationList = new MutableList[scala.Seq[Coordinate]]()

    // first the outer boundary
    poly.outerBoundaryIs.foreach(
      boundary => boundary.linearRing.foreach(
        ring => locationList += ring.coordinates.getOrElse(List.empty)))

    // then the holes
    poly.innerBoundaryIs.foreach(
      boundary => boundary.linearRing.foreach(
        ring => ring.coordinates.foreach(c => locationList += c)))

    val laloList = for (loc <- locationList.flatten.toList) yield loc.toLatLngAlt()
    Option(GEOJS.Polygon(Seq(laloList), bbox))
  }

  /**
    * convert a Kml MultiGeometry into a GeoJson GeometryCollection object
    * @param multiGeom the Kml MultiGeometry input
    * @param bbox the bounding box
    * @return a GeoJson GeometryCollection object
    */
  def toGeoJson(multiGeom: KML.MultiGeometry, bbox: Option[(LatLngAlt, LatLngAlt)]): Option[GEOJS.GeoJson[LatLngAlt]] = {
    val seqGeom = multiGeom.geometries.flatMap({
      case p: KML.Point => Seq(toGeoJson(p, bbox))
      case p: KML.LineString => Seq(toGeoJson(p, bbox))
      case p: KML.LinearRing => Seq(toGeoJson(p, bbox))
      case p: KML.Polygon => Seq(toGeoJson(p, bbox))
      case p: KML.MultiGeometry => Seq(toGeoJson(p, bbox))
      case _ => None
    }).flatten.toList.asInstanceOf[Seq[GeometryCollection[LatLngAlt]]]
    Option(GEOJS.GeometryCollection(seqGeom))
  }

}