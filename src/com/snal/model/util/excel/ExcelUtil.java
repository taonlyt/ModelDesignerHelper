/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.util.excel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author luotao
 */
public class ExcelUtil {

    /**
     * 创建表头
     *
     * @param headnames
     * @param sheet
     */
    public static void createTableHeader(String[] headnames, Sheet sheet) {
        Row headrow = sheet.createRow(0);
        for (int i = 0; i < headnames.length; i++) {
            headrow.createCell(i).setCellValue(headnames[i]);
        }
    }

    public static void loadTxtFileToExcel(String txtfilename, String splitstr, String outputfile) {
        try {
            FileInputStream instream = new FileInputStream(txtfilename);
            BufferedReader bufreader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
            String readline = null;
            XSSFWorkbook wkbook = new XSSFWorkbook();
            XSSFSheet sheet = wkbook.createSheet();
            int rowcount = 0;
            while ((readline = bufreader.readLine()) != null) {
                String[] cells = readline.split(splitstr);
                XSSFRow row = sheet.createRow(rowcount++);
                for (int i = 0; i < cells.length; i++) {
                    row.createCell(i).setCellValue(cells[i]);
                }
            }
            ExcelUtil.writeToHighExcelFile(wkbook, outputfile);
        } catch (Exception e) {
            Logger.getLogger(ExcelUtil.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static HSSFSheet readExcelSheet(String filepath, int sheetindex) {
        POIFSFileSystem fs;
        HSSFSheet sheet = null;
        try {
            fs = new POIFSFileSystem(new FileInputStream(filepath));
            //得到Excel工作簿对象      
            HSSFWorkbook wb = new HSSFWorkbook(fs);
            //得到Excel工作表对象      
            sheet = wb.getSheetAt(sheetindex);
        } catch (IOException ex) {
            Logger.getLogger(ExcelUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sheet;
    }

    public static void writeToExcel(HSSFWorkbook workbook, String outfile) {
        try {
            FileOutputStream fout = new FileOutputStream(outfile);
            workbook.write(fout);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static XSSFSheet readHighExcelSheet(String filename, int sheetindx) {
        FileInputStream fs;
        XSSFSheet sheet = null;
        try {
            fs = new FileInputStream(filename);
            //得到Excel工作簿对象      
            XSSFWorkbook wb = new XSSFWorkbook(fs);
            //得到Excel工作表对象      
            sheet = wb.getSheetAt(sheetindx);
            fs.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(ExcelUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sheet;
    }

    public static void writeToHighExcelFile(XSSFWorkbook workbook, String outfile) {
        try {
            FileOutputStream fout = new FileOutputStream(outfile);
            workbook.write(fout);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeLargeDataToExcel(Workbook workbook, String outfile) {
        System.out.println("正在写入文件:" + outfile);
        try {
            FileOutputStream fout = new FileOutputStream(outfile);
            workbook.write(fout);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("处理完成");
    }

    public static String getXCellValueString(XSSFCell cell) {
        if (cell == null) {
            return null;
        }
        String retvalue = null;
        int celltype = cell.getCellType();
        switch (celltype) {
            case XSSFCell.CELL_TYPE_NUMERIC:
                retvalue = String.valueOf((int) cell.getNumericCellValue());
                break;
            case XSSFCell.CELL_TYPE_STRING:
                retvalue = cell.getStringCellValue();
                break;
            case XSSFCell.CELL_TYPE_FORMULA:
                try {
                    retvalue = String.valueOf(cell.getStringCellValue());
                } catch (IllegalStateException e) {
                    retvalue = String.valueOf((int) cell.getNumericCellValue());
                }
                break;
            default:
                retvalue = cell.getStringCellValue();
        }
        retvalue = retvalue != null ? retvalue.trim() : retvalue;
        return retvalue;
    }

    public static String getHCellValueString(HSSFCell cell) {
        if (cell == null) {
            return "";
        }
        String retvalue = null;
        int celltype = cell.getCellType();
        switch (celltype) {
            case XSSFCell.CELL_TYPE_NUMERIC:
                retvalue = String.valueOf((int) cell.getNumericCellValue());
                break;
            case XSSFCell.CELL_TYPE_STRING:
                retvalue = cell.getStringCellValue();
                break;
            case XSSFCell.CELL_TYPE_FORMULA:
                try {
                    retvalue = String.valueOf(cell.getStringCellValue());
                } catch (IllegalStateException e) {
                    retvalue = String.valueOf((int) cell.getNumericCellValue());
                }
                break;
            default:
                retvalue = cell.getStringCellValue();
        }
        return retvalue;
    }
}
