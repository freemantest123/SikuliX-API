'''
Created on 29.10.2012

@author: rhocke
'''

from org.sikuli.script import Finder as JFinder

class Finder(JFinder):

# TODO make as Python  (support for with)
   
   def __init__(self):
      pass
   
   def __enter__(self):
      return super
   
   def __exit__(type, value, trackback):
      super.destroy()
   
   def __del__(type, value, trackback):
      super.destroy()
