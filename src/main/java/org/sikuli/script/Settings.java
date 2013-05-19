/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.io.File;
import java.lang.reflect.Constructor;
import java.security.CodeSource;
import java.util.Date;
import java.util.Properties;
import org.sikuli.system.OSUtil;

public class Settings {

  public static String UserName = "UnKnown";

  public static int breakPoint = 0;

  public static String libPath = null;
  private static final String sikhomeEnv = System.getenv("SIKULIX_HOME");
  private static final String sikhomeProp = System.getProperty("sikuli.Home");
  public static final String libSub = FileManager.slashify("SikuliX-IDE/libs", false);
  private static String checkFileName;

	/**
	 * Mac: standard place for native libs
	 */
	public static String libPathMac = "/Applications/SikuliX-IDE.app/Contents/libs";

  /**
	 * Win: standard place for native libs
	 */
	public static final String libPathWin = FileManager.slashify(System.getenv("ProgramFiles"), true)+libSub;
	public static final String libPathWin32 = FileManager.slashify(System.getenv("ProgramFiles(x86)"), true)+libSub;

  /**
	 * location of folder Tessdata
	 */
	public static String OcrDataPath;

  /**
	 * standard place in the net to get information about extensions<br />
	 * needs a file extensions.json with content<br />
	 * {"extension-list":<br />
   * &nbsp;{"extensions":<br />
   * &nbsp;&nbsp;[<br />
   * &nbsp;&nbsp;&nbsp;{<br />
   * &nbsp;&nbsp;&nbsp;&nbsp;"name":"SikuliGuide",<br />
   * &nbsp;&nbsp;&nbsp;&nbsp;"version":"0.3",<br />
   * &nbsp;&nbsp;&nbsp;&nbsp;"description":"visual annotations",<br />
   * &nbsp;&nbsp;&nbsp;&nbsp;"imgurl":"somewhere in the net",<br />
   * &nbsp;&nbsp;&nbsp;&nbsp;"infourl":"http://doc.sikuli.org",<br />
   * &nbsp;&nbsp;&nbsp;&nbsp;"jarurl":"---extensions---"<br />
   * &nbsp;&nbsp;&nbsp;},<br />
   * &nbsp;&nbsp;]<br />
   * &nbsp;}<br />
   * }<br />
	 * imgurl: to get an icon from<br />
	 * infourl: where to get more information<br />
	 * jarurl: where to download the jar from (no url: this standard place)<br />
	 */
  public static String SikuliRepo;
  private static String[] args = new String[0];
  public static String[] ServerList = {"https://dl.dropbox.com/u/42895525/SikuliX"};

