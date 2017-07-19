package org.opennms.aci.rpc.rest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Java REST client for ACI API.
 * 
 * @author tf016851
 *
 */
public class ACIRestClient
{

    /**
     * An intermediary class for Rest requests that use HTTP GET
     */
    private class RestHttpGet extends HttpGet
    {
        public RestHttpGet ( final String path, final Map<String, String> parameters ) throws IOException
        {
            super( buildUri( path, parameters ) );
        }
    }

    /**
     * An intermediary class for rest requests that use HTTP POST.
     */
    private class RestHttpPost extends HttpPost
    {
        public RestHttpPost ( final String path ) throws IOException
        {
            super( buildUri( path, null ) );
        }
    }

    /**
     * An intermediary class for rest requests that use HTTP PATCH.
     */
    private class RestHttpPatch extends HttpPatch
    {
        public RestHttpPatch ( final String path ) throws IOException
        {
            super( buildUri( path, null ) );
        }
    }

    /**
     * An intermediary class for rest requests that use HTTP PUT.
     */
    private class RestHttpPut extends HttpPut
    {
        public RestHttpPut ( final String path ) throws IOException
        {
            super( buildUri( path, null ) );
        }
    }

    /**
     * An intermediary class for rest requests that use HTTP DELETE.
     */
    private class RestHttpDelete extends HttpDelete
    {
        public RestHttpDelete ( final String path ) throws IOException
        {
            super( buildUri( path, null ) );
        }
    }

    private static final String     HTTPS_SCHEME   = "https";
    private static final int        DEFAULT_PORT   = 443;
    private final SimpleDateFormat  format         = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
    private final SimpleDateFormat  dateOnlyFormat = new SimpleDateFormat( "yyyy-MM-dd" );
    private final int               port;
    private final String            authHeader;
    private final String            cluster;
    private final String            host;
    private final String            username;
    private final String            password;
    private final DefaultHttpClient httpClient;
    private final HttpContext       httpContext;
    private final String            restUrlPrefix;
    private final BasicCookieStore  cookieStore;

    /**
     * Singleton method for creating new ACIRestClient object and logging into ACI hosts.
     * 
     * @param cluster Name of APIC Cluster
     * @param url Comma delimited list of APIC URLs
     * @param username APIC user login ID.
     * @param password APIC user login password.
     * @return the ACIRestClient instance. Null on failure to create.
     * @throws Exception
     */
    public static ACIRestClient newAciRest( final String cluster, final String url, final String username,
                    final String password ) throws Exception
    {
//        System.err.println( "ACIRestClient.newAciRest: url=" + url + ", username=" + username + ", password="
//                        + password );
        String[] urls = url.split( "," );
        for ( String singleUrl : urls )
        {

            URI uri = new URI( singleUrl );
            if ( !HTTPS_SCHEME.equals( uri.getScheme() ) )
            {
                throw new ACIRestException( "Unsupported scheme " + uri.getScheme() + ".Please use only "
                                + HTTPS_SCHEME );
            }
            int port = uri.getPort();
            if ( port < 0 )
            {
                port = DEFAULT_PORT;
            }
            ACIRestClient aciRest = new ACIRestClient( cluster, uri.getHost(), username, password, null, null, port );
            JSONObject loginResult;
            loginResult = aciRest.login();
            System.err.println( loginResult.toJSONString() );
            return aciRest;
        }
        // If we made it here, then failed to login
        throw new ACIRestException( "Could not login to Rest API server. Please check login credentials." );

    }

    /**
     * Create and return an instance of AciRestClient from the command line parameters.
     *
     * @param cluster Name of APIC Cluster
     * @param url Comma delimited list of APIC URLs
     * @param username APIC user login ID.
     * @param password APIC user login password.
     * @param trustStorePath ACI trust store path.
     * @param trustStorePassword ACI trust store password.
     * @return the ACIRestClient instance. Null on failure to create.
     * @throws java.lang.Exception
     */
    public static ACIRestClient newAciRest( final String cluster, final String url, final String username,
                    final String password, final String trustStorePath, final String trustStorePassword )
                    throws Exception
    {
//        System.err.println( "ACIRestClient.newAciRest: url=" + url + ", username=" + username + ", password="
//                        + password );
        String[] urls = url.split( "," );
        for ( String singleUrl : urls )
        {

            URI uri = new URI( singleUrl );
            if ( !HTTPS_SCHEME.equals( uri.getScheme() ) )
            {
                throw new ACIRestException( "Unsupported scheme " + uri.getScheme() + ".Please use only "
                                + HTTPS_SCHEME );
            }
            int port = uri.getPort();
            if ( port < 0 )
            {
                port = DEFAULT_PORT;
            }
            ACIRestClient aciRest = new ACIRestClient( cluster, uri.getHost(), username, password, trustStorePath,
                                                       trustStorePassword, port );
            JSONObject loginResult;
            loginResult = aciRest.login();
            System.err.println( loginResult.toJSONString() );
            return aciRest;
        }
        // If we made it here, then failed to login
        throw new ACIRestException( "Could not login to Rest API server. Please check login credentials." );
    }

