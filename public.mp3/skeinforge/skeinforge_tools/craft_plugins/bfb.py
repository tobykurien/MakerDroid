#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import settings
from fabmetheus_utilities import svg_codec
from fabmetheus_utilities.solids import triangle_mesh
from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from skeinforge.skeinforge_utilities import skeinforge_polyfile
from skeinforge.skeinforge_utilities import skeinforge_profile
import math
import os
import sys
import time


        
def getNewRepository():
	"Get the repository constructor."
	return BfbRepository()

class BfbRepository:
	"A class to handle the carve settings."
	def __init__( self ):
		"Set the default settings, execute title & settings fileName."
		
		skeinforge_profile.addListsToCraftTypeRepository( 'skeinforge.skeinforge_tools.craft_plugins.bfb.html', self )
		self.useRaft = settings.BooleanSetting().getFromValue( 'Use Extruder 2 for Raft', self, False )
		self.useSupport = settings.BooleanSetting().getFromValue( 'Use Extruder 2 for Support', self, False )
		settings.LabelSeparator().getFromRepository( self )
		self.firstWorkT = settings.IntSpin().getFromValue( 0, 'Work Temperature:', self, 300, 100 )
		self.RaftT = settings.IntSpin().getFromValue( 0, 'Raft Temperature :', self, 300, 100 )
		self.SupportT = settings.IntSpin().getFromValue( 0, 'Support Temperature :', self, 300, 100 )
		
		settings.LabelSeparator().getFromRepository( self )
		self.firstStandbyT = settings.IntSpin().getFromValue( 0, 'Standby Temperature Extruder 1:', self, 300, 100 )
		self.secondStandbyT = settings.IntSpin().getFromValue( 0, 'Standby Temperature Extruder 2:', self, 300, 100 )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '2nd Head Offset', self )
		self.secondHeadOffsetX = settings.FloatSpin().getFromValue( (-100.0), 'X:', self, 100.0, 1.0 )
		self.secondHeadOffsetY = settings.FloatSpin().getFromValue( (-100.0), 'Y:', self, 100.0, 1.0 )
		settings.LabelSeparator().getFromRepository( self )
		self.useM227 = settings.BooleanSetting().getFromValue( 'Activate M227', self, True )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '1st Head Print', self )
		self.firstHeadPrintRev = settings.FloatSpin().getFromValue( 0.1, '1stPrintRev:', self, 100.0, 1.0 )
		self.firstHeadPrintPrime = settings.FloatSpin().getFromValue( 0.1, '1stPrintPrime:', self, 100.0, 1.0 )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '2nd Head Print', self )
		self.secondHeadPrintRev = settings.FloatSpin().getFromValue( 0.1, '2ndPrintRev:', self, 100.0, 1.0 )
		self.secondHeadPrintPrime = settings.FloatSpin().getFromValue( 0.1, '2ndPrintPrime:', self, 100.0, 1.0 )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '1st Head Reverse', self )
		self.firstHeadReverseRev = settings.FloatSpin().getFromValue( 0.1, '1stRev:', self, 100.0, 1.0 )
		self.firstHeadReverseSpeed = settings.IntSpin().getFromValue( 1, '1stSpeed:', self, 200, 100 )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '2nd Head Reverse', self )
		self.secondHeadReverseRev = settings.FloatSpin().getFromValue( 0.1, '2ndRev:', self, 100.0, 1.0 )
		self.secondHeadReverseSpeed = settings.IntSpin().getFromValue( 1, '2ndSpeed:', self, 200, 100 )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '1st Head Prime', self )
		self.firstHeadPrimeRev = settings.FloatSpin().getFromValue( 0.1, '1stPrimeRev:', self, 100.0, 1.0 )
		self.firsHeadPrimeSpeed = settings.IntSpin().getFromValue( 1, '1stPrimeSpeed:', self, 200, 100 )
		settings.LabelSeparator().getFromRepository( self )
		settings.LabelDisplay().getFromName( '2nd Head Prime', self )
		self.secondHeadPrimeRev = settings.FloatSpin().getFromValue( 0.1, '2ndPrimeRev:', self, 100.0, 1.0 )
		self.secondHeadPrimeSpeed = settings.IntSpin().getFromValue( 1, '2ndPrimeSpeed:', self, 200, 100 )
		settings.LabelSeparator().getFromRepository( self )
		self.UseFanEx1 = settings.BooleanSetting().getFromValue( 'UseFanEx1', self, False )
		self.UseFanEx2 = settings.BooleanSetting().getFromValue( 'UseFanEx2', self, False )
		self.firstLayerT = settings.IntSpin().getFromValue( 0, 'First Layer Temperature :', self, 300, 100 )
		""
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute( 'http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Carve' )
		importLatentStringVar = settings.LatentStringVar()
		self.executeTitle = 'Bfb'

	def execute( self ):
		print 'Press "Sceinforge" button please!'

def main():
	"Display the carve dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
