package org.archive.nlp.fetch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrStr;


/**
 * 网页下载类，含有重试错误处理的实现
 * 
 */
class HttpMethodRetryHandlerImpl implements HttpMethodRetryHandler
{
	/**
	 * 重试次数
	 */
	int	m_retryTimes = 5;

	/**
	 * 重试时间间隔
	 */
	int	m_retryInterval	= 5000;
	
	public void setRetryTimes(int times){
		this.m_retryTimes = times;
	}
	public int getRetryTimes(){
		return this.m_retryTimes;
	}
	public void setRetryInterval(int interval){
		this.m_retryInterval = interval;
	}
	public int getRetryInterval(){
		return this.m_retryInterval;
	}
	/**
	 * 重试接口
	 */
	public boolean retryMethod(HttpMethod method, IOException exception,
			int executionCount)
	{
		// 重试次数过多
		if (executionCount > m_retryTimes)
			return false;

		boolean ret = false;
		// 可以重新链接的错误
		if (ConnectException.class.isInstance(exception) == true
				|| HttpRetryException.class.isInstance(exception) == true
				|| NoRouteToHostException.class.isInstance(exception) == true
				|| ProtocolException.class.isInstance(exception) == true
				|| SocketException.class.isInstance(exception) == true
				|| SocketTimeoutException.class.isInstance(exception) == true)
		{
			ret = true;
		}
		if (ret == false)
			return false;
		// 等待一个时间间隔
		try
		{
			Thread.sleep(m_retryInterval);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// 重试
		return true;
	}
}
/**
 * 获取查询URL对应的返回页面
 * @author T
 *
 */
public class HttpDownloader {
	// search url
	private String urlStr;
	//http重试错误处理
	private static HttpMethodRetryHandlerImpl retryHandler = new HttpMethodRetryHandlerImpl();
	
	/////////////////////////////////////////
	public HttpDownloader(int retryTimes, int retryInterval){
		retryHandler.setRetryTimes(retryTimes);
		retryHandler.setRetryInterval(retryInterval);
		
	}
	/**
	 * 设定 search url
	 */
	public void setUrlStr(String url){
		this.urlStr = url;
	}
	public String getUrlStr(){
		return this.urlStr;
	}
	/**
	 * 注：输入的queryUrl如果含有中文词需要预先用－URLEncoder　encode();
	 * @param queryUrl
	 * @return
	 */
	public StringBuffer getContent(String queryUrl){
		StringBuffer result = null;
		setUrlStr(queryUrl);
		HttpClient httpClient = new HttpClient();
		//设置 Http 连接超时为5秒
		//httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
		//httpClient.getHttpConnectionManager().getParams().se
		
		GetMethod getMethod = new GetMethod(getUrlStr());
		//设置 get 请求超时为 5 秒
		//getMethod.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 5000);
		//设置请求重试处理，重试处理：请求5次,间隔为 5 秒
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
		
		try{
			int statusCode = httpClient.executeMethod(getMethod);
			if(statusCode != HttpStatus.SC_OK){
				System.err.println("Method failed: " + getMethod.getStatusLine());
			}else{
				BufferedReader reader = new BufferedReader(new InputStreamReader(getMethod.getResponseBodyAsStream()));
				String line;
				result = new StringBuffer();
				while(null != (line=reader.readLine())){
					result.append(line);
				}
				reader.close();
			}		
		}catch(HttpException e){
			// 发生致命的异常，可能是协议不对或者返回的内容有问题
			System.out.println("Please check your provided http address!");
			//e.printStackTrace();
		}catch(IOException e){
			// 发生网络异常  
			System.out.println("发生网络异常!");
			//e.printStackTrace();
		}finally{
			/*6 .释放连接*/   
			getMethod.releaseConnection(); 
		}
		return result;
	}
	
	private static UrlEncodedFormEntity genEntity(ArrayList<StrStr> paraList, String charset)throws UnsupportedEncodingException {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        
        for(StrStr para: paraList){
        	NameValuePair nameValuePair = new BasicNameValuePair(para.first, para.second);
            params.add(nameValuePair);
        }
        
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, charset);
        
        return entity;
    }
	
	private static UrlEncodedFormEntity genEntity(String topic, String id, String charset) throws UnsupportedEncodingException{
		ArrayList<StrStr> paraList = new ArrayList<StrStr>();
		
		//paraList.add(new StrStr("type", "indri"));
		//paraList.add(new StrStr("number", id));
		//paraList.add(new StrStr("text", "#combine("+topic+")"));
		paraList.add(new StrStr("q", topic));
		
		return genEntity(paraList, charset);		
	}
	
