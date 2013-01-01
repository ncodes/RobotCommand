package com.wordpress.ncodes;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Developer: Kennedy Idialu kennedyidialu@gmail.com
 * Date: 1/1/13
 * Time: 9:57 AM
 */
public class RobotCommand
{
    /**
     * Cache to store rules from multiple host
     */
    private HashMap<String, HashMap<String, ArrayList<String>>> cache;

    /**
     * Crawl delay
     */
    public int crawlDelay = 0;


    /**
     * Initialize the object
     */
    public RobotCommand()
    {
        cache = new HashMap<String, HashMap<String, ArrayList<String>>>();
    }

    protected String getHost(String url) throws MalformedURLException
    {
        URL u = new URL(url);
        return u.getHost();
    }

    /**
     * Determine if a url can be accessed based on the host robots.txt rules
     * @param url the url
     * @param userAgent the user agent
     * @return true is allowed, otherwise false if not allowed
     * @throws Exception
     */
    public boolean allow(String url, String userAgent) throws  Exception
    {
        // Get the host from the url
        String host = getHost(url);

        // check if host is not in cache, fetch, parse and add to cache
        if (!cache.containsKey(host))
        {
            String robotTxt = fetchRobotsTxt(url);
            cache(host, robotTxt);
        }

        HashMap<String, ArrayList<String>> c = cache.get("www.google.com.ng");
        c.put("bot", new ArrayList<String>(){{
            add("disallow: /etc");
        }});

        // determine if url is allowed
        return isAllowed(host, url, userAgent);

    }


    /**
     * Checks weather a url is allowed
     * @param host the host where the robots.txt file resides
     * @param url the url to check
     * @param ua the user agent of the calling process
     * @return true us allowed, otherwise false
     */
    protected boolean isAllowed(String host, String url, String ua)
    {
        ArrayList<String> defaultUARules = null;
        ArrayList<String> uaRules = null;
        boolean allowed = true;

        // Get the cached rules of host
        HashMap<String, ArrayList<String>> hostRules = cache.get(host);

        // Get rules associated with default user agent, if available
        if (hostRules.containsKey("*"))
            defaultUARules = hostRules.get("*");

        // Get the rules associated with the passed user agent, if available
        if (hostRules.containsKey(ua))
            uaRules = hostRules.get(ua);

        // now check if default user agent rules allows or disallows url
        if (defaultUARules != null)
        {
            // determine allow or disallow
            allowed = matches(url, defaultUARules);
        }

        // If user agent was passed
        if (uaRules != null)
        {
            // determine allow or disallow
            allowed = matches(url, uaRules);
        }

        return allowed;
    }


    /**
     * Checks if a list of rules can be found in a url
     * @param url the url
     * @param rules the list of rules
     * @return true if an allowed match is found or false if a disallowed match is found, otherwise return true
     */
    protected boolean matches(String url, List<String> rules)
    {
        boolean allow = true;

        for(String r: rules)
        {
            // separate the rule from the directive
            String directive = r.split(":")[0].trim();
            String rule = r.split(":")[1].trim();

            // escape regex options in rule
            rule = rule.replaceAll("\\*", ".*");
            rule = rule.replaceAll("\\?","[?]");

            // create the pattern and matcher
            Pattern pattern = Pattern.compile(rule);
            Matcher matcher = pattern.matcher(url);

            // if directive is 'allow' and a match is found, set allow to true
            if (directive.equals("allow"))
            {
                if (matcher.find())
                    allow = true;
            }

            // if directive is 'disallow' and a match is found, set allow to false
            else
            {
                if (matcher.find())
                    allow = false;
            }
        }

        return allow;
    }

    /**
     * Parse and create cache from robot txt
     * @param host the host where robots.txt file was downloaded from
     * @param txt the robots.txt content
     */
    protected void cache(String host, String txt)
    {
        // parse robot txt
        parse(txt, host);
    }

    /**
     * Gets the value by separating the directive from the rule
     * @param line the rule
     * @return a string representing the value
     */
    private String getLineValue(String line)
    {
        return line.split(":")[1].trim();
    }

    /**
     * Parse the robots.txt file and cache using host as key
     * @param txt the robots.txt content
     * @param host the host to use as key
     */
    private void parse(String txt, String host)
    {
        String currentUA = "";

        HashMap<String, ArrayList<String>> rulesList = new HashMap<String, ArrayList<String>>();

        // first, change txt to lowercase
        txt = txt.toLowerCase();

        // get all lines in txt
        String[] txtLines = txt.split("\n");

        // start parsing
        for (String line : txtLines)
        {
            if (line.startsWith("user-agent"))
            {
                // get the value of this line/rule
                String ua = getLineValue(line);

                // add user agent to rules list
                if (!rulesList.containsKey(ua))
                    rulesList.put(ua, new ArrayList<String>());

                // set current user agent
                currentUA = ua;
            }
            else
            {

                // process rule
                if (line.startsWith("allow") || line.startsWith("disallow"))
                {
                    // get rules has map of the current user agent
                    ArrayList<String> rules = rulesList.get(currentUA);

                    // if add rule
                    rules.add(line);
                }

                // Get delay
                if (line.startsWith("crawl-delay"))
                    crawlDelay = Integer.parseInt(getLineValue(line));
            }
        }

        cache.put(host, rulesList);
    }

    /**
     * Constructs the robots.txt url
     *
     * @param urlObj the url object
     * @return a url object to the robots.txt file
     * @throws MalformedURLException
     */
    protected URL constructRobotTxtURL(URL urlObj) throws MalformedURLException
    {
        String port = (urlObj.getPort() != -1) ? ":"+urlObj.getPort() : "";
        return new URL(urlObj.getProtocol() + "://" + urlObj.getHost() + port + "/robots.txt");
    }

    /**
     * Fetch the robots.txt file
     * @param theUrl the url
     * @throws IOException
     */
    private String fetchRobotsTxt(String theUrl) throws IOException
    {
        // construct robots.txt location
        URL robotTxtLoc = constructRobotTxtURL(new URL(theUrl));

        InputStream is = robotTxtLoc.openStream();
        int ptr = 0;
        StringBuilder buffer = new StringBuilder();

        while ((ptr = is.read()) != -1)
        {
            buffer.append((char)ptr);
        }

        return buffer.toString();
    }

    public static void main(String[] args)
    {
        RobotCommand rc = new RobotCommand();

        try
        {
            rc.allow("http://www.google.com.ng/et", "bot");
            rc.allow("http://www.google.com.ng/etc", "bot");
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
}
