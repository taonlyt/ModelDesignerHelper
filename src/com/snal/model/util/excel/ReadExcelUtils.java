/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.util.excel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 基于XSSF and SAX (Event API) 读取excel的第一个Sheet的内容
 *
 * @author luotao
 *
 */
public class ReadExcelUtils {

    private int headCount = 0;
    private List<List<String>> list = new ArrayList<List<String>>();

    /**
     * 通过文件流构建DOM进行解析
     *
     * @param ins
     * @param headRowCount 跳过读取的表头的行数
     * @return
     * @throws InvalidFormatException
     * @throws IOException
     */
    public List<List<String>> processDOMReadSheet(InputStream ins, int headRowCount) throws InvalidFormatException, IOException {
        Workbook workbook = WorkbookFactory.create(ins);
        return this.processDOMRead(workbook, headRowCount);
    }

    /**
     * 采用DOM的形式进行解析
     *
     * @param filename
     * @param headRowCount 跳过读取的表头的行数
     * @return
     * @throws IOException
     * @throws InvalidFormatException
     * @throws Exception
     */
    public List<List<String>> processDOMReadSheet(String filename, int headRowCount) throws InvalidFormatException, IOException {
        Workbook workbook = WorkbookFactory.create(new File(filename));
        return this.processDOMRead(workbook, headRowCount);
    }

    /**
     * 采用SAX进行解析
     *
     * @param filename
     * @param headRowCount
     * @return
     * @throws OpenXML4JException
     * @throws IOException
     * @throws SAXException
     * @throws Exception
     */
    public List<List<String>> processSAXReadSheet(String filename, int headRowCount) throws IOException, OpenXML4JException, SAXException {
        headCount = headRowCount;

        OPCPackage pkg = OPCPackage.open(filename);
        XSSFReader r = new XSSFReader(pkg);
        SharedStringsTable sst = r.getSharedStringsTable();
        XMLReader parser = fetchSheetParser(sst);

        Iterator<InputStream> sheets = r.getSheetsData();
        InputStream sheet = sheets.next();
        InputSource sheetSource = new InputSource(sheet);
        parser.parse(sheetSource);
        sheet.close();

        return list;
    }

    private XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException {
        XMLReader parser
                = XMLReaderFactory.createXMLReader(
                        "org.apache.xerces.parsers.SAXParser"
                );
        ContentHandler handler = new SheetHandler(sst);
        parser.setContentHandler(handler);
        return parser;
    }

    /**
     * SAX 解析excel
     */
    private class SheetHandler extends DefaultHandler {

        private SharedStringsTable sst;
        private String lastContents;
        private boolean nextIsString;
        private boolean isNullCell;
        //读取行的索引
        private int rowIndex = 0;
        //是否重新开始了一行
        private boolean curRow = false;
        private List<String> rowContent;

        private SheetHandler(SharedStringsTable sst) {
            this.sst = sst;
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            //节点的类型
            if (name.equals("row")) {
                rowIndex++;
            }
            //表头的行直接跳过
            if (rowIndex > headCount) {
                curRow = true;
                // c => cell
                if (name.equals("c")) {
                    String cellType = attributes.getValue("t");
                    if (null == cellType) {
                        isNullCell = true;
                    } else {
                        if (cellType.equals("s")) {
                            nextIsString = true;
                        } else {
                            nextIsString = false;
                        }
                        isNullCell = false;
                    }
                }
                lastContents = "";
            }
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            //System.out.println("-------end："+name);
            if (rowIndex > headCount) {
                if (nextIsString) {
                    int idx = Integer.parseInt(lastContents);
                    lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
                    nextIsString = false;
                }
                if (name.equals("v")) {
                    //System.out.println(lastContents);
                    if (curRow) {
                        //是新行则new一行的对象来保存一行的值
                        if (null == rowContent) {
                            rowContent = new ArrayList<String>();
                        }
                        rowContent.add(lastContents);
                    }
                } else if (name.equals("c") && isNullCell) {
                    if (curRow) {
                        //是新行则new一行的对象来保存一行的值
                        if (null == rowContent) {
                            rowContent = new ArrayList<String>();
                        }
                        rowContent.add(null);
                    }
                }

                isNullCell = false;

                if ("row".equals(name)) {
                    list.add(rowContent);
                    curRow = false;
                    rowContent = null;
                }
            }

        }

        public void characters(char[] ch, int start, int length)
                throws SAXException {
            lastContents += new String(ch, start, length);
        }
    }

    /**
     * DOM的形式解析execl
     *
     * @param workbook
     * @param headRowCount
     * @return
     * @throws InvalidFormatException
     * @throws IOException
     */
    private List<List<String>> processDOMRead(Workbook workbook, int headRowCount) throws InvalidFormatException, IOException {
        headCount = headRowCount;

        Sheet sheet = workbook.getSheetAt(0);
        //行数
        int endRowIndex = sheet.getLastRowNum();

        Row row = null;
        List<String> rowList = null;

        for (int i = headCount; i <= endRowIndex; i++) {
            rowList = new ArrayList<String>();
            row = sheet.getRow(i);
            for (int j = 0; j < row.getLastCellNum(); j++) {
                if (null == row.getCell(j)) {
                    rowList.add(null);
                    continue;
                }
                int dataType = row.getCell(j).getCellType();
                if (dataType == Cell.CELL_TYPE_NUMERIC) {
                    DecimalFormat df = new DecimalFormat("0.####################");
                    rowList.add(df.format(row.getCell(j).getNumericCellValue()));
                } else if (dataType == Cell.CELL_TYPE_BLANK) {
                    rowList.add(null);
                } else if (dataType == Cell.CELL_TYPE_ERROR) {
                    rowList.add(null);
                } else {
                    //这里的去空格根据自己的情况判断
                    String valString = row.getCell(j).getStringCellValue();
                    Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                    Matcher m = p.matcher(valString);
                    valString = m.replaceAll("");
                    //去掉狗日的不知道是啥东西的空格
                    if (valString.indexOf(" ") != -1) {
                        valString = valString.substring(0, valString.indexOf(" "));
                    }

                    rowList.add(valString);
                }
            }

            list.add(rowList);
        }
        return list;
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        ReadExcelUtils howto = new ReadExcelUtils();
        String fileName = "f:/test.xlsx";
        List<List<String>> list = howto.processSAXReadSheet(fileName, 2);

        ReadExcelUtils h = new ReadExcelUtils();
        String fileName1 = "f:/test.xls";
        List<List<String>> result = h.processDOMReadSheet(fileName1, 2);
    }
}
