"""
This page is in the table of contents.
The xml.py script is an import translator plugin to get a carving from an Art of Illusion xml file.

An import plugin is a script in the interpret_plugins folder which has the function getCarving.  It is meant to be run from the interpret tool.  To ensure that the plugin works on platforms which do not handle file capitalization properly, give the plugin a lower case name.

The getCarving function takes the file name of an xml file and returns the carving.

This example gets a triangle mesh for the xml file boolean.xml.  This example is run in a terminal in the folder which contains boolean.xml and xml.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import xml
>>> xml.getCarving().getCarveRotatedBoundaryLayers()
[-1.159765625, None, [[(-18.925000000000001-2.4550000000000001j), (-18.754999999999981-2.4550000000000001j)
..
many more lines of the carving
..


An xml file can be exported from Art of Illusion by going to the "File" menu, then going into the "Export" menu item, then picking the XML choice.  This will bring up the XML file chooser window, choose a place to save the file then click "OK".  Leave the "compressFile" checkbox unchecked.  All the objects from the scene will be exported, this plugin will ignore the light and camera.  If you want to fabricate more than one object at a time, you can have multiple objects in the Art of Illusion scene and they will all be carved, then fabricated together.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.solids import boolean_solid
from fabmetheus_utilities.solids import cube
from fabmetheus_utilities.solids import cylinder
from fabmetheus_utilities.solids import sphere
from fabmetheus_utilities.solids import triangle_mesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import boolean_carving
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import solid

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def addCarvableObject( carvableObjects, xmlElement ):
	"Add the object info if it is carvable."
	carvableObject = getCarvableObject( xmlElement )
	if carvableObject == None:
		return
	carvableObject.setToObjectAttributeTable()
	carvableObjects.append( carvableObject )

def getCarvableObject( xmlElement ):
	"Get the object if it is carvable."
	if xmlElement == None:
		return
	object = xmlElement.getFirstChildWithClassName( 'object' )
	if 'bf:type' not in object.attributeTable:
		return
	shapeType = object.attributeTable[ 'bf:type' ]
	if shapeType not in globalCarvableClassObjectTable:
		return
	if xmlElement.attributeTable[ 'visible' ] == 'false':
		return
	carvableClassObject = globalCarvableClassObjectTable[ shapeType ]
	newCarvableObject = getNewCarvableObject( carvableClassObject, xmlElement )
	return newCarvableObject

def getCarvingFromParser( xmlParser ):
	"Get the carving for the parser."
	booleanGeometry = boolean_carving.BooleanGeometry()
	artOfIllusionElement = xmlParser.rootElement.getFirstChildWithClassName( 'ArtOfIllusion' )
	sceneElement = artOfIllusionElement.getFirstChildWithClassName( 'Scene' )
	rootElement = sceneElement.getFirstChildWithClassName( 'objects' )
	xmlElements = rootElement.getChildrenWithClassName( 'bf:Elem' )
	for xmlElement in xmlElements:
		addCarvableObject( booleanGeometry.carvableObjects, xmlElement )
	for carvableObject in booleanGeometry.carvableObjects:
		carvableObject.createShape( None )
	return booleanGeometry

def getNewCarvableObject( globalObject, xmlElement ):
	"Get new carvable object info."
	newCarvableObject = globalObject()
	newCarvableObject.name = xmlElement.getFirstChildWithClassName( 'name' ).text
	newCarvableObject.xmlElement = xmlElement.getFirstChildWithClassName( 'object' )
	coords = xmlElement.getFirstChildWithClassName( 'coords' )
	transformAttributeTable = getTransformAttributeTable( coords, 'transformFrom' )
	if len( transformAttributeTable ) < 16:
		transformAttributeTable = getTransformAttributeTable( coords, 'transformTo' )
	newCarvableObject.matrix4X4 = solid.Matrix4X4().getFromAttributeTable( transformAttributeTable )
	return newCarvableObject

def getTransformAttributeTable( coords, transformName ):
	"Get the transform attributes."
	transformAttributeTable = coords.getFirstChildWithClassName( transformName ).attributeTable
	if len( transformAttributeTable ) < 16:
		if 'bf:ref' in transformAttributeTable:
			idReference = transformAttributeTable[ 'bf:ref' ]
			return coords.rootElement.getSubChildWithID( idReference ).attributeTable
	return transformAttributeTable


class BooleanSolid( boolean_solid.BooleanSolid ):
	"An Art of Illusion CSG object info."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		functionTable = { '0': self.getUnion, '1': self.getIntersection, '2': self.getFirstMinusComplement, '3': self.getLastMinusComplement }
		self.operationFunction = functionTable[ self.xmlElement.attributeTable[ 'operation' ] ]
		self.subObjects = []
		addCarvableObject( self.subObjects, self.xmlElement.getFirstChildWithClassName( 'obj1' ) )
		addCarvableObject( self.subObjects, self.xmlElement.getFirstChildWithClassName( 'obj2' ) )


class Cube( cube.Cube ):
	"An Art of Illusion Cube object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		self.halfX = float( self.xmlElement.attributeTable[ 'halfx' ] )
		self.halfY = float( self.xmlElement.attributeTable[ 'halfy' ] )
		self.halfZ = float( self.xmlElement.attributeTable[ 'halfz' ] )


class Cylinder( cylinder.Cylinder ):
	"An Art of Illusion Cylinder object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		self.height = float( self.xmlElement.attributeTable[ 'height' ] )
		self.radiusX = float( self.xmlElement.attributeTable[ 'rx' ] )
		self.topOverBottom = float( self.xmlElement.attributeTable[ 'ratio' ] )
		self.radiusZ = float( self.xmlElement.attributeTable[ 'rz' ] )


class Sphere( sphere.Sphere ):
	"An Art of Illusion Sphere object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object."
		self.radiusX = float( self.xmlElement.attributeTable[ 'rx' ] )
		self.radiusY = float( self.xmlElement.attributeTable[ 'ry' ] )
		self.radiusZ = float( self.xmlElement.attributeTable[ 'rz' ] )


class TriangleMesh( triangle_mesh.TriangleMesh ):
	"An Art of Illusion triangle mesh object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		vertexElement = self.xmlElement.getFirstChildWithClassName( 'vertex' )
		vertexPointElements = vertexElement.getChildrenWithClassName( 'bf:Elem' )
		for vertexPointElement in vertexPointElements:
			coordinateElement = vertexPointElement.getFirstChildWithClassName( 'r' )
			vertex = Vector3( float( coordinateElement.attributeTable[ 'x' ] ), float( coordinateElement.attributeTable[ 'y' ] ), float( coordinateElement.attributeTable[ 'z' ] ) )
			self.vertices.append( vertex )
		edgeElement = self.xmlElement.getFirstChildWithClassName( 'edge' )
		edgeSubelements = edgeElement.getChildrenWithClassName( 'bf:Elem' )
		for edgeSubelementIndex in xrange( len( edgeSubelements ) ):
			edgeSubelement = edgeSubelements[ edgeSubelementIndex ]
			vertexIndexes = [ int( edgeSubelement.attributeTable[ 'v1' ] ), int( edgeSubelement.attributeTable[ 'v2' ] ) ]
			edge = triangle_mesh.Edge().getFromVertexIndexes( edgeSubelementIndex, vertexIndexes )
			self.edges.append( edge )
		faceElement = self.xmlElement.getFirstChildWithClassName( 'face' )
		faceSubelements = faceElement.getChildrenWithClassName( 'bf:Elem' )
		for faceSubelementIndex in xrange( len( faceSubelements ) ):
			faceSubelement = faceSubelements[ faceSubelementIndex ]
			edgeIndexes = [ int( faceSubelement.attributeTable[ 'e1' ] ), int( faceSubelement.attributeTable[ 'e2' ] ), int( faceSubelement.attributeTable[ 'e3' ] ) ]
			face = triangle_mesh.Face().getFromEdgeIndexes( edgeIndexes, self.edges, faceSubelementIndex )
			self.faces.append( face )


globalCarvableClassObjectTable = { 'CSGObject' : BooleanSolid, 'Cube' : Cube, 'Cylinder' : Cylinder, 'Sphere' : Sphere, 'TriangleMesh' : TriangleMesh }
