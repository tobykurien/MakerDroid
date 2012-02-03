"""
This page is in the table of contents.
Circular wave is an example of a skeinforge python procedural script to make a carving.  This script will be imported by the py.py import plugin.

The required function for a skeinforge python procedural script is 'def getLoopLayers( layerThickness ):'.  This function must return a LoopLayer list.  The LoopLayer z values in the list must increase by the layer thickness.  The required part of the LoopLayer class is reprinted below.

class LoopLayer:
 "Loops with a z."
 def __init__( self, z ):
  self.loops = []
  self.z = z

The skeinforge procedural script is similar in concept, although different in execution, to the Masked Retriever's parametric scripts at:
http://blog.thingiverse.com/2009/10/20/parametric-objects-again/
http://blog.thingiverse.com/2009/10/19/parametric-object-party-day-1-the-power-of-standard-custom/

The circular wave root parameters are set in the setRootParameters function, which is reprinted below.

def setRootParameters( self ):
 "Set the root parameters."
 self.height = 10.0
 self.innerRadiusRatio = 0.5
 self.numberOfPoints = 40
 self.numberOfWaves = 3
 self.radius = 10.0
 self.twist = math.radians( 30.0 )

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

import math

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getLoopLayers( layerThickness ):
	"Get the loop layers."
	return CircularWave( layerThickness ).getLoopLayers()


class LoopLayer:
	"Loops with a z."
	def __init__( self, z ):
		self.loops = []
		self.z = z

	def __repr__( self ):
		"Get the string representation of this loop layer."
		return '%s, %s' % ( self.z, self.loops )


class CircularWave:
	"A twisted circular wave."
	def __init__( self, layerThickness ):
		"Initialize."
		self.layerThickness = layerThickness
		self.loopLayers = []
		self.setRootParameters()
		self.setDerivedParameters()
		for layerIndex in xrange( self.numberOfLayers ):
			self.addLoopLayer( layerIndex )
	
	def __repr__( self ):
		"Get the string representation of this carving."
		return '%s, %s, %s, %s' % ( self.layerThickness, self.minimumZ, self.maximumZ, self.rotatedBoundaryLayers )

	def addLoopLayer( self, layerIndex ):
		"Add a loop layer."
		z = self.halfLayerThickness + layerIndex * self.layerThickness
		loopLayer = LoopLayer( z )
		loop = []
		for pointIndex in xrange( self.numberOfPoints ):
			twist = self.twist * z / self.height
			self.addPoint( loop, pointIndex, twist )
		loopLayer.loops.append( loop )
		self.loopLayers.append( loopLayer )

	def addPoint( self, loop, pointIndex, twist ):
		"Add a point."
		rotation = - self.rotationIncrement * pointIndex
		waveRotation = rotation * float( self.numberOfWaves )
		radius = self.midRadius + math.sin( waveRotation ) * self.halfRingWidth
		twistedRotation = rotation + twist
		point = complex( math.cos( twistedRotation ), - math.sin( twistedRotation ) ) * radius
		loop.append( point )

	def getLoopLayers( self ):
		"Get the loop layers."
		return self.loopLayers

	def setDerivedParameters( self ):
		"Set the derived parameters."
		self.halfLayerThickness = 0.5 * self.layerThickness
		self.innerRadius = self.innerRadiusRatio * self.radius
		self.midRadius = 0.5 * ( self.innerRadius + self.radius )
		self.halfRingWidth = self.radius - self.midRadius
		self.numberOfLayers = max( 1, int( round( self.height / self.layerThickness ) ) )
		self.rotationIncrement = 2.0 * math.pi / float( self.numberOfPoints )

	def setRootParameters( self ):
		"Set the root parameters."
		self.height = 10.0
		self.innerRadiusRatio = 0.5
		self.numberOfPoints = 40
		self.numberOfWaves = 3
		self.radius = 10.0
		self.twist = math.radians( 30.0 )
