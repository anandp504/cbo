package com.tumri.cbo.backend;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;

import java.io.PrintStream;

/* ******

// To print AppNexus stuff to the log:


        if(appNexusPrintToLog)
            AppNexusInterface.setDebugPrinter(new AppNexusLogPrinter());
        else AppNexusInterface.setDebugPrinter(new LocalAppNexusPrinter());


 */

public class AppNexusLogPrinter implements AppNexusPrinter
{
    PrintStream out = null;

    public AppNexusLogPrinter(PrintStream out)
    {
        this.out = out;
    }

    @SuppressWarnings("unused")
    public void printLine(Object o)
	{
        if(out != null) out.println(o);
		Utils.logThisPoint(Level.INFO, o);
	}

    @SuppressWarnings("unused")
    public void printLine()
	{
        if(out != null) out.println();
		// Do nothing.
	}

    @SuppressWarnings("unused")
    public void print(Object o)
	{
        if(out != null) out.print(o);
        Utils.logThisPoint(Level.INFO, o);
	}

    @SuppressWarnings("unused")
    public void flush()
	{
        if(out != null) out.flush();
		// do nothing
	}

    static
    {
        Utils.addLogElideClass(AppNexusLogPrinter.class);         
    }

}
