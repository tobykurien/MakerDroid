"""
Triangle Mesh holds the faces and edges of a triangular mesh.

It can read from and write to a GNU Triangulated Surface (.gts) file.

The following examples carve the GNU Triangulated Surface file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and triangle_mesh.py.


>python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import carve
>>> carve.main()
File Screw Holder Bottom.stl is being carved.
The carved file is saved as Screw Holder Bottom_carve.gcode
It took 3 seconds to carve the file.


>>> carve.writeOutput( 'Screw Holder Bottom.stl' )
File Screw Holder Bottom.gcode is being carved.
The carved file is saved as Screw Holder Bottom_carve.gcode
It took 3 seconds to carve the file.


>>> carve.getGcode("
54 162 108 Number of Vertices,Number of Edges,Number of Faces
-5.800000000000001 5.341893939393939 4.017841892579603 Vertex Coordinates XYZ
5.800000000000001 5.341893939393939 4.017841892579603
..
many lines of GNU Triangulated Surface vertices, edges and faces
..
")

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import xml_writer
import cStringIO


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = "GPL 3.0"


def getBeginGeometryXMLOutput():
	"Get the string representation of this object info."
	output = cStringIO.StringIO()
	output.write( "<?xml version='1.0' ?>\n" )
	xml_writer.addBeginXMLTag( { 'version' : '2010-03-17' }, 0, 'booleangeometry', output )
	return output

def getEndGeometryXMLString( output ):
	"Get the string representation of this object info."
	xml_writer.addEndXMLTag( 0, 'booleangeometry', output )
	return output.getvalue()

def getMatrixKey( column, row ):
	"Get the key string from row & column, counting from one."
	return 'm' + str( column + 1 ) + str( row + 1 )

def getTransformedByList( floatList, point ):
	"Get the point transformed by the array."
	return floatList[ 0 ] * point.x + floatList[ 1 ] * point.y + floatList[ 2 ] * point.z + floatList[ 3 ]

def getVector3TransformedByMatrix( matrix, vector3 ):
	"Get the vector3 multiplied by a matrix."
	vector3Transformed = Vector3()
	vector3Transformed.x = getTransformedByList( matrix[ 0 ], vector3 )
	vector3Transformed.y = getTransformedByList( matrix[ 1 ], vector3 )
	vector3Transformed.z = getTransformedByList( matrix[ 2 ], vector3 )
	return vector3Transformed

def transformVector3ByMatrix( matrix, vector3 ):
	"Transform the vector3 by a matrix."
	vector3.setToVector3( getVector3TransformedByMatrix( matrix, vector3 ) )

class Matrix4X4:
	"A four by four matrix."
	def __init__( self ):
		"Add empty lists."
		self.setMatrixToIdentity()

	def __repr__( self ):
		"Get the string representation of this four by four matrix."
		output = cStringIO.StringIO()
		self.addXML( 0, output )
		return output.getvalue()

	def addXML( self, depth, output ):
		"Add xml for this object."
		attributeTable = self.getAttributeTable()
		if len( attributeTable ) > 0:
			xml_writer.addClosedXMLTag( attributeTable, depth, self.__class__.__name__.lower(), output )

	def getAttributeTable( self ):
		"Get the from row column attribute strings, counting from one."
		attributeTable = {}
		for column in xrange( 4 ):
			for row in xrange( 4 ):
				default = float( column == row )
				value = self.matrix[ column ][ row ]
				if abs( value - default ) > 0.00000000000001:
					if abs( value ) < 0.00000000000001:
						value = 0.0
					attributeTable[ getMatrixKey( column, row ) ] = value
		return attributeTable

	def getFromAttributeTable( self, attributeTable ):
		"Get the values from row column attribute strings, counting from one."
		if attributeTable == None:
			return self
		for column in xrange( 4 ):
			for row in xrange( 4 ):
				key = getMatrixKey( column, row )
				if key in attributeTable:
					self.matrix[ column ][ row ] = float( attributeTable[ key ] )
		return self

	def getMultiplied( self, otherMatrix ):
		"Get this matrix multiplied by the other matrix."
		if otherMatrix == None:
			return self.matrix
		if self.matrix == None:
			return None
		#A down, B right from http://en.wikipedia.org/wiki/Matrix_multiplication
		newMatrix4X4 = Matrix4X4()
		newMatrix4X4.setMatrixToZero()
		for column in xrange( 4 ):
			for row in xrange( 4 ):
				matrixColumn = self.matrix[ column ]
				dotProduct = 0
				for elementIndex in xrange( 4 ):
					dotProduct += matrixColumn[ elementIndex ] * otherMatrix[ elementIndex ][ row ]
				newMatrix4X4.matrix[ column ][ row ] = dotProduct
		return newMatrix4X4

	def getReverseMultiplied( self, otherMatrix4X4 ):
		"Get this matrix reverse multiplied by the other matrix."
		if otherMatrix4X4 == None:
			return self
		return otherMatrix4X4.getMultiplied( self.matrix )

	def setMatrixToIdentity( self ):
		"Set the diagonal matrix elements to one."
		self.setMatrixToZero()
		for diagonal in xrange( 4 ):
			self.matrix[ diagonal ][ diagonal ] = 1.0

	def setMatrixToZero( self ):
		"Set the matrix elements to zero."
		self.matrix = [ [ 0.0, 0.0, 0.0, 0.0 ], [ 0.0, 0.0, 0.0, 0.0 ], [ 0.0, 0.0, 0.0, 0.0 ], [ 0.0, 0.0, 0.0, 0.0 ] ]


class Solid:
	"A solid."
	def __init__( self ):
		"Add empty lists."
		self.matrix4X4 = None
		self.name = None

	def __repr__( self ):
		"Get the string representation of this object info."
		output = getBeginGeometryXMLOutput()
		self.addXML( 1, output )
		return getEndGeometryXMLString( output )

	def addXML( self, depth, output ):
		"Add xml for this object."
		attributeTable = self.getAttributeTable()
		attributeTable[ 'id' ] = self.getName()
		xml_writer.addBeginXMLTag( attributeTable, depth, self.getXMLClassName(), output )
		if self.matrix4X4 != None:
			self.matrix4X4.addXML( depth + 1, output )
		self.addXMLSection( depth + 1, output )
		xml_writer.addEndXMLTag( depth, self.getXMLClassName(), output )

	def getAttributeTable( self ):
		"Get attribute table."
		return {}

	def getName( self ):
		"Get the name if it exists, otherwise return the class name."
		if self.name == None:
			return self.__class__.__name__
		return self.name

	def getType( self ):
		"Get type."
		return self.__class__.__name__

	def getXMLClassName( self ):
		"Get xml class name."
		return self.__class__.__name__.lower()
