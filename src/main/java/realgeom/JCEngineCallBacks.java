package realgeom;

/*
 * The code below is more or less a verbatim copy from the Maple documentation.
 */

/*
 * An example of a user defined EngineCallBacks.
 * This extends EngineCallBacksDefault, as we don't implement all the required
 * functions of EngineCallBacks
 */

/* import the required objects */
import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.EngineCallBacksDefault;

import java.io.*;
import java.util.LinkedList;

public class JCEngineCallBacks extends EngineCallBacksDefault
{
    /* instead of printing the messages to the screen, store them in this 
     * list */
    private LinkedList queue;

    public JCEngineCallBacks()
    {
	queue = new LinkedList();
    }

    /* allow public access for removing lines from the queue */
    public int numOfLines()
    {
	return queue.size();
    }

    public String getLine()
    {
	return (String)queue.removeFirst();
    }

    /* only JCEngineCallBacks can add lines into the queue */
    private void addLine( String line )
    {
	queue.addLast( line );
    }

    private void addLineWithOffset( String line, int offset )
    {
	int i;
	StringBuffer buf = new StringBuffer();

	for ( i = 0; i < offset; i++ )
	{
	    buf.append( ' ' );
	}
	buf.append( line );

	queue.addLast( buf.toString() );
    }
    
    public void textCallBack( Object data, int tag, String output ) 
	throws MapleException
    {
	/* used variables */
	data = null;

	/* different options for different tags */
	switch ( tag )
	{
	    /* append "Warning" to the message */
	    case MAPLE_TEXT_WARNING:
		addLine( "Warning, "+output );
		break;

	    default:
		/* just output the message */
		addLine( output );
		break;
	}

    }

    public void errorCallBack( Object data, int offset, String output )
	throws MapleException
    {
	/* append "Error" to the message */
	addLine( "Error, "+output );
    }

    public void statusCallBack( Object data, long bytesUsed, long bytesAlloc, 
	    double cpuTime )
	throws MapleException
    {
	/* make the bytes used message */
	addLine( "bytes used="+bytesUsed*1024 + 
		    ", alloc="+ bytesAlloc*1024 + ", time=" + cpuTime );
    }

    public String readLineCallBack( Object data, boolean debug )
	throws MapleException
    {
	BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );

	/* on a readline, read a line from System.in */

	try
	{
	    return input.readLine();
	}
	catch ( IOException ioE )
	{
	    return "";
	}
    }
}
