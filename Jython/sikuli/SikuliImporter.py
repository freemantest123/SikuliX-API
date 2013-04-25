# Copyright 2010-2011, Sikuli.org
# Released under the MIT License.
# modified RaiMan 2012
import sys
import os
import java.lang.System
import imp
from org.sikuli.script import Settings
from org.sikuli.script import ImageLocator
from org.sikuli.script import Debug
import Sikuli

def _stripPackagePrefix(module_name):
   pdot = module_name.rfind('.')
   if pdot >= 0:
      return module_name[pdot+1:]
   return module_name

class SikuliImporter:

   class SikuliLoader:
      def __init__(self, path):
         self.path = path

      def _load_module(self, fullname):
         (file, pathname, desc) =  imp.find_module(fullname)
         try:
            return imp.load_module(fullname, file, pathname, desc)
         except Exception,e:
            etype, evalue, etb = sys.exc_info()
            evalue = etype("%s !!WHILE IMPORTING!! " % evalue)
            raise etype, evalue, etb
         finally:
            if file:
               file.close()

      def load_module(self, module_name):
         #print "SikuliLoader.load_module", module_name, self.path
         module_name = _stripPackagePrefix(module_name)
         p = ImageLocator.addImagePath(self.path)
         #print "SikuliLoader.load_module: ImageLocator returned path:", p
         if not p: return None
         Sikuli.addModPath(p)
         return self._load_module(module_name)

   def _find_module(self, module_name, fullpath):
      fullpath = fullpath + "/" + module_name + ".sikuli"
      if os.path.exists(fullpath):
         #print "SikuliImporter found", fullpath
         return self.SikuliLoader(fullpath)
      return None

   def find_module(self, module_name, package_path):
      #print "SikuliImporter.find_module", module_name, package_path
      module_name = _stripPackagePrefix(module_name)
      if module_name[0:1] == "*": 
        return None
      if package_path:
         paths = package_path
      else:
         paths = sys.path
      for path in paths:
         mod = self._find_module(module_name, path)
         if mod:
            return mod
      if Sikuli.load(module_name +".jar"):
         Debug.log(2,module_name + ".jar loaded")
         return None
      return None

sys.meta_path.append(SikuliImporter())
del SikuliImporter