	static class PreemptiveAuth implements HttpRequestInterceptor {
        public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {

            AuthState authState = (AuthState) context
                .getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme avaialble yet, try to initialize it
            // preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context
                    .getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context
                    .getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context
                    .getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider
                        .getCredentials(new AuthScope(targetHost
                                    .getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException(
                                "No credentials for preemptive authentication");
                    }
                    authState.setAuthScheme(authScheme);
                    authState.setCredentials(creds);
                }
            }
        }
    }
	
	static int mv2beg(byte[] tmp, int pos) {
        for(int i = pos,j = 0; i<tmp.length; ++i, ++j) {
            tmp[j] = tmp[i];
        }
        return tmp.length - pos;
    }
	/**
	 * APP-1
	 * **/
	public static void queryClueweb() throws ClientProtocolException, IOException{
		
		String topic = "apple";
		String id = "0050";
		
		String charset = "UTF-8";
		
		UrlEncodedFormEntity entity = genEntity(topic, id, charset);
		//http://boston.lti.cs.cmu.edu/Services/clueweb12_batch/		
        HttpPost httppost = new HttpPost("http://boston.lti.cs.cmu.edu/Services/clueweb12_b13/lemur.cgi?"+"q="+topic);
        //httppost.setEntity(entity);
        
        DefaultHttpClient httpclient = new DefaultHttpClient();
        
        String usname = "tokushima-hty";
        String passwd = "hybrid#Mash";
        
        String host = "http://boston.lti.cs.cmu.edu";
        String uri = "http://boston.lti.cs.cmu.edu/Services/clueweb12_b13/lemur.cgi?g=p&x=false&q=apple";

        DefaultHttpClient dhttpclient = new DefaultHttpClient();
        try
        {
        	
            dhttpclient.getCredentialsProvider().setCredentials(new AuthScope(host, 8080), new UsernamePasswordCredentials(usname, passwd));
            HttpGet dhttpget = new HttpGet(uri);

            System.out.println("executing request " + dhttpget.getRequestLine());
            HttpResponse dresponse = dhttpclient.execute(dhttpget);

            System.out.println(dresponse.getStatusLine()    );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            dhttpclient.getConnectionManager().shutdown();
        }
        
        
        //-
        
        /*
        String host = "http://boston.lti.cs.cmu.edu";        
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(host, AuthScope.ANY_PORT), new UsernamePasswordCredentials(usname, passwd));

//        BasicScheme basicAuth = new BasicScheme();
//        BasicHttpContext localcontext = new BasicHttpContext();
//        localcontext.setAttribute("preemptive-auth", basicAuth);
//
//        httpclient.addRequestInterceptor((HttpRequestInterceptor) new PreemptiveAuth(), 0);

        HttpResponse response = httpclient.execute(httppost);

        //System.err.println(response.getStatusLine().toString());
        if (response.getStatusLine().toString().indexOf("401") >= 0) {
            throw new RuntimeException("Authorization is denied!");
        } else if (response.getStatusLine().toString().indexOf("200") < 0) {
            throw new RuntimeException(response.getStatusLine().toString());
        } 
        
        HttpEntity res_entity = response.getEntity();

        //httpclient.getConnectionManager().closeIdleConnections(0, TimeUnit.SECONDS);

        //StringBuffer result = new StringBuffer();
        String result = "";
        if (res_entity != null) {
            InputStream is = res_entity.getContent();			

            int size;
            byte[] tmp = new byte[4098];
            int beg=0,leng=tmp.length;
            while ((size = is.read(tmp,beg,leng)) != -1)
                if(leng == size) {
                    int i;
                    for(i = tmp.length-1; i>0; --i){
                        if(tmp[i] == '\n') {
                            //		    				System.out.println("i: "+i);
                            beg = i+1;
                            result += new String(tmp, 0, beg, charset);
                            beg = mv2beg(tmp, beg);
                            leng = tmp.length - beg;
                            break;
                        }
                    }
                    if(i==0) {
                        System.err.println("Warning: the single sentence is too long!");
                        result += new String(tmp, 0, tmp.length, charset);			
                        beg = 0;
                        leng = tmp.length;
                    }
                } else {
                    result += new String(tmp, 0, beg+size, charset);
                    beg = 0;
                    leng = tmp.length;
                }
            //		    System.err.println(result);
            

            res_entity.consumeContent();
        }
        */

        //System.out.println(result);
		
	}
	/**
	 * APP-2: fetch contents w.r.t. urls in Bing's search log
	 * **/
	//test case
	public static void getHtml(String urlStr){
		String url = urlStr;
		
		HttpDownloader downloader = new HttpDownloader(1, 500);		
		StringBuffer resultBuf = downloader.getContent(url);
		System.out.println(resultBuf.toString());
	}
	//for top-100
	//fetch html source files for given url list file
	public static void getSearchLogHtml(){
		//
		HttpDownloader downloader = new HttpDownloader(1, 1000);
		StringBuffer htmlBuf = null;
		
		String root = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/";
		
		//target file
		String urlListFile = root+"UrlList_Top100Sessions.txt";		
		
		//output 
		String outputDir = root+"/HtmlSourceFiles_Top100/";
		int txtFileID = 0, outputCount = 0, numPerFile = 2000;
		String txtFile = null;
		
		//statistic
		int successCount = 0;
		int failedCount = 0;
		
		//int testCount = 53;
		
		try {
			PrintStream logPrinter = new PrintStream(new FileOutputStream(new File(outputDir+"FetchLog.txt")));
			System.setOut(logPrinter);	
			
			
			ArrayList<String>  urlList = IOText.getLinesAsAList_UTF8(urlListFile);
			
			txtFile = outputDir+StandardFormat.serialFormat(++txtFileID, "00000000")+".txt";		
			
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(txtFile);
			
			BufferedWriter noResultWriter = IOText.getBufferedWriter_UTF8(outputDir+"FailedUrlList.txt");
			
			for(int i=0; i<urlList.size(); i++){
				System.out.println("--"+(i+1));
				
				/*
				if(i> testCount){
					break;
				}
				*/
				
				String url = urlList.get(i);
				
				if(null != (htmlBuf=downloader.getContent(url))){
					successCount++;
					
					String html = htmlBuf.toString();
					
					if((outputCount>0) && (0 == (outputCount%numPerFile))){
						writer.flush();
						writer.close();
						writer = null;
						
						txtFile = outputDir+StandardFormat.serialFormat(++txtFileID, "00000000")+".txt";
						writer = IOText.getBufferedWriter_UTF8(txtFile);
						
						writer.write("<doc>");			 	writer.newLine();
						writer.write("<url>"+url+"</url>"); writer.newLine();
						writer.write(html);					writer.newLine();
						writer.write("</doc>");				writer.newLine();
					
					}else{					
						
						writer.write("<doc>");			 	writer.newLine();
						writer.write("<url>"+url+"</url>"); writer.newLine();
						writer.write(html);					writer.newLine();
						writer.write("</doc>");				writer.newLine();						
					}
					
					outputCount++;
				}else{	
					failedCount++;
					
					noResultWriter.write(url);
					noResultWriter.newLine();
				}				
			}
			
			//final
			writer.flush();
			writer.close();
			
			noResultWriter.flush();
			noResultWriter.close();
			
			System.out.println("SuccessCount:\t"+successCount);
			System.out.println("FailedCount:\t"+failedCount);
			logPrinter.flush();
			logPrinter.close();			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	//////////////////////////////test
	/*
	 *存在：需要将汉字编码的问题，有可能被google翻译为繁体字；
	 * */
	public static void main(String [] args){
		//1
		/*
		try{
			//System.out.println(queryUrl);
			//System.out.println(URLEncoder.encode(queryUrl, "utf-8"));
			
			StringBuffer buffer;
			//replace the query
			String oov = "american online";
			String encodedOOV = URLEncoder.encode(oov, "utf-8");
			String queryUrl = "http://www.google.com/search?hl=zh-CN&as_epq="+encodedOOV+"&num=10&start=0";
			//System.out.println(queryUrl);
			
			
			//String another = "http://www.google.cn/search?hl=zh-CN&as_epq=\"%E7%BE%8E%E5%9B%BD%E7%BA%BF%E4%B8%8A\"&q=America&num=100&start=0";
			HttpDownloader downLoader = new HttpDownloader();
			buffer = downLoader.getContent(queryUrl);
			System.out.println(buffer.toString());
		}catch(Exception e){
			e.printStackTrace();
		}	
		*/
		
		//2
		/*
		try {
			HttpDownloader.queryClueweb();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		*/
		
		//3
		HttpDownloader.getHtml("https://t.co/HgOmFMMgYZ");
		
		//4
		//HttpDownloader.getSearchLogHtml();
		
		
	}
}