  public static final int SikuliVersionMajor = 1;
  public static final int SikuliVersionMinor = 0;
  public static final int SikuliVersionSub = 0;
  public static final int SikuliVersionBetaN = 999;
  private static final String sversion = String.format("%d.%d.%d",
                                   SikuliVersionMajor, SikuliVersionMinor, SikuliVersionSub);
  private static final String bversion = String.format("%d.%d-Beta%d",
                                   SikuliVersionMajor, SikuliVersionMinor, SikuliVersionBetaN);
  public static final String SikuliVersionDefault = "SikuliX-"+sversion;
 	public static final String SikuliVersionBeta = "SikuliX-" + bversion;
  public static final String SikuliVersionDefaultIDE = "SikuliX-IDE-" + sversion;
 	public static final String SikuliVersionBetaIDE = "SikuliX-IDE-" + bversion;
	public static String SikuliVersion;
  public static String SikuliVersionIDE;
  public static String BaseTempPath;

//TODO move libs check to FileManager
	static {
    doDebug = false;
    File libsfolder;
    String libspath;
    Properties props = System.getProperties(); //for debugging

    if (System.getProperty("user.name") != null && !"".equals(System.getProperty("user.name"))) {
      UserName = System.getProperty("user.name");
    }
    BaseTempPath = System.getProperty("java.io.tmpdir") + File.separator + UserName;

    // check Java property sikuli.home
    if (sikhomeProp != null) {
      libspath = (new File(FileManager.slashify(sikhomeProp, true) + "libs")).getAbsolutePath();
      if ((new File(libspath)).exists()) {
        libPath = libspath;
      }
    }

    // check environmenet SIKULI_HOME
    if (libPath == null && sikhomeEnv != null) {
      libspath = FileManager.slashify(sikhomeEnv, true) + "libs";
      if ((new File(libspath)).exists()) {
//TODO this is a hack to check for the new SikuliX - find other solution
        getOS();
        if (checkFileName != null) {
          if ((new File (FileManager.slashify(libspath, true)+checkFileName+".txt")).exists() ||
              (new File (FileManager.slashify(libspath, true)+checkFileName+"32Bit.txt")).exists() ||
              (new File (FileManager.slashify(libspath, true)+checkFileName+"64Bit.txt")).exists()) {
            libPath = libspath;
          }
        }
      }
    }

    // check parent folder of jar file
    if (libPath == null) {
      CodeSource src = Settings.class.getProtectionDomain().getCodeSource();
      if (src != null) {
        String srcParent = (new File(src.getLocation().getPath())).getParent();
        db("jar Location: "+srcParent);
        libsfolder = (new File(srcParent, "libs"));
        if (libsfolder.exists()) {
          db("folder libs found in jar parent folder");
          libPath = libsfolder.getAbsolutePath();
        } else {
          db("no folder libs in jar parent folder");
        }
      }
    }

    // check the working directory and its parent
    if (libPath == null) {
      File wd = new File(System.getProperty("user.dir"));
      File wdp = new File(System.getProperty("user.dir")).getParentFile();
      wd = new File(FileManager.slashify(wd.getAbsolutePath(), true) + libSub);
      wdp = new File(FileManager.slashify(wdp.getAbsolutePath(), true) + libSub);
      if (wd.exists()) {
        libPath = wd.getAbsolutePath();
      } else if (wdp.exists()) {
        libPath = wdp.getAbsolutePath();
      }
      if (libPath == null) {
        wd = new File(FileManager.slashify(System.getProperty("user.home"), true) + libSub);
        if (wd.exists()) {
          libPath = wd.getAbsolutePath();
        }
      }
    }

    // check Mac specific folders
    if (isMac() && libPath == null) {
      if (!(new File(libPathMac)).exists()) {
        libPath = FileManager.slashify("/Applications/" + libSub, true);
      } else {
        libPath = libPathMac;
      }
    }

    // check Windows specific folders
    if (isWindows() && libPath == null) {
      if ((new File(libPathWin)).exists()) {
        libPath = libPathWin;
      } else if ((new File(libPathWin32)).exists()) {
        libPath = libPathWin32;
      }
    }

    // TODO check existence of an extension repository
    SikuliRepo = null;

//TODO set the folder that should contain tessdata
		if (isWindows() || isMac() ) {
			OcrDataPath = libPath;
		} else {
			OcrDataPath = "/usr/local/share";
		}

    // set the version strings
    if (SikuliVersionSub == 0 && SikuliVersionBetaN > 0) {
      SikuliVersion = SikuliVersionBeta;
      SikuliVersionIDE = SikuliVersionBetaIDE;
    } else {
      SikuliVersion = SikuliVersionDefault;
      SikuliVersionIDE = SikuliVersionDefaultIDE;
  	}
    doDebug = false; // to be used as breakpoint
	}

  private static boolean doDebug;
  private static void db(String msg) {
    if (doDebug) {
      System.out.println("[Settings] "+msg);
    }
  }

	public static final int ISWINDOWS = 0;
	public static final int ISMAC = 1;
	public static final int ISLINUX = 2;
	public static final int ISNOTSUPPORTED = 3;
	public static final float FOREVER = Float.POSITIVE_INFINITY;

	public static final int JavaVersion = Integer.parseInt(java.lang.System.getProperty("java.version").substring(2, 3));
	public static final String JREVersion = java.lang.System.getProperty("java.runtime.version");

	public static FindFailedResponse defaultFindFailedResponse = FindFailedResponse.ABORT;
	public static final FindFailedResponse PROMPT = FindFailedResponse.PROMPT;
	public static final FindFailedResponse RETRY = FindFailedResponse.RETRY;
	public static final FindFailedResponse SKIP = FindFailedResponse.SKIP;
	public static final FindFailedResponse ABORT = FindFailedResponse.ABORT;
	public static boolean ThrowException = true; // throw FindFailed exception
	public static float AutoWaitTimeout = 3f; // in seconds
	public static float WaitScanRate = 3f; // frames per second
	public static float ObserveScanRate = 3f; // frames per second
	public static int ObserveMinChangedPixels = 50; // in pixels
	public static double MinSimilarity = 0.7;

	public static float MoveMouseDelay = 0.5f; // in seconds
	public static double DelayBeforeDrop = 0.3;
	public static double DelayAfterDrag = 0.3;

