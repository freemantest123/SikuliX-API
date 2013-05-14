package org.sikuli.script;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;


public class FileManager {

  private static File jniDir = null;
  private static String jarResources = Settings.jarResources;
	private static String libSource = Settings.libSource;
  private static final ArrayList<String> libPaths = new ArrayList<String>();
  private static StringBuffer alreadyLoaded = new StringBuffer("");
	static final int DOWNLOAD_BUFFER_SIZE = 153600;
  private static List<String> libsList = new ArrayList<String>();
  private static ClassLoader cl;

  static {
    cl = FileManager.class.getClassLoader();
    //TODO extractLibs();
  }

  /**
   * System.load() the given library module
   * <br /> from standard places (SikuliX/libs)
   * in the following order<br />
   * 1. -Dsikuli.Home=<br />
   * 2. Environement SIKULI_HOME<br />
   * 3. current working dir<br />
   * 4. parent of current working dir<br />
   * 5. folder user's home (user.home)<br />
   * 6. standard installation places of Sikuli<br />
   *
   * @param libname
   * @param doLoad = true: load it here
   * @throws IOException
   */
  public static void loadLibrary(String libname) {
    if (libPaths.size() == 0) {
      libPaths.add(Settings.libPath);
    }
    File lib = null;
    boolean libFound = false;
    try {
      lib = extractJni(libname);
      if (lib == null) {
        Debug.log(2, "Native library already loaded: " + libname);
        return;
      }
      Debug.log(2, "Native library found: " + libname);
      libFound = true;
    } catch (IOException ex) {
      Debug.error(FileManager.class.getName() + ".loadLibrary: Native library could not be extracted nor found: " + libname);
      System.exit(1);
    }
    try {
      System.load(lib.getAbsolutePath());
    } catch (Error e) {
      Debug.error(FileManager.class.getName() + ".loadLibrary: Native library could not be loaded: " + libname);
      if (libFound) {
        Debug.error("Since native library was found, it might be a problem with needed dependent libraries");
				e.printStackTrace();
      }
      System.exit(1);
    }
    Debug.log(2, "Native library loaded: " + libname);
  }

  /**
   * extract a JNI library from the classpath <br /> Mac: default is .jnilib
   * (.dylib as fallback)
   *
   * @param libname System.loadLibrary() compatible library name
   * @return the extracted File object
   * @throws IOException
   */
  private static File extractJni(String libname) throws IOException {
    if(alreadyLoaded.indexOf("*"+libname)<0) {
      alreadyLoaded.append("*"+libname);
    } else {
      return null;
    }
    String mappedlib = System.mapLibraryName(libname);
    File outfile = new File(getJniDir(), mappedlib);

    /* on darwin, the default mapping is to .jnilib; but
     * if we don't find a .jnilib, try .dylib instead.
     */
    if (!outfile.exists()) {
			URL res = cl.getResource(libSource + mappedlib);
      if (res == null) {
        if (mappedlib.endsWith(".jnilib")) {
          mappedlib = mappedlib.substring(0, mappedlib.length() - 7) + ".dylib";
          String jnilib = mappedlib.toString();
          outfile = new File(getJniDir(), jnilib);
          if (!outfile.exists()) {
            if (cl.getResource(libSource + mappedlib) == null) {
              throw new IOException("Library " + mappedlib + " not on classpath nor in default location");
            } // else copy lib from jar
          } else {
            return outfile;
          }
        } else {
          throw new IOException("Library " + mappedlib + " not on classpath nor in default location");
        }
      } // else copy lib from jar
    } else {
      return outfile;
    }
    File ret = extractJniResource(libSource + mappedlib, outfile);
    return ret;
  }

