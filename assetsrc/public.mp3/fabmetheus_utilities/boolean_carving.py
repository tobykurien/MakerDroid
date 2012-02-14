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

from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import solid
from fabmetheus_utilities.solids import triangle_mesh

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


class BooleanGeometry:
	"A shape scene."
	def __init__( self ):
		"Add empty lists."
		self.belowLoops = []
		self.bridgeLayerThickness = None
		self.carvableObjects = []
		self.importRadius = 0.3
		self.layerThickness = 0.4
		self.rotatedBoundaryLayers = []

	def __repr__( self ):
		"Get the string representation of this carving."
		output = solid.getBeginGeometryXMLOutput()
		for carvableObject in self.carvableObjects:
			carvableObject.addXML( 1, output )
		return solid.getEndGeometryXMLString( output )

	def getCarveCornerMaximum( self ):
		"Get the corner maximum of the vertices."
		return self.cornerMaximum

	def getCarveCornerMinimum( self ):
		"Get the corner minimum of the vertices."
		return self.cornerMinimum

	def getCarveLayerThickness( self ):
		"Get the layer thickness."
		return self.layerThickness

	def getCarveRotatedBoundaryLayers( self ):
		"Get the rotated boundary layers."
		if len( self.carvableObjects ) < 1:
			return []
		self.cornerMaximum = Vector3( - 999999999.0, - 999999999.0, - 9999999999.9 )
		self.cornerMinimum = Vector3( 999999999.0, 999999999.0, 9999999999.9 )
		for carvableObject in self.carvableObjects:
			self.cornerMaximum.z = max( self.cornerMaximum.z, carvableObject.top )
			self.cornerMinimum.z = min( self.cornerMinimum.z, carvableObject.bottom )
		halfHeight = 0.5 * self.layerThickness
		layerTop = self.cornerMaximum.z - halfHeight
		self.setActualMinimumZ( halfHeight, layerTop )
		vertices = []
		for carvableObject in self.carvableObjects:
			vertices += carvableObject.getVertices()
		triangle_mesh.initializeZoneIntervalTable( self, vertices )
		z = self.cornerMinimum.z + halfHeight
		while z < layerTop:
			z = self.getZAddExtruderPaths( z )
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			for loop in rotatedBoundaryLayer.loops:
				for point in loop:
					pointVector3 = Vector3( point.real, point.imag, rotatedBoundaryLayer.z )
					self.cornerMaximum = euclidean.getPointMaximum( self.cornerMaximum, pointVector3 )
					self.cornerMinimum = euclidean.getPointMinimum( self.cornerMinimum, pointVector3 )
		self.cornerMaximum.z = layerTop + halfHeight
		for rotatedBoundaryLayerIndex in xrange( len( self.rotatedBoundaryLayers ) - 1, - 1, - 1 ):
			rotatedBoundaryLayer = self.rotatedBoundaryLayers[ rotatedBoundaryLayerIndex ]
			if len( rotatedBoundaryLayer.loops ) > 0:
				return self.rotatedBoundaryLayers[ : rotatedBoundaryLayerIndex + 1 ]
		return []

	def getExtruderPaths( self, z ):
		"Get extruder loops."
		rotatedBoundaryLayer = euclidean.RotatedLoopLayer( z )
		for carvableObject in self.carvableObjects:
			rotatedBoundaryLayer.loops += carvableObject.getLoops( self.importRadius, z )
		return rotatedBoundaryLayer

	def getInterpretationSuffix( self ):
		"Return the suffix for a boolean carving."
		return 'xml'

	def getZAddExtruderPaths( self, z ):
		"Get next z and add extruder loops."
		rotatedBoundaryLayer = self.getExtruderPaths( triangle_mesh.getEmptyZ( self, z ) )
		self.rotatedBoundaryLayers.append( rotatedBoundaryLayer )
		if self.bridgeLayerThickness == None:
			return z + self.layerThickness
		allExtrudateLoops = []
		for loop in rotatedBoundaryLayer.loops:
			allExtrudateLoops += triangle_mesh.getBridgeLoops( self.layerThickness, loop )
		rotatedBoundaryLayer.rotation = triangle_mesh.getBridgeDirection( self.belowLoops, allExtrudateLoops, self.layerThickness )
		self.belowLoops = allExtrudateLoops
		if rotatedBoundaryLayer.rotation == None:
			return z + self.layerThickness
		return z + self.bridgeLayerThickness

	def setActualMinimumZ( self, halfHeight, layerTop ):
		"Get the actual minimum z at the lowest rotated boundary layer."
		while self.cornerMinimum.z < layerTop:
			if len( self.getExtruderPaths( self.cornerMinimum.z ).loops ) > 0:
				increment = - halfHeight
				while abs( increment ) > 0.001 * halfHeight:
					self.cornerMinimum.z += increment
					increment = 0.5 * abs( increment )
					if len( self.getExtruderPaths( self.cornerMinimum.z ).loops ) > 0:
						increment = - increment
				return
			self.cornerMinimum.z += self.layerThickness

	def setCarveBridgeLayerThickness( self, bridgeLayerThickness ):
		"Set the bridge layer thickness.  If the infill is not in the direction of the bridge, the bridge layer thickness should be given as None or not set at all."
		self.bridgeLayerThickness = bridgeLayerThickness

	def setCarveLayerThickness( self, layerThickness ):
		"Set the layer thickness."
		self.layerThickness = layerThickness

	def setCarveImportRadius( self, importRadius ):
		"Set the import radius."
		self.importRadius = importRadius

	def setCarveIsCorrectMesh( self, isCorrectMesh ):
		"Set the is correct mesh flag."
		self.isCorrectMesh = isCorrectMesh
