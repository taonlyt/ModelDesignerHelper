/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

import java.util.List;

/**
 *
 * @author Luo Tao
 */
public class ChangeRequest {

    private String requestId;
    private List<ModelChangeReq> hiveChgRequest;
    private List<ModelChangeReq> db2ChgRequest;

    public ChangeRequest(String requestId, List<ModelChangeReq> hiveChgRequest, List<ModelChangeReq> db2ChgRequest) {
        this.requestId = requestId;
        this.hiveChgRequest = hiveChgRequest;
        this.db2ChgRequest = db2ChgRequest;
    }

    public ChangeRequest() {
    }

    
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<ModelChangeReq> getHiveChgRequest() {
        return hiveChgRequest;
    }

    public void setHiveChgRequest(List<ModelChangeReq> hiveChgRequest) {
        this.hiveChgRequest = hiveChgRequest;
    }

    public List<ModelChangeReq> getDb2ChgRequest() {
        return db2ChgRequest;
    }

    public void setDb2ChgRequest(List<ModelChangeReq> db2ChgRequest) {
        this.db2ChgRequest = db2ChgRequest;
    }
    
    
}
