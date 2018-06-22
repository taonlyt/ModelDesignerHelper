/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.util.excel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * 读取大Excel数据
 *
 * @author luotao
 */
public class BigExcelUtil {

    public static void main(String[] args) throws IOException {
        BigExcelUtil util = new BigExcelUtil();
        int[] minColumns = {7};
        writeToExcel(null);
        //util.readExcelData("E:/模型主题域设置.xlsx", 0, minColumns);
    }

    /**
     * 读取Excel文件，将每个工作表的数据存入Map中。 key：工作表索引 value：工作表数据集合
     *
     * @param exclfile
     * @param startSheet
     * @param endSheet
     * @param sheetMinColumns
     * @return
     */
    public Map<String, List<List<String>>> readExcelData(String exclfile, int startSheet, int endSheet, int[] sheetMinColumns) {
        Map<String, List<List<String>>> sheetmap = null;
        try {
            File xlsxFile = new File(exclfile);
            if (!xlsxFile.exists()) {
                System.err.println("没有找到文件: " + xlsxFile.getPath());
            }
            OPCPackage p = OPCPackage.open(xlsxFile.getPath(), PackageAccess.READ);
            sheetmap = readAllExcelSheet(p, startSheet, endSheet, sheetMinColumns);
        } catch (IOException | OpenXML4JException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(BigExcelUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sheetmap;
    }

    /**
     * 读取一个工作表格中所有行。
     *
     * @param styles
     * @param strings
     * @param sheetInputStream
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private List<List<String>> readOneSheet(StylesTable styles, ReadOnlySharedStringsTable strings, InputStream sheetInputStream,
            int minColumns) throws IOException, ParserConfigurationException, SAXException {

        InputSource sheetSource = new InputSource(sheetInputStream);
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxFactory.newSAXParser();
        XMLReader sheetParser = saxParser.getXMLReader();
        List<List<String>> sheetrows = new ArrayList();//容器：装载工作表中所有行。
        ContentHandler handler = new ExcelSheetHandler(styles, strings, sheetrows, minColumns);
        sheetParser.setContentHandler(handler);
        sheetParser.parse(sheetSource);
        return sheetrows;
    }

    /**
     * 读取Excel文件中所有工作表
     *
     * @return @throws IOException
     * @throws OpenXML4JException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private Map<String, List<List<String>>> readAllExcelSheet(OPCPackage pkg, int startSheet, int endSheet, int[] sheetMinColumns) throws IOException, OpenXML4JException, ParserConfigurationException, SAXException {

        Map<String, List<List<String>>> sheetmap = new HashMap();
        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
        XSSFReader xssfReader = new XSSFReader(pkg);

        StylesTable styles = xssfReader.getStylesTable();
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        int index = 0;
        while (iter.hasNext()) {
            if (index < startSheet || index > endSheet) {
                break;
            }
            InputStream stream = iter.next();
            String sheetName = iter.getSheetName();
            List<List<String>> sheetrows = readOneSheet(styles, strings, stream, sheetMinColumns[index]);
            stream.close();
            sheetmap.put(String.valueOf(index), sheetrows);
            index++;
        }
        return sheetmap;
    }

    /**
     * @deprecated 
     * @param dist
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void writeToExcel(String dist) throws FileNotFoundException, IOException {
        Workbook workbook = new SXSSFWorkbook();//最重要的就是使用SXSSFWorkbook，表示流的方式进行操作
        Sheet sheet = workbook.createSheet();
        int rows = 500000;
        int cols = 20;
        for (int row = 0; row < rows; row++) {
            Row writeRow = sheet.createRow(row);
            for (int col = 0; col < cols; col++) {
                org.apache.poi.ss.usermodel.Cell cell = writeRow.createCell(col);
                //cell.setCellStyle(cellStyle);
                cell.setCellValue(row + "-" + col);
            }
        }

        FileOutputStream stream = new FileOutputStream("d:/x.xlsx");
        workbook.write(stream);//最终写入文件
        stream.close();
    }
}
