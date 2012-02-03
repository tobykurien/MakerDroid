"""
This page is in the table of contents.
Cleave is a script to cleave a shape into svg slice layers.

==Settings==
===Extra Decimal Places===
Default is one.

Defines the number of extra decimal places export will output compared to the number of decimal places in the layer thickness.  The higher the 'Extra Decimal Places', the more significant figures the output numbers will have.

===Import Coarseness===
Default is one.

When a triangle mesh has holes in it, the triangle mesh slicer switches over to a slow algorithm that spans gaps in the mesh.  The higher the 'Import Coarseness' setting, the wider the gaps in the mesh it will span.  An import coarseness of one means it will span gaps of the perimeter width.

===Layer Thickness===
Default is 0.4 mm.

Defines the thickness of the layer, this is the most important cleave setting.

===Layers===
Cleave slices from bottom to top.  To get a single layer, set the "Layers From" to zero and the "Layers To" to one.  The layer from until layer to range is a python slice.

====Layers From====
Default is zero.

Defines the index of the bottom layer that will be cleaved.  If the layer from is the default zero, the carving will start from the lowest layer.  If the 'Layers From' index is negative, then the carving will start from the 'Layers From' index below the top layer.

====Layers To====
Default is a huge number, which will be limited to the highest index layer.

Defines the index of the top layer that will be cleaved.  If the 'Layers To' index is a huge number like the default, the carving will go to the top of the model.  If the 'Layers To' index is negative, then the carving will go to the 'Layers To' index below the top layer.

===Mesh Type===
Default is 'Correct Mesh'.

====Correct Mesh====
When selected, the mesh will be accurately cleaved, and if a hole is found, cleave will switch over to the algorithm that spans gaps.

====Unproven Mesh====
When selected, cleave will use the gap spanning algorithm from the start.  The problem with the gap spanning algothm is that it will span gaps, even if there is not actually a gap in the model.

===Perimeter Width===
Default is two millimeters.

Defines the width of the perimeter.

==Examples==
The following examples cleave the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and cleave.py.


> python cleave.py
This brings up the cleave dialog.


> python cleave.py Screw Holder Bottom.stl
The cleave tool is parsing the file:
Screw Holder Bottom.stl
..
The cleave tool has created the file:
.. Screw Holder Bottom_cleave.svg


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import cleave
>>> cleave.main()
This brings up the cleave dialog.


>>> cleave.writeOutput( 'Screw Holder Bottom.stl' )
The cleave tool is parsing the file:
Screw Holder Bottom.stl
..
The cleave tool has created the file:
.. Screw Holder Bottom_cleave.svg

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
from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import settings
from fabmetheus_utilities import svg_codec
from skeinforge.skeinforge_utilities import skeinforge_polyfile
from skeinforge.skeinforge_utilities import skeinforge_profile
import math
import os
import sys
import time


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/02/05 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, gcodeText = '', repository = None ):
	"Get cleaved text."
	if gcodec.getHasSuffix( fileName, '.svg' ):
		gcodeText = gcodec.getTextIfEmpty( fileName, gcodeText )
		if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'cleave' ):
			return gcodeText
	carving = svg_codec.getCarving( fileName )
	if carving == None:
		return ''
	if repository == None:
		repository = CleaveRepository()
		settings.getReadRepository( repository )
	return CleaveSkein().getCarvedSVG( carving, fileName, repository )

def getNewRepository():
	"Get the repository constructor."
	return CleaveRepository()

def writeOutput( fileName = '' ):
	"Cleave a GNU Triangulated Surface file.  If no fileName is specified, cleave the first GNU Triangulated Surface file in this folder."
	if fileName == '':
		unmodified = gcodec.getFilesWithFileTypesWithoutWords( fabmetheus_interpret.getImportPluginFileNames() )
		if len( unmodified ) == 0:
			print( "There are no carvable files in this folder." )
			return
		fileName = unmodified[ 0 ]
	startTime = time.time()
	print( 'File ' + gcodec.getSummarizedFileName( fileName ) + ' is being cleaved.' )
	cleaveGcode = getCraftedText( fileName )
	if cleaveGcode == '':
		return
	suffixFileName = fileName[ : fileName.rfind( '.' ) ] + '_cleave.svg'
	suffixDirectoryName = os.path.dirname( suffixFileName )
	suffixReplacedBaseName = os.path.basename( suffixFileName ).replace( ' ', '_' )
	suffixFileName = os.path.join( suffixDirectoryName, suffixReplacedBaseName )
	gcodec.writeFileText( suffixFileName, cleaveGcode )
	print( 'The cleaved file is saved as ' + gcodec.getSummarizedFileName( suffixFileName ) )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to cleave the file.' )
	settings.openWebPage( suffixFileName )


class CleaveRepository:
	"A class to handle the cleave settings."
	def __init__( self ):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository( 'skeinforge.skeinforge_tools.craft_plugins.cleave.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getTranslatorFileTypeTuples(), 'Open File to be Cleaved', self, '' )
		self.extraDecimalPlaces = settings.IntSpin().getFromValue( 0, 'Extra Decimal Places (integer):', self, 2, 1 )
		self.importCoarseness = settings.FloatSpin().getFromValue( 0.5, 'Import Coarseness (ratio):', self, 2.0, 1.0 )
		self.layerThickness = settings.FloatSpin().getFromValue( 0.1, 'Layer Thickness (mm):', self, 1.0, 0.4 )
		self.layersFrom = settings.IntSpin().getFromValue( 0, 'Layers From (index):', self, 20, 0 )
		self.layersTo = settings.IntSpin().getSingleIncrementFromValue( 0, 'Layers To (index):', self, 912345678, 912345678 )
		self.meshTypeLabel = settings.LabelDisplay().getFromName( 'Mesh Type: ', self, )
		importLatentStringVar = settings.LatentStringVar()
		self.correctMesh = settings.Radio().getFromRadio( importLatentStringVar, 'Correct Mesh', self, True )
		self.unprovenMesh = settings.Radio().getFromRadio( importLatentStringVar, 'Unproven Mesh', self, False )
		self.perimeterWidth = settings.FloatSpin().getFromValue( 0.4, 'Perimeter Width (mm):', self, 4.0, 2.0 )
		self.executeTitle = 'Cleave'

	def execute( self ):
		"Cleave button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypes( self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class CleaveSkein( svg_codec.SVGCodecSkein ):
	"A class to cleave a carving."
	def getCarvedSVG( self, carving, fileName, repository ):
		"Parse gnu triangulated surface text and store the cleaved gcode."
		self.carving = carving
		self.layerThickness = repository.layerThickness.value
		self.perimeterWidth = repository.perimeterWidth.value
		carving.setCarveLayerThickness( self.layerThickness )
		importRadius = 0.5 * repository.importCoarseness.value * abs( self.perimeterWidth )
		carving.setCarveImportRadius( max( importRadius, 0.01 * self.layerThickness ) )
		carving.setCarveIsCorrectMesh( repository.correctMesh.value )
		rotatedBoundaryLayers = carving.getCarveRotatedBoundaryLayers()
		if len( rotatedBoundaryLayers ) < 1:
			print( 'There are no slices for the model, this could be because the model is too small.' )
			return ''
		self.cornerMaximum = carving.getCarveCornerMaximum()
		self.cornerMinimum = carving.getCarveCornerMinimum()
		self.layerThickness = self.carving.layerThickness
		self.decimalPlacesCarried = max( 0, 1 + repository.extraDecimalPlaces.value - int( math.floor( math.log10( self.layerThickness ) ) ) )
		return self.getReplacedSVGTemplate( fileName, 'cleave', svg_codec.getTruncatedRotatedBoundaryLayers( repository, rotatedBoundaryLayers ) )


def main():
	"Display the cleave dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
