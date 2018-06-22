/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.util.text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author luotao
 */
public class TextUtil {

  
    public static List<String> readTxtFileToList(String filename, boolean doDistinct) {
        List dslist = new ArrayList();
        try {
            FileInputStream instream = new FileInputStream(filename);
            BufferedReader bufreader = new BufferedReader(new InputStreamReader(instream, "UTF-8"), 50 * 1024 * 1024);
            String readline = null;
            while ((readline = bufreader.readLine()) != null) {
                if (!doDistinct) {
                    dslist.add(readline);
                } else if (!dslist.contains(readline)) {
                    dslist.add(readline);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dslist;
    }
  public static String readTxtFile(String filename, boolean doDistinct) {
        StringBuilder strbffer = new StringBuilder();
        try {
            FileInputStream instream = new FileInputStream(filename);
            byte[] buffer = new byte[1024];
            int readLenth = 0;
            BufferedReader bufreader = new BufferedReader(new InputStreamReader(instream, "UTF-8"), 50 * 1024 * 1024);
            String readline = null;
            while ((readLenth = instream.read(buffer)) != -1) {
                String tem = new String(buffer);
                strbffer.append(tem);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strbffer.toString();
    }
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static void writeToFile(String script, String filename) throws IOException {
        BufferedWriter bufwriter = null;
        try {
            OutputStreamWriter writerStream = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
            bufwriter = new BufferedWriter(writerStream);
            bufwriter.write(script);
            bufwriter.newLine();
            bufwriter.close();
            System.out.println("输出结果：" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufwriter != null) {
                bufwriter.close();
            }
        }
    }
}
