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

from fabmetheus_utilities.solids import cube
from fabmetheus_utilities.solids import triangle_mesh

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


class Sphere( cube.Cube ):
	"A sphere object."
	def createShape( self, matrixChain ):
		"Create the shape."
		self.numberOfInBetweens = 12
		self.numberOfDivisions = self.numberOfInBetweens + 1
		bottomLeft = complex( - 1.0, - 1.0 )
		topRight = complex( 1.0, 1.0 )
		extent = topRight - bottomLeft
		elementExtent = extent / self.numberOfDivisions
		grid = []
		for rowIndex in xrange( self.numberOfDivisions + 1 ):
			row = []
			grid.append( row )
			for columnIndex in xrange( self.numberOfDivisions + 1 ):
				point = complex( elementExtent.real * float( columnIndex ), elementExtent.real * float( rowIndex ) ) + bottomLeft
				row.append( point )
		indexedGridBottom = triangle_mesh.getAddIndexedGrid( grid, self.vertices, - 1.0 )
		indexedGridBottomLoop = triangle_mesh.getIndexedLoopFromIndexedGrid( indexedGridBottom )
		indexedLoops = [ indexedGridBottomLoop ]
		zList = []
		for zIndex in xrange( 1, self.numberOfDivisions ):
			z = elementExtent.real * float( zIndex ) + bottomLeft.real
			zList.append( z )
		gridLoop = []
		for vertex in indexedGridBottomLoop:
			gridLoop.append( vertex.dropAxis( 2 ) )
		indexedLoops += triangle_mesh.getAddIndexedLoops( gridLoop, self.vertices, zList )
		indexedGridTop = triangle_mesh.getAddIndexedGrid( grid, self.vertices, 1.0 )
		indexedLoops.append( triangle_mesh.getIndexedLoopFromIndexedGrid( indexedGridTop ) )
		triangle_mesh.addPillarFromConvexLoopsGrids( self.faces, [ indexedGridBottom, indexedGridTop ], indexedLoops )
		for vertex in self.vertices:
			vertex.normalize()
			vertex.x *= self.radiusX
			vertex.y *= self.radiusY
			vertex.z *= self.radiusZ
		self.transformSetBottomTopEdges( matrixChain )

	def getAttributeTable( self ):
		"Get attribute table."
		return { 'radiusx': self.radiusX, 'radiusy': self.radiusY, 'radiusz': self.radiusZ }

