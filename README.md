# very-simple-web-crawler

## Description:
This program will tilize HTTP programming to “GET” and crawl parts of the HTML based Web.  

The java application takes two arguments from the command line: 
1) A URL as a starting point 
2) The number of hops from that URL (num_hops) 
 
The program will download the html from the starting URL which is provided as the first 
argument to the program.  It will parse the html finding the first \<a href> reference to other 
absolute URLs, for instance: https://www.w3schools.com/tags/att_a_href.asp. Only unique pages are visisted.
The application will then download the html from that page and repeat the operation.  If that page is not accessible then continue on the current page 
looking for the next reference and visit that reference.  The program will do this num_hops times.
