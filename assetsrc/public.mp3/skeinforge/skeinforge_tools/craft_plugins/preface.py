#! /usr/bin/env python
"""
This page is in the table of contents.
Preface converts the svg slices into gcode extrusion layers, optionally prefaced with some gcode commands.

The preface manual page is at:
http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Preface

==Settings==
===Meta===
Default is empty.

The 'Meta' field is to add meta tags or a note to all your files.  Whatever is in that field will be added in a meta tagged line to the output.

===Name of Alteration Files===
Preface looks for alteration files in the alterations folder in the .skeinforge folder in the home directory.  Preface does not care if the text file names are capitalized, but some file systems do not handle file name cases properly, so to be on the safe side you should give them lower case names.  If it doesn't find the file it then looks in the alterations folder in the skeinforge_tools folder. If it doesn't find anything there it looks in the craft_plugins folder.

====Name of End File====
Default is end.gcode.

If there is a file with the name of the "Name of End File" setting, it will be added to the very end.

====Name of Start File====
Default is end.gcode.

If there is a file with the name of the "Name of Start File" setting, it will be added to the very beginning of the gcode.

===Set Positioning to Absolute===
Default is on.

When selected, preface will add the G90 command to set positioning to absolute.

===Set Units to Millimeters===
Default is on.

When selected, preface will add the G21 command to set the units to millimeters.

===Start at Home===
Default is off.

When selected, the G28 go to home gcode will be added at the beginning of the file.

===Turn Extruder Off===
====Turn Extruder Off at Shut Down====
Default is on.

When selected, the M103 turn extruder off gcode will be added at the end of the file.

====Turn Extruder Off at Start Up====
Default is on.

When selected, the M103 turn extruder off gcode will be added at the beginning of the file.

==Examples==
The following examples preface the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and preface.py.


> python preface.py
This brings up the preface dialog.


> python preface.py Screw Holder Bottom.stl
The preface tool is parsing the file:
Screw Holder Bottom.stl
..
The preface tool has created the file:
.. Screw Holder Bottom_preface.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import preface
>>> preface.main()
This brings up the preface dialog.


>>> preface.writeOutput( 'Screw Holder Bottom.stl' )
The preface tool is parsing the file:
Screw Holder Bottom.stl
..
The preface tool has created the file:
.. Screw Holder Bottom_preface.gcode

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from datetime import date
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import intercircle
from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import settings
from skeinforge.skeinforge_utilities import skeinforge_craft
from skeinforge.skeinforge_utilities import skeinforge_polyfile
from skeinforge.skeinforge_utilities import skeinforge_profile
import bfb
import raft
import speed
import os
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/28/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text = '', prefaceRepository = None ):
	"Preface and convert an svg file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), prefaceRepository )

def chan (varible, inc):
        varible = varible*inc
        varible = int(varible)
        return varible

def getCraftedTextFromText( text, prefaceRepository = None ):
	"Preface and convert an svg text."
	if gcodec.isProcedureDoneOrFileIsEmpty( text, 'preface' ):
		return text
	if prefaceRepository == None:
		prefaceRepository = settings.getReadRepository( PrefaceRepository() )
	return PrefaceSkein().getCraftedGcode( prefaceRepository, text )

def getNewRepository():
	"Get the repository constructor."
	return PrefaceRepository()

def writeOutput( fileName = '' ):
	"Preface the carving of a gcode file.  If no fileName is specified, preface the first unmodified gcode file in this folder."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName == '':
		return
	skeinforge_craft.writeChainTextWithNounMessage( fileName, 'preface' )


class PrefaceRepository:
	"A class to handle the preface settings."
	def __init__( self ):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository( 'skeinforge.skeinforge_tools.craft_plugins.preface.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Preface', self, '' )
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute( 'http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Preface' )
		self.meta = settings.StringSetting().getFromValue( 'Meta:', self, '' )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '- Name of Alteration Files -', self )
		self.nameOfEndFile = settings.StringSetting().getFromValue( 'Name of End File:', self, 'end.gcode' )
		self.nameOfStartFile = settings.StringSetting().getFromValue( 'Name of Start File:', self, 'start.gcode' )
		settings.LabelSeparator().getFromRepository( self )
		self.setPositioningToAbsolute = settings.BooleanSetting().getFromValue( 'Set Positioning to Absolute', self, True )
		self.setUnitsToMillimeters = settings.BooleanSetting().getFromValue( 'Set Units to Millimeters', self, True )
		self.startAtHome = settings.BooleanSetting().getFromValue( 'Start at Home', self, False )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '- Turn Extruder Off -', self )
		self.turnExtruderOffAtShutDown = settings.BooleanSetting().getFromValue( 'Turn Extruder Off at Shut Down', self, True )
		self.turnExtruderOffAtStartUp = settings.BooleanSetting().getFromValue( 'Turn Extruder Off at Start Up', self, True )
		self.executeTitle = 'Preface'

	def execute( self ):
		"Preface button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class PrefaceSkein:
	"A class to preface a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.lineIndex = 0
		self.oldLocation = None
		self.rotatedBoundaryLayers = []

	def addFromUpperLowerFile( self, fileName ):
		"Add lines of text from the fileName or the lowercase fileName, if there is no file by the original fileName in the directory."
		fileText = settings.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), fileName )
		fileLines = gcodec.getTextLines( fileText )
		self.distanceFeedRate.addLinesSetAbsoluteDistanceMode( fileLines )

	def addInitializationToOutput( self ):
		"Add initialization gcode to the output."
		self.addFromUpperLowerFile( self.prefaceRepository.nameOfStartFile.value ) # Add a start file if it exists.
		self.distanceFeedRate.addTagBracketedLine( 'creator', 'skeinforge' ) # GCode formatted comment
		absoluteFilePathUntilDot = os.path.abspath( __file__ )[ : os.path.abspath( __file__ ).rfind( '.' ) ]
		if absoluteFilePathUntilDot == '/home/enrique/Desktop/backup/babbleold/script/reprap/pyRepRap/skeinforge_tools/craft_plugins/preface': #check to see if this script is on Enrique's computer
			gcodec.writeFileText( gcodec.getVersionFileName(), date.today().isoformat() )
		versionText = gcodec.getFileText( gcodec.getVersionFileName() )
		self.distanceFeedRate.addTagBracketedLine( 'version', versionText ) # GCode formatted comment
		self.distanceFeedRate.addLine( '(<extruderInitialization>)' ) # GCode formatted comment
		"BFB CODE 1st INITIALIZATION OF EXTRUDERS --------------------------------------------------------------------------------------------"
		self.bfbclass = bfb.BfbRepository()
		self.bfbclass = settings.getReadRepository( self.bfbclass )
		self.raftclass = raft.RaftRepository()
		self.raftclass = settings.getReadRepository( self.raftclass )
		self.secondHeadPrimeRev = chan(self.bfbclass.secondHeadPrimeRev.value, 6400)
		self.secondHeadPrimeSpeed = chan(self.bfbclass.secondHeadPrimeSpeed.value, 10)
		self.secondHeadPrintRev = chan(self.bfbclass.secondHeadPrintRev.value, 6400)
		self.secondHeadPrintPrime = chan(self.bfbclass.secondHeadPrintPrime.value, 6400)
		self.firstHeadPrimeRev = chan(self.bfbclass.firstHeadPrimeRev.value, 6400)
		self.firsHeadPrimeSpeed = chan(self.bfbclass.firsHeadPrimeSpeed.value, 10)
		self.firstHeadPrintRev = chan(self.bfbclass.firstHeadPrintRev.value, 6400)
		self.firstHeadPrintPrime = chan(self.bfbclass.firstHeadPrintPrime.value, 6400)
		self.speedclass = speed.SpeedRepository()
		self.speedclass = settings.getReadRepository( self.speedclass )
		travelfeedRateMinute=60*self.speedclass.travelFeedRatePerSecond.value
		version=settings.getfirmware()
		version=version[0:2]
		
		
		if self.raftclass.activateRaft.value == True:
			"use second extruder for raft"
			if self.bfbclass.useRaft.value == True:
				if version == "x1":
                                        self.distanceFeedRate.addLine( '#v3.00' )
					self.distanceFeedRate.addLine( 'G90' )
					self.distanceFeedRate.addLine( 'M228' )
					self.distanceFeedRate.addLine( 'M103' )
					self.distanceFeedRate.addLine( 'G92 X'+str(self.bfbclass.secondHeadOffsetX.value)+' Y'+str(self.bfbclass.secondHeadOffsetY.value)+' F'+str(travelfeedRateMinute) )
					self.distanceFeedRate.addLine( 'M107' )
					self.distanceFeedRate.addLine( 'M204 S'+str(self.bfbclass.RaftT.value) )
					self.distanceFeedRate.addLine( 'M552 P'+str(self.secondHeadPrimeRev)+' S'+str(self.secondHeadPrimeSpeed) )
					self.distanceFeedRate.addLine( 'M103 P3200 S1000' )
					self.distanceFeedRate.addLine( 'M543' )
					if self.bfbclass.useM227.value== True:
						self.distanceFeedRate.addLine( 'M227 P'+str(self.secondHeadPrintPrime)+' S'+str(self.secondHeadPrintRev) )
				if version == "x2":
                                        self.distanceFeedRate.addLine( '#v4.00' )
					self.distanceFeedRate.addLine( 'G90' )
					self.distanceFeedRate.addLine( 'G92 X'+str(self.bfbclass.secondHeadOffsetX.value)+' Y'+str(self.bfbclass.secondHeadOffsetY.value)+' F'+str(travelfeedRateMinute) )
					if self.bfbclass.useM227.value== True:
						self.distanceFeedRate.addLine( 'M227 P'+str(self.secondHeadPrintPrime)+' S'+str(self.secondHeadPrintRev) )
					self.distanceFeedRate.addLine( 'M107' )
					self.distanceFeedRate.addLine( 'M204 S'+str(self.bfbclass.RaftT.value) )
					self.distanceFeedRate.addLine( 'M552 P'+str(self.secondHeadPrimeRev)+' S'+str(self.secondHeadPrimeSpeed) )
					self.distanceFeedRate.addLine( 'M543' )
				if self.bfbclass.UseFanEx2.value== True:
					"Turn ON FAN"
					self.distanceFeedRate.addLine( 'M106')
				else:
					"Turn OFF FAN"
					self.distanceFeedRate.addLine( 'M107')
			
			else:
				if version == "x1":
					"use just one extruder for raft"
					self.distanceFeedRate.addLine( 'G90' )
					self.distanceFeedRate.addLine( 'M228' )
					self.distanceFeedRate.addLine( 'M103' )
					self.distanceFeedRate.addLine( 'M107' )
					self.distanceFeedRate.addLine( 'M104 S'+str(self.bfbclass.RaftT.value) )
					self.distanceFeedRate.addLine( 'M551 P'+str(self.firstHeadPrimeRev)+' S'+str(self.firsHeadPrimeSpeed) )
					self.distanceFeedRate.addLine( 'M103 P3200 S1000' )
					self.distanceFeedRate.addLine( 'M543' )
					
				if version == "x2":
					"use just one extruder for raft"
					self.distanceFeedRate.addLine( 'G90' )
					self.distanceFeedRate.addLine( 'M107' )
					self.distanceFeedRate.addLine( 'M104 S'+str(self.bfbclass.RaftT.value) )
					self.distanceFeedRate.addLine( 'M551 P'+str(self.firstHeadPrimeRev)+' S'+str(self.firsHeadPrimeSpeed) )
					self.distanceFeedRate.addLine( 'M543' )
				if self.bfbclass.useM227.value== True:
						self.distanceFeedRate.addLine( 'M227 P'+str(self.firstHeadPrintPrime)+' S'+str(self.firstHeadPrintRev) )
				if self.bfbclass.UseFanEx1.value== True:
					"Turn ON FAN"
					self.distanceFeedRate.addLine( 'M106')
				else:
					"Turn OFF FAN"
					self.distanceFeedRate.addLine( 'M107')
		else:
			if version == "x1":
				"NO RAFT"
				self.distanceFeedRate.addLine( 'G90' )
				self.distanceFeedRate.addLine( 'M228' )
				self.distanceFeedRate.addLine( 'M103' )
				self.distanceFeedRate.addLine( 'M107' )
				self.distanceFeedRate.addLine( 'M104 S'+str(self.bfbclass.firstWorkT.value) )
				self.distanceFeedRate.addLine( 'M551 P'+str(self.firstHeadPrimeRev)+' S'+str(self.firsHeadPrimeSpeed) )
				self.distanceFeedRate.addLine( 'M103 P3200 S1000' )
				self.distanceFeedRate.addLine( 'M543' )
			if version == "x2":
				self.distanceFeedRate.addLine( 'G90' )
				self.distanceFeedRate.addLine( 'M107' )
				self.distanceFeedRate.addLine( 'M104 S'+str(self.bfbclass.firstWorkT.value) )
				self.distanceFeedRate.addLine( 'M551 P'+str(self.firstHeadPrimeRev)+' S'+str(self.firsHeadPrimeSpeed) )
				self.distanceFeedRate.addLine( 'M543' )
			if self.bfbclass.useM227.value== True:
				self.distanceFeedRate.addLine( 'M227 P'+str(self.firstHeadPrintPrime)+' S'+str(self.firstHeadPrintRev) )
			if self.bfbclass.UseFanEx1.value== True:
				"Turn ON FAN"
				self.distanceFeedRate.addLine( 'M106')
			else:
				"Turn OFF FAN"
				self.distanceFeedRate.addLine( 'M107')
		if self.prefaceRepository.setPositioningToAbsolute.value:
			self.distanceFeedRate.addLine( 'G90' ) # Set positioning to absolute.
		if self.prefaceRepository.setUnitsToMillimeters.value:
			self.distanceFeedRate.addLine( 'G21' ) # Set units to millimeters.
		if self.prefaceRepository.startAtHome.value:
			self.distanceFeedRate.addLine( 'G28' ) # Start at home.
		if self.prefaceRepository.turnExtruderOffAtStartUp.value:
			self.distanceFeedRate.addLine( 'M103' ) # Turn extruder off.
		craftTypeName = skeinforge_profile.getCraftTypeName()
		self.distanceFeedRate.addTagBracketedLine( 'craftTypeName', craftTypeName )
		self.distanceFeedRate.addTagBracketedLine( 'decimalPlacesCarried', self.distanceFeedRate.decimalPlacesCarried )
		self.distanceFeedRate.addTagBracketedLine( 'layerThickness', self.distanceFeedRate.getRounded( self.layerThickness ) )
		if self.prefaceRepository.meta.value:
			self.distanceFeedRate.addTagBracketedLine( 'meta', self.prefaceRepository.meta.value )
		self.distanceFeedRate.addTagBracketedLine( 'perimeterWidth', self.distanceFeedRate.getRounded( self.perimeterWidth ) )
		self.distanceFeedRate.addTagBracketedLine( 'profileName', skeinforge_profile.getProfileName( craftTypeName ) )
		self.distanceFeedRate.addTagBracketedLine( 'procedureDone', 'carve' )
		self.distanceFeedRate.addTagBracketedLine( 'procedureDone', 'preface' )
		self.distanceFeedRate.addLine( '(</extruderInitialization>)' ) # Initialization is finished, extrusion is starting.
		self.distanceFeedRate.addLine( '(<extrusion>)' ) # Initialization is finished, extrusion is starting.

	def addPathData( self, line ):
		"Add the data from the path line."
		line = line.replace( '"', ' ' )
		splitLine = line.split()
		if splitLine[ 1 ] != 'transform=':
			return
		line = line.lower()
		line = line.replace( 'm', ' ' )
		line = line.replace( 'l', ' ' )
		line = line.replace( '/>', ' ' )
		splitLine = line.split()
		if 'd=' not in splitLine:
			return
		splitLine = splitLine[ splitLine.index( 'd=' ) + 1 : ]
		pathSequence = []
		for word in splitLine:
			if word == 'z':
				loop = []
				for pathSequenceIndex in xrange( 0, len( pathSequence ), 2 ):
					coordinate = complex( pathSequence[ pathSequenceIndex ], pathSequence[ pathSequenceIndex + 1 ] )
					loop.append( coordinate )
				self.rotatedBoundaryLayer.loops.append( loop )
				pathSequence = []
			else:
				pathSequence.append( float( word ) )

	def addPreface( self, rotatedBoundaryLayer ):
		"Add preface to the carve layer."
		self.distanceFeedRate.addLine( '(<layer> %s )' % rotatedBoundaryLayer.z ) # Indicate that a new layer is starting.
		if rotatedBoundaryLayer.rotation != None:
			self.distanceFeedRate.addTagBracketedLine( 'bridgeRotation', str( rotatedBoundaryLayer.rotation ) ) # Indicate the bridge rotation.
		for loop in rotatedBoundaryLayer.loops:
			self.distanceFeedRate.addGcodeFromLoop( loop, rotatedBoundaryLayer.z )
		self.distanceFeedRate.addLine( '(</layer>)' )

	def addRotatedLoopLayer( self, z ):
		"Add rotated loop layer."
		self.rotatedBoundaryLayer = euclidean.RotatedLoopLayer( z )
		self.rotatedBoundaryLayers.append( self.rotatedBoundaryLayer )

	def addShutdownToOutput( self ):
		"Add shutdown gcode to the output."
		self.distanceFeedRate.addLine( '(</extrusion>)' ) # GCode formatted comment
		if self.prefaceRepository.turnExtruderOffAtShutDown.value:
			self.distanceFeedRate.addLine( 'M103' ) # Turn extruder motor off.
		self.addFromUpperLowerFile( self.prefaceRepository.nameOfEndFile.value ) # Add an end file if it exists.

	def addTextData( self, line ):
		"Add the data from the text line."
		if line.find( 'layerThickness' ) != - 1:
			return
		line = line.replace( '>', ' ' )
		line = line.replace( '<', ' ' )
		line = line.replace( ',', ' ' )
		splitLine = line.split()
		if 'Layer' not in splitLine:
			return
		splitLine = splitLine[ splitLine.index( 'Layer' ) + 1 : ]
		if 'z' not in splitLine:
			return
		zIndex = splitLine.index( 'z' )
		self.addRotatedLoopLayer( float( splitLine[ zIndex + 1 ] ) )

	def getCraftedGcode( self, prefaceRepository, gcodeText ):
		"Parse gcode text and store the bevel gcode."
		self.prefaceRepository = prefaceRepository
		gcodeText = gcodeText.replace( '\t', ' ' )
		gcodeText = gcodeText.replace( ';', ' ' )
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		self.addInitializationToOutput()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			self.addPreface( rotatedBoundaryLayer )
		self.addShutdownToOutput()
		return self.distanceFeedRate.output.getvalue()

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ].lstrip()
			splitLine = gcodec.getWithoutBracketsEqualTab( line ).split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == 'layerThickness':
				self.layerThickness = float( splitLine[ 1 ] )
			elif firstWord == 'extrusionStart':
				return
			elif firstWord == 'perimeterWidth':
				self.perimeterWidth = float( splitLine[ 1 ] )

	def parseLine( self, lineIndex ):
		"Parse a gcode line and add it to the preface skein."
		line = self.lines[ lineIndex ].lstrip()
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '(<bridgeRotation>' or firstWord == '<!--bridgeRotation-->':
			secondWordWithoutBrackets = splitLine[ 1 ].replace( '(', '' ).replace( ')', '' )
			self.rotatedBoundaryLayer.rotation = complex( secondWordWithoutBrackets )
		elif firstWord == '<path':
			self.addPathData( line )
		elif firstWord == '<text':
			self.addTextData( line )


def main():
	"Display the preface dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
