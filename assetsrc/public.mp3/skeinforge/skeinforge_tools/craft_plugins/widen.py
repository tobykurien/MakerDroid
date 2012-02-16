#! /usr/bin/env python
"""
This page is in the table of contents.
Widen will widen the outside perimeters away from the inside perimeters, so that the outsides will be at least two perimeter widths away from the insides and therefore the outside filaments will not overlap the inside filaments.

For example, if a mug has a very thin wall, widen would widen the outside of the mug so that the wall of the mug would be two perimeter widths wide, and the outside wall filament would not overlap the inside filament.

For another example, if the outside of the object runs right next to a hole, widen would widen the wall around the hole so that the wall would bulge out around the hole, and the outside filament would not overlap the hole filament.

The widen manual page is at:
http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Widen

==Operation==
The default 'Activate Widen' checkbox is off.  When it is on, widen will work, when it is off, widen will not be called.

==Examples==
The following examples widen the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and widen.py.

> python widen.py
This brings up the widen dialog.


> python widen.py Screw Holder Bottom.stl
The widen tool is parsing the file:
Screw Holder Bottom.stl
..
The widen tool has created the file:
.. Screw Holder Bottom_widen.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import widen
>>> widen.main()
This brings up the widen dialog.


>>> widen.writeOutput( 'Screw Holder Bottom.stl' )
The widen tool is parsing the file:
Screw Holder Bottom.stl
..
The widen tool has created the file:
.. Screw Holder Bottom_widen.gcode

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import intercircle
from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import settings
from fabmetheus_utilities.solids import triangle_mesh
from skeinforge.skeinforge_utilities import skeinforge_craft
from skeinforge.skeinforge_utilities import skeinforge_polyfile
from skeinforge.skeinforge_utilities import skeinforge_profile
import os
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/28/04 $"
__license__ = "GPL 3.0"


def addToPaths( paths, radius, segment ):
	"Add the segment to the paths."
	begin = segment[ 0 ].point
	end = segment[ 1 ].point
	for path in paths:
		if abs( begin - path[ - 1 ] ) < radius:
			path.append( end )
			return
	paths.append( [ begin, end ] )

def connectEndPath( endIndex, paths, radius ):
	"Connect the end path."
	endPath = paths[ endIndex ]
	begin = endPath[ 0 ]
	end = endPath[ - 1 ]
	for pathIndex in xrange( 0, endIndex ):
		path = paths[ pathIndex ]
		if abs( begin - path[ - 1 ] ) < radius:
			path += endPath[ 1 : ]
			del paths[ endIndex ]
			return
		if abs( end - path[ 0 ] ) < radius:
			paths[ pathIndex ] = endPath + path[ 1 : ]
			del paths[ endIndex ]
			return

def getCraftedText( fileName, text = '', repository = None ):
	"Widen the preface file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), repository )

def getCraftedTextFromText( gcodeText, repository = None ):
	"Widen the preface gcode text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'widen' ):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository( WidenRepository() )
	if not repository.activateWiden.value:
		return gcodeText
	return WidenSkein().getCraftedGcode( gcodeText, repository )

def getIntersectingWithinLoops( loop, loopList, outsetLoop ):
	"Get the loops which are intersecting or which it is within."
	intersectingWithinLoops = []
	for otherLoop in loopList:
		if getIsIntersectingWithinLoop( loop, otherLoop, outsetLoop ):
			intersectingWithinLoops.append( otherLoop )
	return intersectingWithinLoops

def getIsIntersectingWithinLoop( loop, otherLoop, outsetLoop ):
	"Determine if the loop is intersecting or is within the other loop."
	if euclidean.isLoopIntersectingLoop( loop, otherLoop ):
		return True
	return euclidean.isPathInsideLoop( otherLoop, loop ) != euclidean.isPathInsideLoop( otherLoop, outsetLoop )

def getNewRepository():
	"Get the repository constructor."
	return WidenRepository()

def getSegmentsFromPoints( loops, pointBegin, pointEnd ):
	"Get endpoint segments from the beginning and end of a line segment."
	normalizedSegment = pointEnd - pointBegin
	normalizedSegmentLength = abs( normalizedSegment )
	if normalizedSegmentLength == 0.0:
		return []
	normalizedSegment /= normalizedSegmentLength
	segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
	pointBeginRotated = segmentYMirror * pointBegin
	pointEndRotated = segmentYMirror * pointEnd
	xIntersectionIndexList = []
	xIntersectionIndexList.append( euclidean.XIntersectionIndex( - 1, pointBeginRotated.real ) )
	xIntersectionIndexList.append( euclidean.XIntersectionIndex( - 1, pointEndRotated.real ) )
	for loopIndex in xrange( len( loops ) ):
		rotatedLoop = euclidean.getPointsRoundZAxis( segmentYMirror, loops[ loopIndex ] )
		euclidean.addXIntersectionIndexesFromLoopY( rotatedLoop, loopIndex, xIntersectionIndexList, pointBeginRotated.imag )
	segments = euclidean.getSegmentsFromXIntersectionIndexes( xIntersectionIndexList, pointBeginRotated.imag )
	for segment in segments:
		for endpoint in segment:
			endpoint.point *= normalizedSegment
	return segments

def getWidenedLoop( loop, loopList, outsetLoop, radius, tinyRadius ):
	"Get the widened loop."
	intersectingWithinLoops = getIntersectingWithinLoops( loop, loopList, outsetLoop )
	if len( intersectingWithinLoops ) < 1:
		return loop
	widdershinsLoops = [ loop ]
	for intersectingWithinLoop in intersectingWithinLoops:
		reversedLoop = intersectingWithinLoop[ : ]
		reversedLoop.reverse()
		widdershinsLoops.append( reversedLoop )
	paths = []
	for widdershinsLoopIndex in xrange( len( widdershinsLoops ) ):
		widdershinsLoop = widdershinsLoops[ widdershinsLoopIndex ]
		for pointIndex in xrange( len( widdershinsLoop ) ):
			pointBegin = widdershinsLoop[ pointIndex ]
			pointEnd = widdershinsLoop[ ( pointIndex + 1 ) % len( widdershinsLoop ) ]
			otherLoops = widdershinsLoops[ : widdershinsLoopIndex ] + widdershinsLoops[ widdershinsLoopIndex + 1 : ]
			segments = getSegmentsFromPoints( otherLoops, pointBegin, pointEnd )
			for segment in segments:
				addToPaths( paths, tinyRadius, segment )
	for endIndex in xrange( len( paths ) - 1, 0, - 1 ):
		connectEndPath( endIndex, paths, tinyRadius )
	widenedLoop = []
	for path in paths:
		widenedLoop += path
	if abs( widenedLoop[ 0 ] - widenedLoop[ - 1 ] ) < tinyRadius:
		widenedLoop = widenedLoop[ 1 : ]
	return euclidean.getSimplifiedLoop( widenedLoop, radius )

def writeOutput( fileName = '' ):
	"Widen the carving of a gcode file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		skeinforge_craft.writeChainTextWithNounMessage( fileName, 'widen' )


class WidenRepository:
	"A class to handle the widen settings."
	def __init__( self ):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository( 'skeinforge.skeinforge_tools.craft_plugins.widen.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Widen', self, '' )
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute( 'http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Widen' )
		self.activateWiden = settings.BooleanSetting().getFromValue( 'Activate Widen:', self, False )
		self.executeTitle = 'Widen'

	def execute( self ):
		"Widen button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class WidenSkein:
	"A class to widen a skein of extrusions."
	def __init__( self ):
		self.boundary = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.lineIndex = 0
		self.rotatedBoundaryLayer = None

	def addWiden( self, rotatedBoundaryLayer ):
		"Add widen to the layer."
		loops = triangle_mesh.getLoopsInOrderOfArea( triangle_mesh.compareAreaAscending, rotatedBoundaryLayer.loops )
		widdershinsLoops = []
		clockwiseInsetLoops = []
		for loopIndex in xrange( len( loops ) ):
			loop = loops[ loopIndex ]
			if euclidean.isWiddershins( loop ):
				otherLoops = loops[ : loopIndex ] + loops[ loopIndex + 1 : ]
				leftPoint = euclidean.getLeftPoint( loop )
				if euclidean.isPointInsideLoops( otherLoops, leftPoint ):
					self.distanceFeedRate.addGcodeFromLoop( loop, rotatedBoundaryLayer.z )
				else:
					widdershinsLoops.append( loop )
			else:
				clockwiseInsetLoops += intercircle.getInsetLoopsFromLoop( self.doublePerimeterWidth, loop )
				self.distanceFeedRate.addGcodeFromLoop( loop, rotatedBoundaryLayer.z )
		for widdershinsLoop in widdershinsLoops:
			outsetLoop = intercircle.getLargestInsetLoopFromLoop( widdershinsLoop, - self.doublePerimeterWidth )
			widenedLoop = getWidenedLoop( widdershinsLoop, clockwiseInsetLoops, outsetLoop, self.perimeterWidth, self.tinyRadius )
			self.distanceFeedRate.addGcodeFromLoop( widenedLoop, rotatedBoundaryLayer.z )

	def getCraftedGcode( self, gcodeText, repository ):
		"Parse gcode text and store the widen gcode."
		self.repository = repository
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for line in self.lines[ self.lineIndex : ]:
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon( line )
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedLine( 'procedureDone', 'widen' )
			elif firstWord == '(<extrusion>)':
				self.distanceFeedRate.addLine( line )
				return
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float( splitLine[ 1 ] )
				self.doublePerimeterWidth = 2.0 * self.perimeterWidth
				self.tinyRadius = 0.1 * self.perimeterWidth
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the widen skein."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon( line )
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '(<boundaryPoint>':
			location = gcodec.getLocationFromSplitLine( None, splitLine )
			self.boundary.append( location.dropAxis( 2 ) )
		elif firstWord == '(<layer>':
			self.rotatedBoundaryLayer = euclidean.RotatedLoopLayer( float( splitLine[ 1 ] ) )
			self.distanceFeedRate.addLine( line )
		elif firstWord == '(</layer>)':
			self.addWiden( self.rotatedBoundaryLayer )
			self.rotatedBoundaryLayer = None
		elif firstWord == '(<surroundingLoop>)':
			self.boundary = []
			self.rotatedBoundaryLayer.loops.append( self.boundary )
		if self.rotatedBoundaryLayer == None:
			self.distanceFeedRate.addLine( line )


def main():
	"Display the widen dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
