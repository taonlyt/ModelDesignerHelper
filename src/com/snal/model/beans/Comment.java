/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

/**
 *
 * @author csandy
 */
public class Comment {

    /**
     * 注释分隔符
     */
    private static final String COMMENT_DELIMITER = "----";

    public static String startComment(String content) {
        return COMMENT_DELIMITER + content + COMMENT_DELIMITER;
    }
}