    /**
     * Create and return an instance of AciRestClient.
     *
     * @param cluster Name of APIC Cluster
     * @param host OCI server host name.
     * @param username OCI user name.
     * @param password OCI password.
     * @param trustStorePath OCI trust store path.
     * @param trustStorePassword OCI trust store password.
     * @param port OCI server port.
     * @throws java.lang.Exception
     */
    private ACIRestClient ( final String cluster, final String host, final String username, final String password,
                    final String trustStorePath, final String trustStorePassword, final int port ) throws Exception
    {
        if ( trustStorePath != null )
        {
            initializeTrustStore( trustStorePath, trustStorePassword );
            this.httpClient = new DefaultHttpClient();
        }
        else
        {

            TrustStrategy acceptingTrustStrategy = ( cert, authType ) -> true;
            SSLSocketFactory sf = new SSLSocketFactory( acceptingTrustStrategy,
                                                        SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER );
            SchemeRegistry registry = new SchemeRegistry();
            registry.register( new Scheme( "https", port, sf ) );
            ClientConnectionManager ccm = new PoolingClientConnectionManager( registry );
            this.httpClient = new DefaultHttpClient( ccm );

        }
        this.username = username;
        this.password = password;
        this.authHeader = "Basic " + Base64.encodeBase64String( (username + ':' + password).getBytes( "UTF-8" ) );
        this.cluster = cluster;
        this.host = host;
        this.port = port;
        this.httpContext = new BasicHttpContext();
        this.cookieStore = new BasicCookieStore();
        this.httpContext.setAttribute( ClientContext.COOKIE_STORE, this.cookieStore );
        this.restUrlPrefix = "/api/";
    }

    /**
     * Initialize trust store in system properties.
     *
     * @param trustStorePath trust store path.
     * @param trustStorePassword trust store password.
     * @throws ACIRestException
     */
    private void initializeTrustStore( final String trustStorePath, final String trustStorePassword )
                    throws ACIRestException
    {
        File trustStore = new File( trustStorePath );
        if ( !trustStore.exists() )
        {
            throw new ACIRestException( "Trust store does not exist in the given path: " + trustStorePath );
        }
        System.setProperty( "javax.net.ssl.trustStore", trustStorePath );
        if ( trustStorePassword != null )
        {
            System.setProperty( "javax.net.ssl.trustStorePassword", trustStorePassword );
        }
    }

    /**
     * Login to rest api server.
     *
     * @return the version of the rest server.
     * @throws Exception on failure to login.
     */
    private JSONObject login() throws Exception
    {
        // RestHttpPost restPost = new RestHttpPost( "login" );
        RestHttpPost restPost = new RestHttpPost( "aaaLogin.json" );
        restPost.addHeader( "Authorization", authHeader );
        restPost.setEntity( new StringEntity( "{\"aaaUser\": {\"attributes\": {\"name\": \"" + username
                        + "\", \"pwd\": \"" + password + "\"}}}" ) );
        // restPost.setEntity( new StringEntity( "<aaaUser name='svcOssAci' pwd='kf3as=Nx'/>" ) );
        final HttpResponse httpResponse = httpClient.execute( restPost, httpContext );
        final HttpEntity httpEntity = httpResponse.getEntity();
        final String data = EntityUtils.toString( httpEntity );
        final JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse( data );

        JSONArray imdata = (JSONArray) result.get( "imdata" );
        JSONObject r2 = (JSONObject) imdata.get( 0 );
        if ( !r2.containsKey( "aaaLogin" ) )
        {
            throw new ACIRestException( "Failed to Login, reason: \n" + ((JSONObject) r2).toJSONString() );
        }
        JSONObject aaaLogin = (JSONObject) r2.get( "aaaLogin" );
        JSONObject attributes = (JSONObject) aaaLogin.get( "attributes" );
        String token = (String) attributes.get( "token" );

        Cookie cookie = new BasicClientCookie( "APIC-cookie", token );
        this.cookieStore.addCookie( cookie );

        return result;
    }

