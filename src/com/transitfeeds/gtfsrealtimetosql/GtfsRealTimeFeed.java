package com.transitfeeds.gtfsrealtimetosql;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.sun.org.apache.xml.internal.security.utils.Base64;

public class GtfsRealTimeFeed {

	private URI mUri;
	private String mUsername;
	private String mPassword;
	private FeedMessage mFeedMessage;
	private boolean mOutputHeaders = false;
	
	private Logger mLogger;

	public GtfsRealTimeFeed(URI uri) {
	    mUri = uri;
	}
	
	public void setLogger(Logger logger) {
	    mLogger = logger;
	}
	
	public void setCredentials(String username, String password) {
		mUsername = username;
		mPassword = password;
	}
	
	public void setOutputHeaders(boolean flag) {
	    mOutputHeaders = flag;
	}

	public FeedMessage getFeedMessage() {
		return mFeedMessage;
	}
	
	private static final String HTTPS = "https";
	private static final String GZIP = "gzip";
	
	private void log(String str) {
	    if (mLogger != null) {
	        mLogger.info(str);
	    }
	}
	
	private int mSocketTimeoutMs = 30000;
	private int mConnectTimeoutMs = 30000;
	
	public void load() throws ConnectTimeoutException, SocketTimeoutException, NoSuchAlgorithmException, KeyManagementException, ClientProtocolException, IOException, HttpException {
		HttpClient httpClient;

		URI uri = mUri;
		        
	    log("Loading " + uri.toString() + " ...");
		
		HttpClientBuilder builder = HttpClientBuilder.create();

		RequestConfig.Builder configBuilder = RequestConfig.custom();
		configBuilder.setSocketTimeout(mSocketTimeoutMs);
		configBuilder.setConnectTimeout(mConnectTimeoutMs);
		
	    builder.setDefaultRequestConfig(configBuilder.build());

	    if (uri.getScheme().equals(HTTPS)) {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			
			sslContext.init(null, new TrustManager[] { new javax.net.ssl.X509TrustManager() {
				
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				
				@Override
				public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				}
				
				@Override
				public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				}
			} }, new SecureRandom());
			
			int port = uri.getPort();
			
			if (port < 0) {
				port = 443;
			}

			builder.setSslcontext(sslContext);
		}

		httpClient = builder.build();
		
		HttpGet httpGet = new HttpGet(uri);
		httpGet.setHeader("Accept-Encoding", GZIP);
        httpGet.setHeader("Accept", "application/x-protobuf,application/octet-stream,*/*");

		String username = mUsername;
		String password = mPassword;

		if (username != null && password != null) {
			String creds = String.format("%s:%s", username, password);
			httpGet.setHeader("Authorization", "Basic " + Base64.encode(creds.getBytes()));
		}

		HttpResponse response = httpClient.execute(httpGet);

		if (mOutputHeaders) {
		    log("Request headers:");
    		outputHeaders(httpGet.getAllHeaders());
    
    		log("Response headers:");
    	    outputHeaders(response.getAllHeaders());
		}
		
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new HttpException("Unexpected response: " + response.getStatusLine().toString(), response.getStatusLine().getStatusCode());
		}

		HttpEntity httpEntity = response.getEntity();
		
		Header contentEncoding = httpEntity.getContentEncoding();

		InputStream is;
		
		if (contentEncoding == null || !contentEncoding.getValue().equalsIgnoreCase(GZIP)) {
			is = httpEntity.getContent();
		}
		else {
			is = new GZIPInputStream(new ByteArrayInputStream(EntityUtils.toByteArray(httpEntity)));
		}

		mFeedMessage = GtfsRealtime.FeedMessage.parseFrom(is);
		log("Finished Loading " + uri.toString());
	}
	
	private void outputHeaders(Header[] headers) {
	    String str = "";
	    
	    for (int i = 0; i < headers.length; i++) {
	        Header header = headers[i];
	        
	        if (i > 0) {
	            str += "\n";
	        }
	        
	        str += header.toString();
	    }
	    
	    log(str);
	}
}
