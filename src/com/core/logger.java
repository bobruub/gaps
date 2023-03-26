package com.core;

import com.core.config;

public class logger {

    config config = new config();

    public void info(String name ) {
            System.out.println("INFO: " + name);

    }

    public void info(String name, String logLevel) {
        if (logLevel.equals("INFO") || logLevel.equals("DEBUG"))
            System.out.println("INFO: " + name);

    }  

    public void error(String string ) {
        System.out.println("ERRR: " + string);

    }

    public void conf(String string ) {
        System.out.println("CONF: " + string);

    }
    public void debug(String string) {
            System.out.println("DBUG: " + string);
    }

     public void debug(String string,String logLevel ) {
        if (logLevel.equals("DEBUG"))
            System.out.println("DEBUG: " + string);

}
}