	public static String BundlePath = null;

	/**
	 * in-jar folder to load other ressources from
	 */
	public static String jarResources = "META-INF/res/";
	/**
	 * in-jar folder to load native libs from
	 */
	public static String libSource = "META-INF/libs/";

	public static boolean OcrTextSearch = false;
	public static boolean OcrTextRead = false;

	/**
	 * true = start slow motion mode, false: stop it (default: false)
	 * show a visual for SlowMotionDelay seconds (default: 2)
	 */
	public static boolean ShowActions = false;
	public static float SlowMotionDelay = 2.0f; // in seconds

	/**
	 * true = highlight every match (default: false) (show red rectangle around)
	 * for DefaultHighlightTime seconds (default: 2)
	 */
	public static boolean Highlight = false;
	public static float DefaultHighlightTime = 2f;
	public static float WaitAfterHighlight = 0.3f;

	public static boolean ActionLogs = true;
	public static boolean InfoLogs = true;
	public static boolean DebugLogs = false;
	public static boolean ProfileLogs = false;
	public static boolean LogTime = false;
	public static boolean UserLogs = true;
	public static String UserLogPrefix = "user";
	public static boolean UserLogTime = true;

	/**
	 * default pixels to add around with nearby() and grow()
	 */
	public static final int DefaultPadding = 50;

	static OSUtil osUtil = null;

	public static boolean isJava7() {
		return JavaVersion > 6;
	}

  public static void showJavaInfo() {
    Debug.log(1, "Running on Java " + JavaVersion + " (" + JREVersion + ")");
  }

	public static String getFilePathSeperator() {
		return File.separator;
	}

	public static String getPathSeparator() {
		if (isWindows()) {
			return ";";
		}
		return ":";
	}

	public static String getSikuliDataPath() {
		String home, sikuliPath;
		if (isWindows()) {
			home = System.getenv("APPDATA");
			sikuliPath = "Sikuli";
		} else if (isMac()) {
			home = System.getProperty("user.home")
							+ "/Library/Application Support";
			sikuliPath = "Sikuli";
		} else {
			home = System.getProperty("user.home");
			sikuliPath = ".sikuli";
		}
		File fHome = new File(home, sikuliPath);
		return fHome.getAbsolutePath();
	}

	/**
	 * returns the absolute path to the user's extension path
	 */
	public static String getUserExtPath() {
		String ret = getSikuliDataPath() + File.separator + "extensions";
		File f = new File(ret);
		if (!f.exists()) {
			f.mkdirs();
		}
		return ret;
	}

	public static int getOS() {
		int osRet = ISNOTSUPPORTED;
    checkFileName = null;
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith("mac")) {
			osRet = ISMAC;
      checkFileName = "MadeForMac";
		} else if (os.startsWith("windows")) {
			osRet = ISWINDOWS;
      checkFileName = "MadeForWindows";
		} else if (os.startsWith("linux")) {
			osRet = ISLINUX;
      checkFileName = "MadeForLinux";
		}
		return osRet;
	}

	public static boolean isWindows() {
		return getOS() == ISWINDOWS;
	}

	public static boolean isLinux() {
		return getOS() == ISLINUX;
	}

	public static boolean isMac() {
		return getOS() == ISMAC;
	}

	public static String getOSVersion() {
		return System.getProperty("os.version");
	}

  public static String getVersion() {
    return SikuliVersion;
  }

	static String getOSUtilClass() {
		String pkg = "org.sikuli.system.";
		switch (getOS()) {
			case ISMAC:
				return pkg + "MacUtil";
			case ISWINDOWS:
				return pkg + "WinUtil";
			case ISLINUX:
				return pkg + "LinuxUtil";
			default:
				Debug.error("Warning: Sikuli doesn't fully support your OS.");
				return pkg + "DummyUtil";
		}
	}

	public static OSUtil getOSUtil() {
		if (osUtil == null) {
			try {
				Class c = Class.forName(getOSUtilClass());
				Constructor constr = c.getConstructor();
				osUtil = (OSUtil) constr.newInstance();
			} catch (Exception e) {
				Debug.error("Can't create OS Util: " + e.getMessage());
			}
		}
		return osUtil;
	}

  public static void setArgs(String[] args) {
    Settings.args = args;
  }

  public static String[] getArgs() {
    return Settings.args;
  }

  public static String getTimestamp() {
    return (new Date()).getTime() + "";
}
}
