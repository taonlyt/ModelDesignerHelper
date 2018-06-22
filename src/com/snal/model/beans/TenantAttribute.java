/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

/**
 *
 * @author tao.luo
 */
public class TenantAttribute {

    private String location;
    private String dbserver;
    private String teamcode;
    private String tenant;

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDbserver() {
        return dbserver;
    }

    public void setDbserver(String dbserver) {
        this.dbserver = dbserver;
    }

    public String getTeamcode() {
        return teamcode;
    }

    public void setTeamcode(String teamcode) {
        this.teamcode = teamcode;
    }

    @Override
    public String toString() {
        return "TenantAttribute{" + "location=" + location + ", dbserver=" + dbserver + ", teamcode=" + teamcode + ", tenant=" + tenant + '}';
    }

}
