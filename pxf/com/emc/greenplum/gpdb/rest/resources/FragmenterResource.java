package com.emc.greenplum.gpdb.rest.resources;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.emc.greenplum.gpdb.hdfsconnector.BaseMetaData;
import com.emc.greenplum.gpdb.hdfsconnector.IDataFragmenter;
import com.emc.greenplum.gpdb.hdfsconnector.FragmenterFactory;

/*
 * Class enhances the API of the WEBHDFS REST server.
 * Returns the data fragments that a data resource is made of, enabling parallel processing of the data resource.
 * Example for querying API FRAGMENTER from a web client
 * curl -i "http://localhost:50070/gpdb/v2/Fragmenter?path=/dir1/dir2/*txt"
 * /gpdb/ is made part of the path when this package is registered in the jetty servlet
 * in NameNode.java in the hadoop package - /hadoop-core-X.X.X.jar
 */
@Path("/v2/Fragmenter/")
public class FragmenterResource
{
	org.apache.hadoop.fs.Path path = null;
	private Log Log;

	enum FunctionType{
		GetFragments,
		GetStats
	};

	
	public FragmenterResource() throws IOException
	{ 
		Log = LogFactory.getLog(FragmenterResource.class);
	}
	
	@GET
	@Path("getFragments")
	@Produces("application/json")
	public Response getFragments(@Context HttpHeaders headers,
						  		  @QueryParam("path") String p) throws Exception
	{
		return getDataFromFragmenter(headers, p, FunctionType.GetFragments);
	}
	

	/*
	 * Returns statistics for the given path (data source).
	 * Example for querying API FRAGMENTER from a web client
	 * curl -i "http://localhost:50070/gpdb/v2/Fragmenter/getStats?path=/dir1/dir2/*txt"
	 * An default answer, unless a fragmenter implements GetStats, would be:
	 * {"GPXFFilesSize":[{"blockSize":67108864,"numberOfBlocks":1000,"numberOfTuples":1000000}]}
	 * Currently only HDFS is implemented to calculate the block size and block number, 
	 * and returns -1 for number of tuples.
	 * Example:
	 * {"GPXFFilesSize":[{"blockSize":67108864,"numberOfBlocks":3,"numberOfTuples":-1}]}
	 */
	@GET
	@Path("getStats")
	@Produces("application/json")
	public Response getStats(@Context HttpHeaders headers,
			                 @QueryParam("path") String p) throws Exception
	{
		return getDataFromFragmenter(headers, p, FunctionType.GetStats);
	}
	
	
	Response getDataFromFragmenter(HttpHeaders headers,
			                       String path,
			                       FunctionType func) throws Exception
	{		                  
		String startmsg = new String("FRAGMENTER started for path \"" + path + "\"");
		startmsg += ", Function " + func + ".";
				
		if (headers != null) 
		{
			for (String header : headers.getRequestHeaders().keySet()) 
				startmsg += " Header: " + header + " Value: " + headers.getRequestHeader(header);
		}
		
		Log.debug(startmsg);
				  
		/* Convert headers into a regular map */
		Map<String, String> params = convertToRegularMap(headers.getRequestHeaders());
		final IDataFragmenter fragmenter = FragmenterFactory.create(new BaseMetaData(params));
		final String datapath = new String(path);
		final FunctionType function = func;
		
		StreamingOutput streaming = new StreamingOutput()
		{
			/*
			 * Function queries the gpfusion Fragmenter for the data fragments of the resource
			 * The fragments are returned in a string formatted in JSON	 
			 */			
			@Override
			public void write(final OutputStream out) throws IOException
			{
				DataOutputStream dos = new DataOutputStream(out);
				
				try
				{
					switch (function)
					{
						case GetFragments:
							dos.writeBytes(fragmenter.GetFragments(datapath));
							break;
						case GetStats:
							dos.writeBytes(fragmenter.GetStats(datapath));
							break;
					}
				} 
				catch (org.mortbay.jetty.EofException e)
				{
					Log.error("Remote connection closed by GPDB", e);
				}
				catch (Exception e)
				{
					// API does not allow throwing Exception so need to convert to something
					// I can throw without declaring...
					Log.error("Exception thrown streaming", e);
					throw new RuntimeException(e);
				}				
			}
		};
		
		return Response.ok( streaming, MediaType.APPLICATION_OCTET_STREAM ).build();
	}
	
	Map<String, String> convertToRegularMap(MultivaluedMap<String, String> multimap)
	{
		Map<String, String> result = new HashMap<String, String>();
		for (String key : multimap.keySet())
		{
			String newKey = key;
			if (key.startsWith("X-GP-"))
				newKey = key.toUpperCase();
			result.put(newKey, multimap.getFirst(key).replace("\\\"", "\""));
		}
		return result;
	}
}