    /**
     * Parse the parameters from the URL path.
     *
     * @param path the url path.
     * @return the parameter map that was parsed. Empty map if no query in URL.
     * @throws java.lang.Exception if the query could not be parsed.
     */
    private Map<String, String> parseQuery( String path ) throws Exception
    {
        Map<String, String> params = new HashMap<String, String>();
        if ( !path.contains( "?" ) )
        {
            return params;
        }
        String[] pathQuery = path.split( "\\?" );
        path = pathQuery[0];
        String query = pathQuery[1];
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme( HTTPS_SCHEME ).setHost( host ).setPort( port ).setPath( path ).setQuery( query );
        for ( NameValuePair pair : uriBuilder.getQueryParams() )
        {
            if ( params.containsKey( pair.getName() ) )
            {
                throw new ACIRestException( "Could not parse query from URL. Duplicate query parameter found: "
                                + pair.getName() );
            }
            params.put( pair.getName(), pair.getValue() );
        }
        return params;
    }

    /**
     * Method to get Health and Fault details for all the managed objects for given classes.
     * 
     * @param classes
     * @throws Exception
     */
    public void getHealth( String... classes ) throws Exception
    {
        for ( String apicClass : classes )
        {
            String queryUrl = "node/class/" + apicClass + ".json";
            JSONObject result = (JSONObject) this.get( queryUrl );
            JSONArray imdata = (JSONArray) result.get( "imdata" );
            for ( Object object : imdata )
            {
                JSONObject objectData = (JSONObject) object;
                if ( objectData.get( "error" ) != null )
                {
                    printError( imdata, apicClass, this.host );
                }
                else
                {
                    for ( Object object2 : objectData.keySet() )
                    {
                        String key = (String) object2;
                        JSONObject attributes = (JSONObject) ((JSONObject) objectData.get( key )).get( "attributes" );
                        String dn = (String) attributes.get( "dn" );

                        // Health Details
                        String hqueryUrl = null;
                        if ( apicClass.equals( "fabricNode" ) )
                        {
                            hqueryUrl = "mo/" + dn + "/sys.json?rsp-subtree-include=health,no-scoped";
                        }
                        else
                        {
                            hqueryUrl = "mo/" + dn + ".json?rsp-subtree-include=health,no-scoped";
                        }

                        JSONObject healthrt = (JSONObject) this.get( hqueryUrl );
                        JSONArray healthdata = (JSONArray) healthrt.get( "imdata" );
                        printObjectProperties( healthdata, apicClass, this.host );
                        // printObjectProperties( healthdata, "healthInst", this.host );

                        // Fault Details
                        String fqueryUrl = null;
                        if ( apicClass.equals( "fabricNode" ) )
                        {
                            fqueryUrl = "mo/" + dn
                                            + "/sys.json?rsp-subtree-include=faults,no-scoped&query-target=subtree";
                        }
                        else
                        {
                            fqueryUrl = "mo/" + dn + ".json?rsp-subtree-include=faults,no-scoped&query-target=subtree";
                        }

                        JSONObject faultrt = (JSONObject) this.get( fqueryUrl );
                        JSONArray faultdata = (JSONArray) faultrt.get( "imdata" );
                        // printObjectProperties( faultdata, "faultDelegate", this.host );
                        printObjectProperties( faultdata, apicClass, this.host );

                    }
                }
            }

        }

    }

