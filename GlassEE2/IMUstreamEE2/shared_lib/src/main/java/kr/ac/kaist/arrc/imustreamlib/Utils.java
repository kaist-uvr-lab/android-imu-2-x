package kr.ac.kaist.arrc.imustreamlib;


/**
 * Created by arrc on 3/29/2018.
 *
 * https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
 */

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Utils {

    /**
     * Convert byte array to hex string
     *
     * @param bytes
     * @return
     */
    private static final String TAG = "Utils";
    private static final String FLODER_NAME = "SelfSync";

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for(int idx=0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     * @param str
     * @return  array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     * @param filename
     * @return
     * @throws IOException
     */
    public static String loadFileAsString(String filename) throws IOException {
        final int BUFLEN=1024;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8=false;
            int read,count=0;
            while((read=is.read(bytes)) != -1) {
                if (count==0 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF ) {
                    isUTF8=true;
                    baos.write(bytes, 3, read-3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count+=read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try{ is.close(); } catch(Exception ex){}
        }
    }



    public static String getDirForDevice(){
        String baseDir_str;
        if(Build.MODEL.contains( "Glass")){
            baseDir_str = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/"+FLODER_NAME+"/";
        }else{
            baseDir_str = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+FLODER_NAME+"/";
        }

        return baseDir_str;
    }
    public static void writeDataCSV(String filename, ArrayList<String> data) {
        Log.v(TAG, "writing data " + filename);

        File outputFile = new File(filename);
        Log.v(TAG, "outputFile: " + outputFile.getAbsolutePath());
        try {
//			FileUtils.writeLines(outputFile, data);
            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String line : data) {
                bw.write(line);
                bw.newLine();
            }
            bw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }

        if (data != null) {
            data.clear();
        }
        Log.d(TAG, "data write successful: "+ filename);
    }
    public static void writeDataCSV(String filename, ArrayList<String> data, String data_column) {
        Log.v(TAG, "writing data " + filename);

        File outputFile = new File(filename);
        Log.v(TAG, "outputFile: " + outputFile.getAbsolutePath());
        try {
//			FileUtils.writeLines(outputFile, data);
            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(data_column);
            bw.newLine();
            for (String line : data) {
                bw.write(line);
                bw.newLine();
            }
            bw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }

        if (data != null) {
            data.clear();
        }
        Log.d(TAG, "data write successful: "+ filename);
    }
    public static void appendStringToThisFile(String filename, String contents){

        File fnew = new File(filename);
        try {
            FileWriter f2 = new FileWriter(fnew, true);
            f2.write(contents);
            f2.close();
            Log.d(TAG, "data write successful: "+filename);
        } catch (IOException e) {
            Log.e(TAG, "appendStringToThisFile");
            e.printStackTrace();
        }
    }
    public static String getDeviceInfo(){
        return  Build.MANUFACTURER
                + " " + Build.MODEL + " " + Build.VERSION.RELEASE
                + " " + Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName();
    }







}