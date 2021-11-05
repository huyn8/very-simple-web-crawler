import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @Author Huy Nguyen
 * Simple Crawl
 * @Description The program downloads the html from the a startingURL which is provided via command line
 * as the first argument to the program. It then parses the html to find the first <a href > reference to 
 * to other URLs as the number of hops (which is the second argument) allows. The number of URLs visited
 * will be the same as the number of hops. The site visited will be printed out. No sites will be duplicated 
 * twice. 
 *
 */
public class WebCrawler {
	static int hops; //number of URL hops to perform
	static Queue<String> links = new LinkedList<String>(); //stack for links
    static Set<String> uniqueSet = new HashSet<String>(); //set for duplicates
	static Pattern pattern = Pattern.compile("<a href=\"(http[s]?://(.*?))\"", Pattern.DOTALL); //pattern for finding href tag and http link
    static Matcher matcher; //matcher for pattern
    static int count = 0; //enumerates all visited links
    
	/**
	 * Main function to run program by calling the execute function (see execute())
	 * @param args: an array of command line arguments (expected 2 max)
	 * @throws IOException for any input and output exception when parsing
	 * @return none
	 * @precondition: valid number of arguments (2 expected)
	 * @postcondition: none
	 */
	public static void main(String[] args) throws IOException {
		if(args.length != 2) {
			System.out.println("Invalid number of arguments, 2 required");
			return;
		}
		
		links.add(args[0]); //adding starting link	  			
		hops = Integer.parseInt(args[1]); //number of hops to crawl
		
		while (hops != 0 && links.size() != 0) { //hop until hops run out or links run out
			execute(); //calls execute function to run program
		}
		System.out.println("Program finished with total: " + count + " hop(s)");//prints out number of hops
	}
	
	/**
	 * Execute function to perform http server request, receive http response, and
	 * parse html body from http response for http links. The hops operation will
	 * perform similar to DFS or "Depth First" hops. Depth-first hops work by accessing 
	 * the first link (if any) on an html body. If no links exist on the current link, terminate 
	 * the program regardless of the number of hops. If there are still links but no hops left,
	 * terminate as well.
	 * @param args: none
	 * @throws IOException for any input and output exception when parsing
	 * @return none
	 * @precondition: valid starting http and valid number of hops >= 0
	 * @postcondition: none
	 */	
	public static void execute() {
		while(true) {//run until either links or hops run out
			if(links.isEmpty()) {//no more links in queue
				System.out.println("No more links, program terminated");
				return;
			}
			if(hops == 0) {//no more hops to access links
				System.out.println("No more hops, program terminated");
				return;
			}
			else {
				String urlString = links.remove();//remove next link from queue for url connection
				boolean redirected = false;//checker for redirection
				uniqueSet.add(cleanURL(urlString)); //add cleaned URL to visited list
				try {
					BufferedReader reader; //buffer to read in string data from input stream 
					URL url = new URL(urlString); //create new url
					HttpURLConnection connection = (HttpURLConnection)url.openConnection(); //access url
					connection.setReadTimeout(5000);//time out check for sites that don't respond in 5s
					if(connection.getConnectTimeout() == 5000) {//throw error for sites that don't respond
						throw new HttpConnectTimeoutException("ERROR");
					}
					if(connection.getResponseCode() >= 300 && connection.getResponseCode() <= 399) {//redirection code 3xx
						urlString = connection.getHeaderField("Location"); //getting relocated URL & clean URL (no trailing /)
						url = new URL(urlString); //reseting URL object
						connection = (HttpURLConnection)url.openConnection(); //reseting access connection to URL
						redirected = true;//set redirected true for later
					}
					if(connection.getResponseCode() >= 400 && connection.getResponseCode() <= 499) {//dis-functional url, code 4xx
						System.out.print("CODE " + connection.getResponseCode() + " ");
						throw new MalformedURLException();
					}
					if(connection.getResponseCode() >= 500 && connection.getResponseCode() <= 599) {//server error, code 5xx
						System.out.print("CODE " + connection.getResponseCode() + " ");
						throw new UnknownHostException();
					}
					//success 2xx code and new link
					urlString = cleanURL(urlString);
					links.clear();//clear queue if it's a new link to store new link's links
					hops--;//successful connection means a hop has been made
					count++;//for counting visited links		
					if(redirected) {//when true, add redirected link to uniqueSet instead
						redirected = false;
						uniqueSet.add(urlString); //add redirected and cleaned URL to visited list
					}
					System.out.println("Visited: " + urlString + " [" + count + "]");//print visited links
					reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));//read in html body
					String line; //individual line parsing on html body
					boolean newLinkFound = false;//to break out of loop and go immediately to next link 
					while((line = reader.readLine()) != null && hops != 0) { //parse all lines on html body to find links	
						if(newLinkFound) {//break loop and go to link (no need to parse the rest of the html body)
							break;
						}
						matcher = pattern.matcher(line);//match pre-defined pattern on each line to find a href tag and link
						while(matcher.find()) { //a line may contain multiple valid links, find all links
							//System.out.println(matcher.group(1));
							if(!uniqueSet.contains(cleanURL(matcher.group(1)))) {//add url to visited list
								links.add(cleanURL(matcher.group(1)));//adding new link(s) to queue (FIFO)
								newLinkFound = true;//for breaking outer loop
								break;//break inner loop
							}
						}
					}
				}
				//error handling
				catch (MalformedURLException e) {//code 4xx
					System.out.println("ERROR: " + urlString + " is not valid URL");//url errors
				}
				catch (IOException e) {//code 5xx
					if(e.getMessage().indexOf("Read timed out") >= 0) {
						System.out.println("ERROR: session timed out because " + urlString + " did not respond");//time out error
					}
					else {
						System.out.println("ERROR: " + urlString + " is not a valid site");//other IO errors
					}
				}
			}	
		}
	}	
	
	/**
	 * cleanURL function to clean all incoming URLs from forward slash trailing at
	 * the end of an URL and s in https for use in visited list and URL connection accessing
	 * @param url: raw string of URL
	 * @return url: string of cleaned URL (no trailing /)
	 * @precondition: valid URL
	 * @postcondition: none
	 */	
	public static String cleanURL(String url) {
		String trailingMark = url.substring(url.length()-1);
		String onlyHttp1 = "";
		String onlyHttp2 = "";
		if(url.indexOf('s') == 4) {//get rid of the s in https
			onlyHttp1 = url.substring(0, 4);
			onlyHttp2 = url.substring(5);
			url = onlyHttp1+onlyHttp2;
		}
		if(trailingMark.indexOf('/') < 0) {//return if no trailing /
			return url;
		}
		else {
			url = url.substring(0, url.lastIndexOf('/'));//get rid of trailing /
		}
		return url;
	}
}