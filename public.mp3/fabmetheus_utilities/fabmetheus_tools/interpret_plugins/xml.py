"""
This page is in the table of contents.
The xml.py script is an import translator plugin to get a carving from an xml file.

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


An example of an xml boolean geometry format file follows below.

<?xml version='1.0' ?>
<booleangeometry version="2010-03-10">
	<booleansolid id="Boolean1" operation="lastminuscomplement">
		<cube halfx="1.6" halfy="2.925" halfz="1.6" id="Cube 1">
			<matrix4x4 m14="-0.425" m24="-0.53">
			</matrix4x4>
		</cube>
		<cube halfx="1.36" halfy="3.05" halfz="1.36" id="Cube 2">
			<matrix4x4 m14="0.665" m24="0.405">
			</matrix4x4>
		</cube>
		<matrix4x4 m14="-20.1" m24="0.19">
		</matrix4x4>
	</booleansolid>
</booleangeometry>

In the 'booleangeometry' format, all class and attribute names are lower case.  The defined geometric objects are booleansolid, cube, cylinder, sphere and trianglemesh.  The id attribute is not necessary.  The default matrix is a four by four identity matrix.  The attributes of the cube, cylinder and sphere default to one.  The attributes of the vector3 vertices in the triangle mesh default to zero.  The boolean operation names are join, intersection, firstminuscomplement and lastminuscomplement.  Examples of the remaining geometric obejcts follow below.

<?xml version='1.0' ?>
<booleangeometry version="2010-03-10">
	<cylinder height="20.4" id="Cylinder 1" radiusx="5.4" radiusz="5.4" topoverbottom="1.0">
		<matrix4x4 m14="0.6" m24="-1.3">
		</matrix4x4>
	</cylinder>
</booleangeometry>

<?xml version='1.0' ?>
<booleangeometry version="2010-03-10">
	<sphere id="Sphere1" radiusx="15.0" radiusy="10.0" radiusz="5.0">
		<matrix4x4>
		</matrix4x4>
	</sphere>
</booleangeometry>

<?xml version='1.0' ?>
<booleangeometry version="2010-03-10">
	<trianglemesh id="tetrahedron">
		<vertices>
			<vector3 x="-5.0" y="0.0" z="2.89">
			</vector3>
			<vector3 x="0.0" y="0.0" z="-5.77">
			</vector3>
			<vector3 x="5.0" y="0.0" z="2.89">
			</vector3>
			<vector3 x="0.0" y="8.66" z="0.0">
			</vector3>
		</vertices>
		<faces>
			<face vertex0="3" vertex1="0" vertex2="1">
			</face>
			<face vertex0="3" vertex1="1" vertex2="2">
			</face>
			<face vertex0="3" vertex1="2" vertex2="0">
			</face>
			<face vertex0="0" vertex1="2" vertex2="1">
			</face>
		</faces>
		<matrix4x4 m11="3.74939945665e-33" m13="1.0" m21="-1.0" m22="6.12323399574e-17" m32="-1.0" m33="6.12323399574e-17">
		</matrix4x4>
	</trianglemesh>
</booleangeometry>

The 'booleangeometry' xml format is the preferred skeinforge format.  When the Interpret button in the Interpret tool in Meta is clicked, any xml format for which there is a plugin will be converted to the 'booleangeometry' format.

There is a plugin for the 'Art of Illusion' xml format.  An xml file can be exported from Art of Illusion by going to the "File" menu, then going into the "Export" menu item, then picking the XML choice.  This will bring up the XML file chooser window, choose a place to save the file then click "OK".  Leave the "compressFile" checkbox unchecked.  All the objects from the scene will be exported, the artofillusion plugin will ignore the light and camera.  If you want to fabricate more than one object at a time, you can have multiple objects in the Art of Illusion scene and they will all be carved, then fabricated together.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.xml_simple_parser import XMLSimpleParser
from fabmetheus_utilities import gcodec
import os
import sys

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCarving( fileName = '' ):
	"Get the carving for the xml file."
	xmlText = gcodec.getFileText( fileName )
	if xmlText == '':
		return None
	xmlParser = XMLSimpleParser( xmlText )
	lowerClassName = xmlParser.rootElement.children[ 0 ].className.lower()
	pluginModule = gcodec.getModuleWithDirectoryPath( getPluginsDirectoryPath(), lowerClassName )
	if pluginModule == None:
		return None
	return pluginModule.getCarvingFromParser( xmlParser )

def getPluginsDirectoryPath():
	"Get the plugins directory path."
	return gcodec.getAbsoluteFolderPath( __file__, 'xml_plugins' )

def main():
	"Display the inset dialog."
	if len( sys.argv ) > 1:
		getCarving( ' '.join( sys.argv[ 1 : ] ) )

if __name__ == "__main__":
	main()
