/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.util.excel;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author luotao
 */
public class ExcelSheetHandler extends DefaultHandler {

    private StylesTable stylesTable;
    private ReadOnlySharedStringsTable sharedStringsTable;
    private boolean vIsOpen;
    private xssfDataType nextDataType;
    private final int minColumnCount;
    private short formatIndex;
    private String formatString;
    private final DataFormatter formatter;
    private int thisColumn = -1;
    private int lastColumnNumber = -1;
    private StringBuffer value;
    private List<List<String>> sheetrows;
    private List<String> cells = new ArrayList();
    private int minColumns;

    enum xssfDataType {
        BOOL,
        ERROR,
        FORMULA,
        INLINESTR,
        SSTINDEX,
        NUMBER,
    }

    public ExcelSheetHandler(StylesTable styles, ReadOnlySharedStringsTable strings, List<List<String>> sheetrows, int minColumns) {
        this.stylesTable = styles;
        this.sharedStringsTable = strings;
        this.minColumnCount = minColumns;
        this.minColumns = minColumns;
        this.value = new StringBuffer();
        this.nextDataType = xssfDataType.NUMBER;
        this.formatter = new DataFormatter();
        this.sheetrows = sheetrows;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

        if ("inlineStr".equals(name) || "v".equals(name)) {
            vIsOpen = true;
            value.setLength(0);
        } else if ("c".equals(name)) {
            String r = attributes.getValue("r");
            int firstDigit = -1;//第一个出现数字的位置
            for (int c = 0; c < r.length(); ++c) {
                if (Character.isDigit(r.charAt(c))) {
                    firstDigit = c;
                    break;
                }
            }
            thisColumn = nameToColumn(r.substring(0, firstDigit));
            this.nextDataType = xssfDataType.NUMBER;
            this.formatIndex = -1;
            this.formatString = null;
            String cellType = attributes.getValue("t");
            String cellStyleStr = attributes.getValue("s");
            if ("b".equals(cellType)) {
                nextDataType = xssfDataType.BOOL;
            } else if ("e".equals(cellType)) {
                nextDataType = xssfDataType.ERROR;
            } else if ("inlineStr".equals(cellType)) {
                nextDataType = xssfDataType.INLINESTR;
            } else if ("s".equals(cellType)) {
                nextDataType = xssfDataType.SSTINDEX;
            } else if ("str".equals(cellType)) {
                nextDataType = xssfDataType.FORMULA;
            } else if (cellStyleStr != null) {
                int styleIndex = Integer.parseInt(cellStyleStr);
                XSSFCellStyle style = stylesTable.getStyleAt(styleIndex);
                this.formatIndex = style.getDataFormat();
                this.formatString = style.getDataFormatString();
                if (this.formatString == null) {
                    this.formatString = BuiltinFormats.getBuiltinFormat(this.formatIndex);
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {

        String thisStr = null;

        if ("v".equals(name)) {
            switch (nextDataType) {
                case BOOL:
                    char first = value.charAt(0);
                    thisStr = first == '0' ? "FALSE" : "TRUE";
                    break;
                case ERROR:
                    thisStr = "\"ERROR:" + value.toString() + '"';
                    break;
                case FORMULA:
                    thisStr = value.toString();
                    break;

                case INLINESTR:
                    XSSFRichTextString rtsi = new XSSFRichTextString(value.toString());
                    thisStr = rtsi.toString();
                    break;
                case SSTINDEX:
                    String sstIndex = value.toString();
                    try {
                        int idx = Integer.parseInt(sstIndex);
                        XSSFRichTextString rtss = new XSSFRichTextString(sharedStringsTable.getEntryAt(idx));
                        thisStr = rtss.toString();
                    } catch (NumberFormatException ex) {
                        System.err.println("Failed to parse SST index '" + sstIndex + "': " + ex.toString());
                    }
                    break;
                case NUMBER:
                    String n = value.toString();
                    if (this.formatString != null) {
                        thisStr = formatter.formatRawCellContents(Double.parseDouble(n), this.formatIndex, this.formatString);
                    } else {
                        thisStr = n;
                    }
                    break;
                default:
                    thisStr = "";
                    break;
            }
            if (lastColumnNumber == -1) {
                lastColumnNumber = 0;
            }
            if (thisColumn - lastColumnNumber > 1) {
                for (int k = 1; k < thisColumn - lastColumnNumber; k++) {
                    cells.add("");
                }
            }
            cells.add(thisStr);
            if (thisColumn > -1) {
                lastColumnNumber = thisColumn;
            }

        } else if ("row".equals(name)) {
            if (minColumns > 0) {
                if (lastColumnNumber == -1) {
                    lastColumnNumber = 0;
                }
                if (minColumnCount - lastColumnNumber > 0) {
                    for (int k = 0; k < minColumnCount - lastColumnNumber; k++) {
                        cells.add("");
                    }
                }
            }
            sheetrows.add(cells);
            cells = new ArrayList();
            lastColumnNumber = -1;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (vIsOpen) {
            value.append(ch, start, length);
        }
    }

    private int nameToColumn(String name) {
        int column = -1;
        for (int i = 0; i < name.length(); ++i) {
            int c = name.charAt(i);
            column = (column + 1) * 26 + c - 'A';
        }
        return column;
    }
}
