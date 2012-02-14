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
	lowerClassName = xmlElement.className.lower()
	if lowerClassName not in globalCarvableClassObjectTable:
		return
	if 'visible' in xmlElement.attributeTable:
		if xmlElement.attributeTable[ 'visible' ] == 'false':
			return
	carvableObject = globalCarvableClassObjectTable[ lowerClassName ]()
	if 'id' in xmlElement.attributeTable:
		carvableObject.name = xmlElement.attributeTable[ 'id' ]
	carvableObject.xmlElement = xmlElement
	carvableObject.matrix4X4 = solid.Matrix4X4()
	matrixElement = xmlElement.getFirstChildWithClassName( 'matrix4x4' )
	if matrixElement != None:
		carvableObject.matrix4X4.getFromAttributeTable( matrixElement.attributeTable )
	return carvableObject

def getCarvingFromParser( xmlParser ):
	"Get the carving for the parser."
	booleanGeometry = boolean_carving.BooleanGeometry()
	booleanGeometryElement = xmlParser.rootElement.getFirstChildWithClassName( 'booleangeometry' )
	xmlElements = booleanGeometryElement.children
	for xmlElement in xmlElements:
		addCarvableObject( booleanGeometry.carvableObjects, xmlElement )
	for carvableObject in booleanGeometry.carvableObjects:
		carvableObject.createShape( None )
	return booleanGeometry


class BooleanSolid( boolean_solid.BooleanSolid ):
	"An Art of Illusion CSG object info."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		functionTable = { 'union': self.getUnion, 'intersection': self.getIntersection, 'firstminuscomplement': self.getFirstMinusComplement, 'lastminuscomplement': self.getLastMinusComplement }
		self.operationFunction = functionTable[ self.xmlElement.attributeTable[ 'operation' ] ]
		self.subObjects = []
		for xmlElement in self.xmlElement.children:
			addCarvableObject( self.subObjects, xmlElement )


class Cube( cube.Cube ):
	"An Art of Illusion Cube object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		self.halfX = euclidean.getFloatOneFromDictionary( 'halfx', self.xmlElement.attributeTable )
		self.halfY = euclidean.getFloatOneFromDictionary( 'halfy', self.xmlElement.attributeTable )
		self.halfZ = euclidean.getFloatOneFromDictionary( 'halfz', self.xmlElement.attributeTable )


class Cylinder( cylinder.Cylinder ):
	"An Art of Illusion Cylinder object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		self.height = euclidean.getFloatOneFromDictionary( 'height', self.xmlElement.attributeTable )
		self.radiusX = euclidean.getFloatOneFromDictionary( 'radiusx', self.xmlElement.attributeTable )
		self.topOverBottom = euclidean.getFloatOneFromDictionary( 'topoverbottom', self.xmlElement.attributeTable )
		self.radiusY = euclidean.getFloatOneFromDictionary( 'radiusy', self.xmlElement.attributeTable )
		self.radiusZ = euclidean.getFloatOneFromDictionary( 'radiusz', self.xmlElement.attributeTable )
		if 'radiusy' in self.xmlElement.attributeTable:
			self.isYImaginaryAxis = True


class Sphere( sphere.Sphere ):
	"An Art of Illusion Sphere object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		self.radiusX = euclidean.getFloatOneFromDictionary( 'radiusx', self.xmlElement.attributeTable )
		self.radiusY = euclidean.getFloatOneFromDictionary( 'radiusy', self.xmlElement.attributeTable )
		self.radiusZ = euclidean.getFloatOneFromDictionary( 'radiusz', self.xmlElement.attributeTable )


class TriangleMesh( triangle_mesh.TriangleMesh ):
	"An Art of Illusion triangle mesh object."
	def setToObjectAttributeTable( self ):
		"Set the shape of this carvable object info."
		verticesElement = self.xmlElement.getFirstChildWithClassName( 'vertices' )
		vector3Elements = verticesElement.getChildrenWithClassName( 'vector3' )
		for vector3Element in vector3Elements:
			vector3Table = vector3Element.attributeTable
			vertex = Vector3( euclidean.getFloatZeroFromDictionary( 'x', vector3Table ), euclidean.getFloatZeroFromDictionary( 'y', vector3Table ), euclidean.getFloatZeroFromDictionary( 'z', vector3Table ) )
			self.vertices.append( vertex )
		facesElement = self.xmlElement.getFirstChildWithClassName( 'faces' )
		faceElements = facesElement.getChildrenWithClassName( 'face' )
		for faceIndex in xrange( len( faceElements ) ):
			face = triangle_mesh.Face()
			faceElement = faceElements[ faceIndex ]
			face.index = faceIndex
			for vertexIndexIndex in xrange( 3 ):
				face.vertexIndexes.append( int( faceElement.attributeTable[ 'vertex' + str( vertexIndexIndex ) ] ) )
			self.faces.append( face )


globalCarvableClassObjectTable = { 'booleansolid' : BooleanSolid, 'cube' : Cube, 'cylinder' : Cylinder, 'sphere' : Sphere, 'trianglemesh' : TriangleMesh }