  /**
   * Gets the working directory to use for jni extraction. <br /> standard
   * locations: <br /> Mac: "/Applications/Sikuli-IDE.app/Contents/libs"
   * <br /> Windows / Linux & Mac (environment): %SIKULI_HOME% / $SIKULI_HOME
   * <br /> if not exists: java.io.tmp/tmplib
   *
   * @return jni working dir
   * @throws IOException if there's a problem creating the dir
   */
  private static File getJniDir() throws IOException {
    if (jniDir == null) {
      Debug.log(2, "Checking JNI library working directory");
      for (String path : libPaths) {
        if (path == null || "".equals(path)) {
          continue;
        }
        jniDir = new File(path);
        if (jniDir.exists()) {
          System.setProperty("java.library.tmpdir", path);
          Debug.log(2, "Using as JNI library working directory: '" + jniDir + "'");
          Debug.log(2, "Using JNI directory as OCR directory (tessdata) too");
          Settings.OcrDataPath = jniDir.getAbsolutePath();
          return jniDir;
        }
      }
      String libTmpDir = System.getProperty("java.io.tmpdir") + "/tmplib";
      System.setProperty("java.library.tmpdir", libTmpDir);
      jniDir = new File(libTmpDir);
      Debug.log(2, "Initialised JNI library working directory to '" + jniDir + "'");
    }

    if (!jniDir.exists()) {
      if (!jniDir.mkdirs()) {
        throw new IOException("Unable to create JNI library working directory " + jniDir);
      }
    }
    return jniDir;
  }

  /**
   * extract a resource to a writable file
   *
   * @param resourcename the name of the resource on the classpath
   * @param outputfile the file to copy to
   * @return the extracted file
   * @throws IOException
   */
  private static File extractJniResource(String resourcename, File outputfile) throws IOException {
    InputStream in = cl.getResourceAsStream(resourcename);
    if (in == null) {
      throw new IOException("Resource " + resourcename + " not on classpath");
    }
    Debug.log(2, "Extracting '" + resourcename + "' to '" + outputfile.getAbsolutePath() + "'");
    OutputStream out = new FileOutputStream(outputfile);
    copy(in, out);
    out.close();
    in.close();
    return outputfile;
  }

  /**
   * extract a resource to an absolute path
   *
   * @param resourcename the name of the resource on the classpath
   * @param outputname the path of the file to copy to
   * @return the extracted file
   * @throws IOException
   */
  private static File extractJniResource(String resourcename, String outputname) throws IOException {
    return extractJniResource(resourcename, new File(outputname));
  }

  private static void extractLibs() {
    Debug.log(2, "FileManager: trying to acces jar");
    CodeSource src = FileManager.class.getProtectionDomain().getCodeSource();
    int iDir=0, iFile=0;
    if (src != null) {
      URL jar = src.getLocation();
      if (! jar.toString().endsWith(".jar")) {
        Debug.log(2,"FileManager: not running from jar");
      } else {
        try {
          ZipInputStream zip = new ZipInputStream(jar.openStream());
          ZipEntry ze;
          Debug.log(2, "FileManager: accessing jar: " + jar.toString());
          while ((ze = zip.getNextEntry()) != null) {
            String entryName = ze.getName();
            if (entryName.startsWith("META-INF/libs")) {
              //Debug.log(2, "FileManager: "+entryName);
              libsList.add( entryName  );
              if (entryName.endsWith(File.separator)) {
                iDir++;
              } else {
                iFile++;
              }
            }
          }
          Debug.log(2, "FileManager: found in META-INF/libs: Dirs: "+iDir+" Files: "+iFile );
        } catch (IOException e) {
          Debug.error("FileManager: List jar did not work");
        }
      }
    } else {
      Debug.error("FileManager: cannot access jar");
    }
  }

  /**
	 * Assume the list of resources can be found at path/filelist.txt
   *
	 * @return the local path to the extracted resources
	 */
	public static String extract(String path) throws IOException {
		InputStream in = cl.getResourceAsStream(path + "/filelist.txt");
		String localPath = System.getProperty("java.io.tmpdir") + "/sikuli/" + path;
		new File(localPath).mkdirs();
		Debug.log(4, "extract resources " + path + " to " + localPath);
		writeFileList(in, path, localPath);
		return localPath + "/";
	}

  private static void writeFile(String from, String to) throws IOException {
      Debug.log(7, "FileManager: JarResource: copy " + from + " to "+ to);
			File toF = new File(to);
			toF.getParentFile().mkdirs();
			InputStream in = cl.getResourceAsStream(from);
			if (in != null) {
				OutputStream out = null;
				try {
					out = new FileOutputStream(toF);
					copy(in, out);
				} catch (IOException e) {
					Debug.log(7, "FileManager: JarResource: Can't extract " + from + ": " + e.getMessage());
				} finally {
					if (out != null) {
						out.close();
					}
				}
			} else {
				Debug.log(7, "FileManager: JarResource: not found: " + from);
			}
  }

