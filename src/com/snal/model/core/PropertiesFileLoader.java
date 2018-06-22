/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.beans.TenantAttribute;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesFileLoader {

    public static Properties loadConfData() {
        String propfile = System.getProperty("user.dir") + "\\init.properties";
        String conffile = System.getProperty("user.dir") + "\\conf.properties";
        Properties prop = new Properties();
        Properties prop2 = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(propfile));
            InputStream in2 = new BufferedInputStream(new FileInputStream(conffile));
            System.out.println("loading init.properties ...");
            prop.load(new InputStreamReader(in, "UTF-8"));

            System.out.println("loading conf.properties ...");
            prop2.load(new InputStreamReader(in2, "UTF-8"));
            Enumeration elementnames2 = prop2.propertyNames();
            while (elementnames2.hasMoreElements()) {
                String key = (String) elementnames2.nextElement();
                prop.put(key, prop2.getProperty(key));
            }
            in.close();
            in2.close();
        } catch (Exception ex) {
            Logger.getLogger(PropertiesFileLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return prop;
    }

    public static Map<String, TenantAttribute> initTenantAttribute(Properties prop) {
        Map<String, TenantAttribute> retmap = new HashMap<>();
        Enumeration elementnames = prop.propertyNames();
        while (elementnames.hasMoreElements()) {
            String key = (String) elementnames.nextElement();
            String value = prop.getProperty(key);

            int idx = key.lastIndexOf(".") + 1;
            String tenant = key.substring(idx);
            TenantAttribute tenantAttr = retmap.get(tenant);
            if (tenantAttr == null) {
                tenantAttr = new TenantAttribute();
                tenantAttr.setTenant(tenant);
                retmap.put(tenant, tenantAttr);
            }
            if (key.contains("hdsf")) {
                tenantAttr.setLocation(value);
            } else if (key.contains("teamcode")) {
                tenantAttr.setTeamcode(value);
            } else if (key.contains("dbserver")) {
                tenantAttr.setDbserver(value);
            }
        }
        return retmap;
    }
}
