# Copyright 2010-2011, Sikuli.org
# Released under the MIT License.
# modified RaiMan 2012
from org.sikuli.script import Screen as JScreen
import inspect
#import __main__
#import __builtin__
import sys
import traceback

from Region import *
from java.awt import Rectangle

DEBUG=False

class Screen(Region):
   def __init__(self, id=None):
      if DEBUG: print "**IN*** Jython INIT Screen"
      if id != None:
         r = JScreen.getBounds(id)
      else:
         r = JScreen.getBounds(JScreen.getPrimaryId())
      (x, y, w, h) = (int(r.getX()), int(r.getY()), \
                      int(r.getWidth()), int(r.getHeight()))
      Region.__init__(self, x, y, w, h)
      self.setScriptingType("JythonScreen")
      if DEBUG: print "**OUT** Jython INIT Screen"

   @classmethod
   def getNumberScreens(cls):
      return JScreen.getNumberScreens()

   def getBounds(self):
      return self.getScreen().getBounds()

   def selectRegion(self, msg=None):
      if msg:
         r = self.getScreen().selectRegion(msg)
      else:
         r = self.getScreen().selectRegion()
      if r:
         return Region(r)
      else:
         return None

   ##
   # Enters the screen-capture mode asking the user to capture a region of
   # the screen if no arguments are given.
   # If any arguments are specified, capture() automatically captures the given
   # region of the screen.
   # @param *args The args can be 4 integers: x, y, w, and h, a <a href="org/sikuli/script/Match.html">Match</a> object or a {@link #Region} object.
   # @return The path to the captured image.
   #
   def capture(self, *args):
      scr = self.getScreen()
      if len(args) == 0:
         simg = scr.userCapture("Select an image")
         if simg:
            return simg.getFile()
         else:
            return None
      elif len(args) == 1:
         if __builtin__.type(args[0]) is types.StringType or __builtin__.type(args[0]) is types.UnicodeType:
            simg = scr.userCapture(args[0])
            if simg:
               return simg.getFile()
            else:
               return None
         else:
            return scr.capture(args[0]).getFile()
      elif len(args) == 4:
         return scr.capture(args[0], args[1], args[2], args[3]).getFile()
      else:
         return None

   def _exposeAllMethods(self, mod):
      exclude_list = [ 'class', 'classDictInit', 'clone', 'equals', 'finalize',
                       'getClass', 'hashCode', 'notify', 'notifyAll',
                       'toGlobalCoord', 'toString', 'getLocationFromPSRML', 'getRegionFromPSRM',
                       'capture', 'selectRegion', 'create', 'observeInBackground', 'waitAll',
                       'updateSelf', 'findNow', 'findAllNow', 'getEventManager']
      if DEBUG: print "***** _exposeAllMethods", str(self)
      dict = sys.modules[mod].__dict__
      for name in dir(self):
         if name in exclude_list: continue
         try:
            if not inspect.ismethod(getattr(self,name)): continue
         except:
            continue
         if name[0] != '_' and name[:7] != 'super__':
            dict[name] = eval("self."+name)
            if DEBUG and name == 'checkWith': print name, str(dict[name])[1:]
            #__main__.__dict__[name] = eval("self."+name)

   def toString(self):
      return self.getScreen().toString()