    /**
     * Method to get statistical related information for all the managed objects of given classes.
     * 
     * @param classes
     * @throws Exception
     */
    @SuppressWarnings( "unchecked" )
    public void getStats( String... classes ) throws Exception
    {
        for ( String apicClass : classes )
        {
            if ( !apicClass.equals( "fvCEp" ) )
            {
                String queryUrl = "node/class/" + apicClass + ".json";
                JSONObject result = (JSONObject) this.get( queryUrl );
                JSONArray imdata = (JSONArray) result.get( "imdata" );
                for ( Object object : imdata )
                {
                    JSONObject objectData = (JSONObject) object;
                    for ( Object object2 : objectData.keySet() )
                    {
                        String key = (String) object2;
                        JSONObject attributes = (JSONObject) ((JSONObject) objectData.get( key )).get( "attributes" );
                        String dn = (String) attributes.get( "dn" );

                        // Health Details
                        String moqueryUrl = "mo/" + dn + ".json?rsp-subtree-include=stats,no-scoped";
                        JSONObject moret = (JSONObject) this.get( moqueryUrl );
                        JSONArray modata = (JSONArray) moret.get( "imdata" );
                        printObjectProperties( modata, apicClass, this.host );
                    }
                }

            }
            else
            {
                String queryUrl = "node/class/" + apicClass
                                + ".json?rsp-subtree=children&rsp-subtree-class=fvRsCEpToPathEp";
                JSONObject result = (JSONObject) this.get( queryUrl );
                JSONArray imdata = (JSONArray) result.get( "imdata" );

                for ( Object object : imdata )
                {
                    JSONObject objectData = (JSONObject) object;
                    for ( Object object2 : objectData.keySet() )
                    {
                        String key = (String) object2;
                        JSONObject attributes = (JSONObject) ((JSONObject) objectData.get( key )).get( "attributes" );
                        JSONArray children = (JSONArray) ((JSONObject) objectData.get( key )).get( "children" );

                        for ( Object cobject : children )
                        {
                            JSONObject cobjectData = (JSONObject) cobject;
                            for ( Object cobject2 : cobjectData.keySet() )
                            {
                                String ckey = (String) cobject2;
                                JSONObject childrendata = (JSONObject) ((JSONObject) cobjectData.get( ckey ))
                                                .get( "attributes" );
                                for ( Object object3 : childrendata.keySet() )
                                {
                                    String keys3 = (String) object3;
                                    attributes.put( keys3, childrendata.get( keys3 ) );
                                }
                            }

                        }
                        attributes.put( "apic_host", this.host );
                        attributes.put( "component", apicClass );
                        System.out.println( attributes.toJSONString() );
                    }
                }
            }
        }
    }

    /**
     * Method to get general information for all the managed objects for given classes
     * 
     * @param classes
     * @throws Exception
     */
    public void getClassInfo( String... classes ) throws Exception
    {

        List<Thread> classThreads = new ArrayList<Thread>();
        for ( String apicClass : classes )
        {
            if ( apicClass.equals( "faultRecord" ) || apicClass.equals( "eventRecord" )
                            || apicClass.equals( "aaaModLR" ) )
            {
                Thread t = new Thread( new Runnable() {
                    @Override
                    public void run()
                    {
                        getBigDataResult( apicClass );
                    }
                } );
                t.start();
                classThreads.add( t );
            }
            else
            {
                String queryUrl = "node/class/" + apicClass + ".json";
                JSONObject result = (JSONObject) this.get( queryUrl );
                int totalCount = Integer.parseInt( (String) result.get( "totalCount" ) );
                printObjectProperties( (JSONArray) result.get( "imdata" ), apicClass, this.host );
                System.err.println( "Found " + totalCount + " " + apicClass + " record(s)" );
            }
        }

        // Loop and wait on threads
        for ( Thread t : classThreads )
        {
            if ( t.isAlive() )
                t.join();
        }
    }

