package com.tumri.af.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {
	    private static Logger log = LogManager.getLogger(FileUtils.class);
	    
	    private static final int BUF_SIZE = 256*1024;  // 256 K
	    
	    /** Closes the object if it is not null.
	     * If closing the object throws an exception, the exception is logged.
	     * @param c The thing to close.
	     */
	    public static void close(Closeable c) {
	    	if(c != null) {
	    		try {
	    			c.close();
	    		} catch(IOException ioe) {
	    			log.error("Error closing file:", ioe);
	    		}
	    	}
	    }
	    
	    public static void makeDir(String dir) throws Exception {
	        File dirObj = new File(dir);
	        dirObj.mkdirs();
	    }

	    public static boolean exists(String path) {
	        File dir = new File(path);

	        if (dir.exists()) {
	            return true;
	        }
	        return false;
	    }
	    
	    public static String makeZip(String srcDir, String zipFilename) {
	        File srcFolder = new File(srcDir);
	        File zipFile = new File(zipFilename);
	        ZipOutputStream zos=null;
	        try {
	           zos = new ZipOutputStream(new  BufferedOutputStream(new FileOutputStream(zipFile)));
	           zipDir(srcDir, zos, srcFolder.getName());
	           zos.close();
	        }
	        catch(Exception e){        	
	        	throw new RuntimeException("Exception in constructing ZipOutputStream Object");
	        }
	        
	        return zipFile.getName();
	    }
	    
	    public static void zipDir(String dir2zip, ZipOutputStream zos, String parent) {
           	
	        try {
	            //create a new File object based on the directory we 
	            //have to zip File    
	            File zipDir = new File(dir2zip);

	            //get a listing of the directory content 
	            String[] dirList = zipDir.list();
	            byte[] readBuffer = new byte[BUF_SIZE];
	            int bytesIn = 0;

	            //loop through dirList, and zip the files 
	            for (int i = 0; i < dirList.length; i++) {
	                File f = new File(zipDir, dirList[i]);

	                if (f.isDirectory()) {
	                    //if the File object is a directory, call this 
	                    //function again to add its content recursively 
	                    String filePath = f.getPath();
	                    zipDir(filePath, zos, parent + File.separator +
	                        f.getName());

	                    //loop again 
	                    continue;
	                }
	                
	                //if we reached here, the File object f was not 
	                //a directory 
	                //create a FileInputStream on top of f 
	                FileInputStream fis = new FileInputStream(f);

	                //create a new zip entry               
	                ZipEntry anEntry = new ZipEntry(parent + File.separator +
	                        f.getName());
	                //place the zip entry in the ZipOutputStream object 
	                zos.putNextEntry(anEntry);

	                //now write the content of the file to the ZipOutputStream 
	                while ((bytesIn = fis.read(readBuffer)) != -1) {
	                    zos.write(readBuffer, 0, bytesIn);
	                }

	                //close the Stream 
	                fis.close();               
	            }
	        } catch (Exception e) {
	        	e.printStackTrace();
	            //handle exception 
	        }
	    }

	    public static void deleteDir(File dirObj) throws Exception {
	        if (!dirObj.exists()) {
	            //nothing to do
	            return;
	        }

	        File[] children = dirObj.listFiles();

	        if (children != null) {
	            for (File file : children) {
	                deleteDir(file);
	            }
	        }

	        dirObj.delete();
	    }
	    
	    public static void deleteDir(String dir) throws Exception {
	        File dirObj = new File(dir);
	        deleteDir(dirObj);
	    }

        public static void deleteFolderContents(File dirObj) throws Exception {
            if (!dirObj.exists()) {
                //nothing to do
                return;
            }

            File[] children = dirObj.listFiles();

            if (children != null) {
                for (File file : children) {
                    deleteDir(file);
                }
            }
        }

        public static void deleteFolderContents(String dir) throws Exception {
            File dirObj = new File(dir);
            deleteFolderContents(dirObj);
        }

        /** Copies the contents of the specified file to the destination.
         * Overwrites the file at the specified destination if it already exists.
         * @param sourcePath The file path of the source file.
         * @param destPath The path of the destination file.
         * @exception IOException If error copying the file.
         */
        public static void copyFile(String sourcePath, String destPath) throws IOException {
        	InputStream in = null;
        	OutputStream out = null;
        	try {
        		in = new FileInputStream(sourcePath);
        		out = new FileOutputStream(destPath);
        		copyStream(in, out);
        	} finally {
        		close(out);
        		close(in);
        	}
        }
        
        /** Copies the contents of the specified file to the destination.
         * Overwrites the file at the specified destination if it already exists.
         * @param src The source file.
         * @param dest The destination file.
         * @exception IOException If error copying the file.
         */
        public static void copyFile(File src, File dest) throws IOException {
        	InputStream in = null;
        	OutputStream out = null;
        	try {
        		in = new FileInputStream(src);
        		out = new FileOutputStream(dest);
        		copyStream(in, out);
        	} finally {
        		close(out);
        		close(in);
        	}
        }
        
    	/** Copies the contents of the input stream to the output stream.
    	 * @param in The input stream (assumed not null).
    	 * @param out The output stream (assumed not null).
    	 * @exception IOException If error reading or writing.
    	 */
        public static void copyStream(InputStream in, OutputStream out) throws IOException {
        	byte[] buf = new byte[BUF_SIZE];
        	for(int n = in.read(buf); n > 0; n = in.read(buf)) {
        		out.write(buf, 0, n);
        	}
        }
	}