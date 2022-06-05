package com.tcp;

import java.io.FilterInputStream;
import java.io.InputStream;

public class tcpInputStream extends FilterInputStream
{
	 public tcpInputStream(InputStream in)
	    {
	        super(in);
	    }
	    public String readLine(int lineLength)
	            throws Exception
	    {
	        StringBuffer result = new StringBuffer();
	        for (int i = 0; i < lineLength; i++)
	        {
	            int ch = read();
	            result.append((char) ch);
	        }
	        return result.toString();
	    }

	    // Get a line
	    public String readLine() throws Exception
	    {
	        StringBuffer result=new StringBuffer();
	        boolean finished = false;
	        do
	        {
	            int ch = -1;
	            ch = read();
	            if(ch==-1)
	                return result.toString();
	            
	            if(ch==13)
	                return result.toString();
	            
	            result.append((char) ch);
	 
	        } while (!finished);
	        return result.toString();
	    }
}