    private void getBigDataResult( String apicClass )
    {
        String cwd = Paths.get( "." ).toAbsolutePath().normalize().toString();
        String fileName = cluster + "_" + apicClass + "_LastTransactionTime.txt";
        String filePath = Paths.get( cwd, fileName ).toAbsolutePath().normalize().toString();
        BufferedReader reader = null;
        System.err.println( filePath );

        try
        {
            File file = new File( filePath );
            String currentTime = this.getCurrentTime();

            if ( file.isFile() )
            {
                // Found previous run, start from that point
                reader = new BufferedReader( new FileReader( file ) );
                String scaleStart = reader.readLine();
                this.batchBigData( filePath, apicClass, scaleStart );
            }
            else
            {
                // No previous run, process all data
                this.processAllBigData( filePath, apicClass, currentTime );
            }
        }
        catch ( FileNotFoundException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void processAllBigData( String filePath, String apicClass, String scaleEnd ) throws Exception
    {
        final java.util.Calendar cal = GregorianCalendar.getInstance();

        String queryUrl = "node/class/" + apicClass + ".json?rsp-subtree-include=count&query-target-filter=lt("
                        + apicClass + ".created,\"" + scaleEnd + "\")";
        JSONObject result = (JSONObject) this.get( queryUrl );
        JSONArray imdata = (JSONArray) result.get( "imdata" );

        int count = 0;
        for ( Object object : imdata )
        {
            JSONObject data = (JSONObject) object;
            for ( Object object2 : data.keySet() )
            {
                String key = (String) object2;
                JSONObject attributes = (JSONObject) ((JSONObject) data.get( key )).get( "attributes" );
                count = Integer.parseInt( (String) attributes.get( "count" ) );
            }
        }

        if ( count < 99000 )
        {
            queryUrl = "node/class/" + apicClass + ".json?query-target-filter=lt(" + apicClass + ".created,\""
                            + scaleEnd + "\")";

            int totalCount = this.queryAndPrint( apicClass, queryUrl );
            this.updateFile( filePath, scaleEnd );
            System.err.println( "Finished processing " + totalCount + " " + apicClass + " entrie(s)" );
        }
        else
        {
            // Since we have more than 99000 results, we have to paginate over last 60 days
            String[] timeparts = scaleEnd.split( "T" );
            String onlydate = timeparts[0];
            String onlytimewtz = timeparts[1];
            String onlytime = onlytimewtz.substring( 1, onlytimewtz.length() - 6 );
            String onlytz = onlytimewtz.substring( onlytimewtz.length() - 6 );
            String tz = onlytz.replace( ":", "" );

            cal.setTimeInMillis( format.parse( onlydate + "T" + onlytime + tz ).getTime() );
            cal.add( GregorianCalendar.DATE, -60 ); // default to 60 days
            String previous = dateOnlyFormat.format( cal.getTime() );
            String scaleStart = previous + "T" + onlytime + onlytz;
            this.batchBigData( filePath, apicClass, scaleStart );
        }
    }

    private void batchBigData( String filePath, String apicClass, String startTime ) throws Exception
    {
        final java.util.Calendar currentCal = GregorianCalendar.getInstance();
        final java.util.Calendar startCal = GregorianCalendar.getInstance();
        String currentTime = this.getCurrentTime();
        String[] currentTimeparts = currentTime.split( "T" );
        String currentdate = currentTimeparts[0];

        int entryCount = 0;

        String[] startTimeparts = startTime.split( "T" );
        String onlydate = startTimeparts[0];
        String onlytimewtz = startTimeparts[1];
        String onlytime = onlytimewtz.substring( 0, onlytimewtz.length() - 6 );
        String onlytz = onlytimewtz.substring( onlytimewtz.length() - 6 );
        String tz = onlytz.replace( ":", "" );

        startCal.setTimeInMillis( format.parse( onlydate + "T" + onlytime + tz ).getTime() );
        
        if (currentCal.before( startCal ))
            throw new ACIRestException( filePath + ": contains date in future: " + startTime );

        int days = (int)( (currentCal.getTimeInMillis() - startCal.getTimeInMillis()) / (1000 * 60 * 60 * 24));
        if ( (startCal.get( GregorianCalendar.DATE ) == currentCal.get( GregorianCalendar.DATE) ) ||
                        (days <= 1)  )
        {
            // Same date or within 24 hours just get all data
            int totalCount = this.processBigDataRange( apicClass, startTime, currentTime );
            entryCount += totalCount;
            this.updateFile( filePath, currentTime );
        }
        else
        {
            // This means we have a range, need to determine how much in range.
//            int loop = currentCal.get( GregorianCalendar.DATE ) - startCal.get( GregorianCalendar.DATE );
            int loop = days;
            int start = 0;

            while ( start < loop )
            {
                currentCal.setTimeInMillis( format.parse( currentdate + "T" + onlytime + tz ).getTime() );
                currentCal.add( GregorianCalendar.DATE, (-1 * (loop - start)) );
                String previous = dateOnlyFormat.format( currentCal.getTime() );
                String scaleStart = previous + "T" + onlytime + onlytz;
                currentCal.add( GregorianCalendar.DATE, 1 );
                String end = dateOnlyFormat.format( currentCal.getTime() );
                String scaleEnd = end + "T" + onlytime + onlytz;

                int totalCount = this.processBigDataRange( apicClass, scaleStart, scaleEnd );
                entryCount += totalCount;

                this.updateFile( filePath, scaleEnd );
                start++;
            }
        }
        System.err.println( "Finished processing " + entryCount + " " + apicClass + " entrie(s)" );

    }

    private int processBigDataRange( String apicClass, String scaleStart, String scaleEnd ) throws Exception
    {
        String queryUrl = "node/class/" + apicClass + ".json?query-target-filter=and(ge(" + apicClass + ".created,\""
                        + scaleStart + "\"),lt(" + apicClass + ".created,\"" + scaleEnd + "\"))";

        int totalCount = this.queryAndPrint( apicClass, queryUrl );
        System.err.println( "Processed " + apicClass + " ( " + scaleStart + " to " + scaleEnd + " ) with " + totalCount
                        + " entrie(s)" );

        return totalCount;
    }

    private int queryAndPrint( String apicClass, String queryUrl ) throws Exception
    {
        int totalCount = 0;
        JSONObject result = (JSONObject) this.get( queryUrl );
        JSONArray imdata = (JSONArray) result.get( "imdata" );
        totalCount = Integer.parseInt( (String) result.get( "totalCount" ) );
        printObjectProperties( imdata, apicClass, this.host );

        return totalCount;
    }

    private synchronized void updateFile( String filePath, String timedata ) throws IOException
    {

        BufferedReader reader = null;
        PrintWriter pw = null;

        File file = new File( filePath );

        try
        {
            // Last, store current datetimestamp to file
            FileWriter fw = new FileWriter( file, false );
            pw = new PrintWriter( fw );
            pw.println( timedata );
        }
        finally
        {
            try
            {
                if ( reader != null )
                    reader.close();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
            try
            {
                if ( pw != null )
                {
                    pw.flush();
                    pw.close();
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    private String getCurrentTime()
    {
        // Last, store current datetimestamp to file
        String queryUrl = "node/mo/info.json";
        String currentTime = null;
        try
        {
            JSONObject result = (JSONObject) this.get( queryUrl );
            JSONArray tdata = (JSONArray) result.get( "imdata" );
            JSONObject objectData = (JSONObject) tdata.get( 0 );
            JSONObject attributes = null;
            for ( Object object : objectData.keySet() )
            {
                String key = (String) object;
                JSONObject classData = (JSONObject) objectData.get( key );
                attributes = (JSONObject) classData.get( "attributes" );
            }

            currentTime = (String) attributes.get( "currentTime" );
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return currentTime;

    }

    @SuppressWarnings( "unchecked" )
    private void printObjectProperties( JSONArray data, String apicClass, String apicHost )
    {
        // String now = this.format.format( new Date() );

        for ( Object object : data )
        {
            JSONObject objectData = (JSONObject) object;
            if ( objectData == null )
                continue;
            for ( Object object2 : objectData.keySet() )
            {
                String key = (String) object2;
                JSONObject classData = (JSONObject) objectData.get( key );
                JSONObject attributes = (JSONObject) classData.get( "attributes" );
                if ( attributes == null )
                    continue;

                attributes.put( "apic_host", apicHost );
                attributes.put( "component", apicClass );
                System.out.println( attributes.toJSONString() );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void printError( JSONArray data, String apicClass, String apicHost )
    {
        // String now = this.format.format( new Date() );

        for ( Object object : data )
        {
            JSONObject objectData = (JSONObject) object;
            if ( objectData == null )
                continue;

            System.err.println( objectData.toJSONString() );
        }
    }
    /**
     * Send a GET request to the rest server.
     *
     * @param path the rest path to send the GET request to.
     * @return the result of the GET request.
     * @throws java.lang.Exception
     */
    public Object get( String path ) throws Exception
    {
        RestHttpGet restGet = new RestHttpGet( path, parseQuery( path ) );
        return doExecute( restGet );
    }

    /**
     * Send Http POST request to rest server.
     *
     * @param path the rest server path to send the POST request to.
     * @param payload the JSON payload for the request.
     * @return the response from the server.
     * @throws java.lang.Exception
     */
    public Object post( String path, JSONObject payload ) throws Exception
    {
        RestHttpPost restPost = new RestHttpPost( path );
        restPost.setEntity( new StringEntity( payload.toString() ) );
        return doExecute( restPost );
    }

    /**
     * Send Http POST request to rest server.
     *
     * @param path the rest server path to send the POST request to.
     * @return the response from the server.
     * @throws java.lang.Exception
     */
    public Object post( String path ) throws Exception
    {
        RestHttpPost restPost = new RestHttpPost( path );
        return doExecute( restPost );
    }

    /**
     * Send Http PATCH request to rest server.
     *
     * @param path the rest server path to send the PATCH request to.
     * @param payload the JSON payload for the request.
     * @return the response from the server.
     * @throws java.lang.Exception
     */
    public Object patch( String path, JSONObject payload ) throws Exception
    {
        RestHttpPatch restPatch = new RestHttpPatch( path );
        restPatch.setEntity( new StringEntity( payload.toString() ) );
        return doExecute( restPatch );
    }

    /**
     * Send HTTP PUT request to rest server.
     *
     * @param path the rest path to send the PUT request to.
     * @param payload the JSON payload for the request.
     * @return the response from the server.
     * @throws java.lang.Exception
     */
    public Object put( String path, JSONObject payload ) throws Exception
    {
        RestHttpPut restPut = new RestHttpPut( path );
        restPut.setEntity( new StringEntity( payload.toString() ) );
        return doExecute( restPut );
    }

    /**
     * Send HTTP DELETE request to rest server.
     *
     * @param path the rest path to send the DELETE request to.
     * @return the response from the server.
     * @throws java.lang.Exception
     */
    public Object delete( String path ) throws Exception
    {
        RestHttpDelete restDelete = new RestHttpDelete( path );
        return doExecute( restDelete );
    }

    /**
     * Execute a Http request.
     *
     * @param request the Http request to be executed.
     * @return the server response object.
     * @throws java.lang.Exception
     */
    private Object doExecute( final HttpRequestBase request ) throws Exception
    {
        final JSONParser parser = new JSONParser();

        RestHttpGet restGet = new RestHttpGet( "aaaRefresh.json", null );
        final HttpResponse authResponse = httpClient.execute( restGet, httpContext );
        final HttpEntity authEntity = authResponse.getEntity();
        final String authData = EntityUtils.toString( authEntity );
        JSONObject result = (JSONObject) parser.parse( authData );

        JSONArray imdata = (JSONArray) result.get( "imdata" );
        JSONObject r2 = (JSONObject) imdata.get( 0 );
        JSONObject aaaLogin = (JSONObject) r2.get( "aaaLogin" );
        JSONObject attributes = (JSONObject) aaaLogin.get( "attributes" );
        String token = (String) attributes.get( "token" );

        Cookie cookie = new BasicClientCookie( "APIC-cookie", token );
        this.cookieStore.addCookie( cookie );

        final HttpResponse httpResponse = httpClient.execute( request, httpContext );
        final HttpEntity httpEntity = httpResponse.getEntity();
        final String data = EntityUtils.toString( httpEntity );
        return parser.parse( data );
    }

    /**
     * Construct URI.
     *
     * @param path the URL path suffix.
     * @param parameters the parameters of the URL.
     * @return the constructed URI.
     * @throws IOException on failure to construct to URI.
     */
    private URI buildUri( String path, final Map<String, String> parameters ) throws IOException
    {
        try
        {
            if ( parameters != null && path.contains( "?" ) )
            {
                String[] pathQuery = path.split( "\\?" );
                path = pathQuery[0];
            }

            final URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme( HTTPS_SCHEME ).setHost( host ).setPort( port );
            if ( path.startsWith( restUrlPrefix ) )
            {
                uriBuilder.setPath( path );
            }
            else
            {
                uriBuilder.setPath( restUrlPrefix + path );
            }
            if ( parameters != null )
            {

                for ( final Map.Entry<String, String> parameter : parameters.entrySet() )
                {
                    uriBuilder.addParameter( parameter.getKey(), parameter.getValue() );
                }
            }
            return uriBuilder.build();
        }
        catch ( URISyntaxException ex )
        {
            throw new IOException( ex );
        }
    }

    // private static class DefaultTrustManager implements X509TrustManager
    // {
    //
    // @Override
    // public void checkClientTrusted( X509Certificate[] arg0, String arg1 ) throws
    // CertificateException
    // {
    // }
    //
    // @Override
    // public void checkServerTrusted( X509Certificate[] arg0, String arg1 ) throws
    // CertificateException
    // {
    // }
    //
    // @Override
    // public X509Certificate[] getAcceptedIssuers()
    // {
    // return null;
    // }
    // }
}
