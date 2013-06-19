/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

public class FileManager {
  private static String jarResources = Settings.jarResources;
  static final int DOWNLOAD_BUFFER_SIZE = 153600;
  static INativeLoader nativeLoader = null;


  /**
   * System.load() the given library module <br /> from standard places (SikuliX/libs) in the
   * following order<br /> 1. -Dsikuli.Home=<br /> 2. Environement SIKULI_HOME<br /> 3. current
   * working dir<br /> 4. parent of current working dir<br /> 5. folder user's home (user.home)<br
   * /> 6. standard installation places of Sikuli<br />
   *
   * @param libname
   * @param doLoad = true: load it here
   * @throws IOException
   */
  public static void loadLibrary(String libname) {
    if (nativeLoader == null) {
      nativeLoader = getNativeLoader("basic", null);
    }
    nativeLoader.check(nativeLoader.getLibType());
    nativeLoader.export(libname+nativeLoader.getLibType(), null);
  }

  /**
   * copy an InputStream to an OutputStream.
   *
   * @param in InputStream to copy from
   * @param out OutputStream to copy to
   * @throws IOException if there's an error
   */
  public static void copy(InputStream in, OutputStream out) throws IOException {
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
    Random rand = new Random();
    int randomInt = 1 + rand.nextInt();

    File tempDir = new File(Settings.BaseTempPath + File.separator + "tmp-" + randomInt + ".sikuli");
    if (tempDir.exists() == false) {
      tempDir.mkdirs();
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
   * Copy a file *src* to the path *dest* and check if the file name conflicts. If a file with the
   * same name exists in that path, rename *src* to an alternative name.
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
    try {
      return URLDecoder.decode(p, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      return p;
    }
  }

  /**
   * Retrieves the actual script file<br /> - from a folder script.sikuli<br /> - from a folder
   * script (no extension) (script.sikuli is used, if exists)<br /> - from a file script.skl or
   * script.zip (after unzipping to temp)<br /> - from a jar script.jar (after preparing as
   * extension)<br />
   *
   * @param scriptName one of the above.
   * @return The File containing the actual script.
   */
  public static File getScriptFile(File scriptName, IScriptRunner runner, String[] args) {
    if (scriptName == null) {
      return null;
    }
    String script;
    String scriptType;
    File scriptFile = null;
    if (scriptName.getPath().contains("..")) {
      //TODO accept double-dot pathnames
      Debug.error("Sorry, scriptnames with dot or double-dot path elements are not supported: %s", scriptName.getPath());
      System.exit(1);
    }
    int pos = scriptName.getName().lastIndexOf(".");
    if (pos == -1) {
      script = scriptName.getName();
      scriptType = "sikuli";
      if ((new File(scriptName.getAbsolutePath() + ".sikuli")).exists()) {
        scriptName = new File(scriptName.getAbsolutePath() + ".sikuli");
      }
    } else {
      script = scriptName.getName().substring(0, pos);
      scriptType = scriptName.getName().substring(pos + 1);
    }
    if ("sikuli".equals(scriptType)) {
      if (runner == null) {
        // check for script.xxx inside folder
        File[] content = scriptName.listFiles(new FileFilterScript(script+"."));
        if (content == null || content.length == 0) {
          Debug.error("Unable to get ScriptRunner from a contained file's file-ending named %s.xxx", script);
          System.exit(1);
        }
        scriptFile = content[0];
        scriptType = scriptFile.getName().substring(scriptFile.getName().lastIndexOf(".")+1);
        runner = SikuliScript.setRunner(SikuliScript.getScriptRunner(null, scriptType, args));
      }
      if (scriptFile == null) {
        // try with fileending
        scriptFile = (new File(scriptName, script + "." + runner.getFileEndings()[0])).getAbsoluteFile();
        if (!scriptFile.exists() || scriptFile.isDirectory()) {
          // try without fileending
          scriptFile = new File(scriptName, script);
          if (!scriptFile.exists() || scriptFile.isDirectory()) {
            Debug.error("No runnable script found in %s", scriptFile.getAbsolutePath());
            return null;
          }
        }
      }
    } else if ("skl".equals(scriptType) || "zip".equals(scriptType)) {
      //TODO unzip to temp and run from there
      return null; // until ready
    } else if ("jar".equals(scriptType)) {
      //TODO try to load and run as extension
      return null; // until ready
    }
    return scriptFile;
  }

  /**
   * Returns the directory that contains the images used by the ScriptRunner.
   *
   * @param scriptFile The file containing the script.
   * @return The directory containing the images.
   */
  public static File resolveImagePath(File scriptFile) {
    if (!scriptFile.isDirectory()) {
      return scriptFile.getParentFile();
    }
    return scriptFile;
  }

  private static class FileFilterScript implements FilenameFilter {

    private String _check;

    public FileFilterScript(String check) {
      _check = check;
    }

    @Override
    public boolean accept(File dir, String fileName) {
      return fileName.startsWith(_check);
    }
  }
  
  public static INativeLoader getNativeLoader(String name, String[] args) {
    INativeLoader nativeLoader = null;
    ServiceLoader<INativeLoader> loader = ServiceLoader.load(INativeLoader.class);
    Iterator<INativeLoader> scriptRunnerIterator = loader.iterator();
    while (scriptRunnerIterator.hasNext()) {
      INativeLoader currentRunner = scriptRunnerIterator.next();
      if ((name != null && currentRunner.getName().toLowerCase().equals(name.toLowerCase()))) {
        nativeLoader = currentRunner;
        nativeLoader.init(args);
        break;
      }
    }
    if (nativeLoader == null) {
      Debug.error("Could not load any NativeLoader!");
      System.exit(1);
    }
    return nativeLoader;
  }  
}