  private static void writeFileList(InputStream ins, String fromPath, String outPath) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(ins));
		String line;
		while ((line = r.readLine()) != null) {
			Debug.log(7, "write " + line);
			if (line.startsWith("./")) {
				line = line.substring(1);
			}
			String fullpath = outPath + line;
			File outf = new File(fullpath);
			outf.getParentFile().mkdirs();
			InputStream in = cl.getResourceAsStream(fromPath + line);
			if (in != null) {
				OutputStream out = null;
				try {
					out = new FileOutputStream(outf);
					copy(in, out);
				} catch (IOException e) {
					Debug.log("Can't extract " + fromPath + line + ": " + e.getMessage());
				} finally {
					if (out != null) {
						out.close();
					}
				}
			} else {
				Debug.log("Resource not found: " + fromPath + line);
			}
		}
	}

	/**
   * copy an InputStream to an OutputStream.
   *
   * @param in InputStream to copy from
   * @param out OutputStream to copy to
   * @throws IOException if there's an error
   */
  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] tmp = new byte[8192];
    int len = 0;
    while (true) {
      len = in.read(tmp);
      if (len <= 0) {
        break;
      }
      out.write(tmp, 0, len);
    }
  }

	public static String downloadURL(URL url, String localPath) throws IOException {
		InputStream reader = url.openStream();
		String[] path = url.getPath().split("/");
		String filename = path[path.length - 1];
		File fullpath = new File(localPath, filename);
		FileOutputStream writer = new FileOutputStream(fullpath);
		byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
		int totalBytesRead = 0;
		int bytesRead = 0;
		while ((bytesRead = reader.read(buffer)) > 0) {
			writer.write(buffer, 0, bytesRead);
			totalBytesRead += bytesRead;
		}
		reader.close();
		writer.close();
		return fullpath.getAbsolutePath();
	}

	public static String unzipSKL(String fileName) {
		File file;
	      try {
          file = new File(fileName);
          if (!file.exists()) {
            throw new IOException(fileName + ": No such file");
          }
          String name = file.getName();
          name = name.substring(0, name.lastIndexOf('.'));
          File tmpDir = createTempDir();
          File sikuliDir = new File(tmpDir + File.separator + name + ".sikuli");
          sikuliDir.mkdir();
          unzip(fileName, sikuliDir.getAbsolutePath());
          return sikuliDir.getAbsolutePath();
				} catch (IOException e) {
					System.err.println(e.getMessage());
					return null;
				}
	}

	public static File createTempDir() {
		final String baseTempPath = System.getProperty("java.io.tmpdir");

		Random rand = new Random();
		int randomInt = 1 + rand.nextInt();

		File tempDir = new File(baseTempPath + File.separator + "tmp-" + randomInt + ".sikuli");
		if (tempDir.exists() == false) {
			tempDir.mkdir();
		}

		tempDir.deleteOnExit();

    Debug.log(2, "FileManager: tempdir create: %s", tempDir);

		return tempDir;
	}

  public static void deleteTempDir(String path) {
    File fpath = new File(path);
    String[] files = fpath.list();
    if (files != null) {
      for (String fname : files) {
        (new File(fpath, fname)).delete();
      }
    }
    fpath.delete();
    if (fpath.exists()) {
      Debug.log(2, "FileManager: tempdir delete not possible: %s", fpath);
    } else {
      Debug.log(2, "FileManager: tempdir delete: %s", fpath);
    }
  }

  public static File createTempFile(String suffix) {
    return createTempFile(suffix, null);
  }

  public static File createTempFile(String suffix, String path) {
    String temp1 = "sikuli-";
    String temp2 = "." + suffix;
    File fpath = null;
    if (path != null) {
      fpath = new File(path);
    }
    try {
      File temp = File.createTempFile(temp1, temp2, fpath);
      temp.deleteOnExit();
      Debug.log(2, "FileManager: tempfile create: %s", temp.getAbsolutePath());
      return temp;
    } catch (IOException ex) {
      Debug.error("FileManager.createTempFile: IOException: %s", fpath + File.pathSeparator + temp1 + "12....56" + temp2);
      return null;
    }
  }

  public static String saveTmpImage(BufferedImage img) {
    return saveTmpImage(img, null);
  }

  public static String saveTmpImage(BufferedImage img, String path) {
    File tempFile;
    try {
      tempFile = createTempFile("png", path);
      if (tempFile != null) {
        ImageIO.write(img, "png", tempFile);
        return tempFile.getAbsolutePath();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

	public static void unzip(String zip, String path)
					throws IOException, FileNotFoundException {
		final int BUF_SIZE = 2048;
		FileInputStream fis = new FileInputStream(zip);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			int count;
			byte data[] = new byte[BUF_SIZE];
			FileOutputStream fos = new FileOutputStream(
							new File(path, entry.getName()));
			BufferedOutputStream dest = new BufferedOutputStream(fos, BUF_SIZE);
			while ((count = zis.read(data, 0, BUF_SIZE)) != -1) {
				dest.write(data, 0, count);
			}
			dest.close();
		}
		zis.close();
	}

  public static void openURL(String url) {
    try {
      URL u = new URL(url);
      Desktop.getDesktop().browse(u.toURI());
    } catch (Exception ex) {
      ex.printStackTrace();
}
  }

  public static void xcopy(String src, String dest, String current) throws IOException {
    File fSrc = new File(src);
    File fDest = new File(dest);
    if (fSrc.getAbsolutePath().equals(fDest.getAbsolutePath())) {
      return;
    }
    if (fSrc.isDirectory()) {
      if (!fDest.exists()) {
        fDest.mkdir();
      }
      String[] children = fSrc.list();
      for (String child : children) {
        if (current != null && (child.endsWith(".py") || child.endsWith(".html"))
                && child.startsWith(current + ".")) {
          Debug.log(2, "SaveAs: deleting %s", child);
          continue;
        }
        xcopy(src + File.separator + child, dest + File.separator + child, null);
      }
    } else {
      if (fDest.isDirectory()) {
        dest += File.separator + fSrc.getName();
      }
      InputStream in = new FileInputStream(src);
      OutputStream out = new FileOutputStream(dest);
      // Copy the bits from instream to outstream
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
    }
  }


  /**
   * Copy a file *src* to the path *dest* and check if the file name conflicts.
   * If a file with the same name exists in that path, rename *src* to an
   * alternative name.
   */
  public static File smartCopy(String src, String dest) throws IOException {
    File fSrc = new File(src);
    String newName = fSrc.getName();
    File fDest = new File(dest, newName);
    if (fSrc.equals(fDest)) {
      return fDest;
    }
    while (fDest.exists()) {
      newName = getAltFilename(newName);
      fDest = new File(dest, newName);
    }
    FileManager.xcopy(src, fDest.getAbsolutePath(), null);
    if (fDest.exists()) {
      return fDest;
    }
    return null;
  }

  public static String convertStreamToString(InputStream is) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return sb.toString();
  }

  public static String getAltFilename(String filename) {
    int pDot = filename.lastIndexOf('.');
    int pDash = filename.lastIndexOf('-');
    int ver = 1;
    String postfix = filename.substring(pDot);
    String name;
    if (pDash >= 0) {
      name = filename.substring(0, pDash);
      ver = Integer.parseInt(filename.substring(pDash + 1, pDot));
      ver++;
    } else {
      name = filename.substring(0, pDot);
    }
    return name + "-" + ver + postfix;
  }

  public static boolean exists(String path) {
    File f = new File(path);
    return f.exists();
  }

  public static void mkdir(String path) {
    File f = new File(path);
    if (!f.exists()) {
      f.mkdir();
    }
  }

  public static String getName(String filename) {
    File f = new File(filename);
    return f.getName();
  }

  public static String slashify(String path, boolean isDirectory) {
    String p;
    if (path == null) {
      p = "";
    } else {
      p = path;
      if (File.separatorChar != '/') {
        p = p.replace(File.separatorChar, '/');
      }
      if (isDirectory) {
        if (!p.endsWith("/")) {
          p = p + "/";
        }
      } else if (p.endsWith("/")) {
        p = p.substring(0, p.length() - 1);
      }
    }
    return p;
  }
}
