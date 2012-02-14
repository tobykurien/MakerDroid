"""
XML tag writer utilities.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def addBeginXMLTag( attributeDictionary, depth, name, output ):
	"Add the begin xml tag."
	attributeDictionaryString = ''
	attributeDictionaryKeys = attributeDictionary.keys()
	attributeDictionaryKeys.sort()
	for attributeDictionaryKey in attributeDictionaryKeys:
		attributeDictionaryString += ' %s="%s"' % ( attributeDictionaryKey, attributeDictionary[ attributeDictionaryKey ] )
	depthStart = '\t' * depth
	output.write( '%s<%s%s>\n' % ( depthStart, name, getAttributeDictionaryString( attributeDictionary ) ) )

def addClosedXMLTag( attributeDictionary, depth, name, output ):
	"Add the closed xml tag."
	attributeDictionaryString = ''
	attributeDictionaryKeys = attributeDictionary.keys()
	attributeDictionaryKeys.sort()
	for attributeDictionaryKey in attributeDictionaryKeys:
		attributeDictionaryString += ' %s="%s"' % ( attributeDictionaryKey, attributeDictionary[ attributeDictionaryKey ] )
	depthStart = '\t' * depth
	output.write( '%s<%s%s />\n' % ( depthStart, name, getAttributeDictionaryString( attributeDictionary ) ) )

def addEndXMLTag( depth, name, output ):
	"Add the end xml tag."
	depthStart = '\t' * depth
	output.write( '%s</%s>\n' % ( depthStart, name ) )

def getAttributeDictionaryString( attributeDictionary ):
	"Add the closed xml tag."
	attributeDictionaryString = ''
	attributeDictionaryKeys = attributeDictionary.keys()
	attributeDictionaryKeys.sort()
	for attributeDictionaryKey in attributeDictionaryKeys:
		attributeDictionaryString += ' %s="%s"' % ( attributeDictionaryKey, attributeDictionary[ attributeDictionaryKey ] )
	return attributeDictionaryString
