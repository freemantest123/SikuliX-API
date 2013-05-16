# Copyright 2010-2011, Sikuli.org
# Released under the MIT License.
# modified RaiMan 2012

from org.sikuli.script import Region as JRegion
from org.sikuli.script import Location
from org.sikuli.script import Settings
from org.sikuli.script import SikuliEventAdapter
from Constants import *
import sys
import inspect
import types
import time
import java.lang.String
import __main__
import __builtin__

DEBUG=False

class Region(JRegion):

   def __init__(self, *args):
      if DEBUG: print "**IN*** Jython INIT Region"
      if len(args)==4:
         JRegion.__init__(self, args[0], args[1], args[2], args[3])
      elif len(args)==1:
         JRegion.__init__(self, args[0])
      else:
         raise Exception("Wrong number of parameters of Region's contructor")
      self.setScriptingType("JythonRegion")
      self._global_funcs = None
      if DEBUG: print "**OUT** Jython INIT Region"

########################## support for with:
   # override all global sikuli functions by this region's methods.
   def __enter__(self):
      exclude_list = [ 'ROI' ]
      if DEBUG: print "with: entering *****", self
      self._global_funcs = {}
      dict = sys.modules['__main__'].__dict__
      for name in dir(self):
         if name in exclude_list: continue
         try:
            if not inspect.ismethod(getattr(self,name)):
               continue
         except:
            continue
         if dict.has_key(name):
            self._global_funcs[name] = dict[name]
            if DEBUG and name == 'checkWith': print "with: save %s ( %s )"%(name, str(dict[name])[1:])
            dict[name] = eval("self."+name)
            if DEBUG and name == 'checkWith': print "with: is now: %s"%(str(dict[name])[1:])
      return self

   def __exit__(self, type, value, traceback):
      if DEBUG: print "with: exiting ****", self
      dict = sys.modules['__main__'].__dict__
      for name in self._global_funcs.keys():
         dict[name] = self._global_funcs[name]
         if DEBUG and name == 'checkWith': print "with restore: %s"%(str(dict[name])[1:])
      self._global_funcs = None

########################## helper to check with
   def checkWith(self):
      print "checkWith: from: ", self

#######################################################################
#---- SIKULI  PUBLIC  API
#######################################################################

########################## Python wait() needs to be here because Java Object has a final method: wait(long timeout).
# If we want to let Sikuli users use wait(int/long timeout), we need this Python method.
# FIXME: default timeout should be autoWaitTimeout
   def wait(self, target, timeout=None):
      if isinstance(target, int) or isinstance(target, long) or isinstance(target, float):
         time.sleep(target)
         return
      if timeout == None:
         ret = JRegion.wait(self, target)
      else:
         ret = JRegion.wait(self, target, timeout)
      return ret

########################## Python paste() needs to be here because of encoding conversion
   def paste(self, *args):
      if len(args) == 1:
         target = None
         s = args[0]
      elif len(args) == 2:
         target = args[0]
         s = args[1]
      if isinstance(s, types.StringType):
         s = java.lang.String(s, "utf-8")
      return JRegion.paste(self, target, s)

######################### the new Region.text() feature (Tesseract 3) returns utf8
   def text(self):
      return JRegion.text(self).encode("utf8")

########################## observe(): Special setup for Jython
   def onAppear(self, target, handler):
      class AnonyObserver(SikuliEventAdapter):
         def targetAppeared(self, event):
            handler(event)
      return JRegion.onAppear(self, target, AnonyObserver())

   def onVanish(self, target, handler):
      class AnonyObserver(SikuliEventAdapter):
         def targetVanished(self, event):
            handler(event)
      return JRegion.onVanish(self, target, AnonyObserver())

   def onChange(self, arg1, arg2=None):
      t_arg1 = __builtin__.type(arg1)
      if t_arg1 is types.IntType:
         min_size = arg1
         handler = arg2
      else:
         assert arg2 == None, "onChange: wrong arguments"
         min_size = None
         handler = arg1
      class AnonyObserver(SikuliEventAdapter):
         def targetChanged(self, event):
            handler(event)
      if min_size != None:
         return JRegion.onChange(self, min_size, AnonyObserver())
      return JRegion.onChange(self, AnonyObserver())

   def observe(self, time=FOREVER, background=False):
      if background:
         return self.observeInBackground(time)
      else:
         return JRegion.observe(self, time)
