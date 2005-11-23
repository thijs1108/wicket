/*
 * $Id$
 * $Revision$
 * $Date$
 *
 * ==============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package wicket.markup.html;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import wicket.IResponseFilter;
import wicket.RequestCycle;

/**
 * This is a filter that injects javascript code to the top head portion and after the body so that
 * the time can me measured what the client parse time was for this page. 
 * It also reports the total server parse/response time in the client and logs the server response time and response size
 * it took for a specific response in the server log.
 * 
 * @author jcompagner
 */
public class ServerAndClientTimeFilter implements IResponseFilter
{
	private static Log log = LogFactory.getLog(ServerAndClientTimeFilter.class);

	/**
	 * @see wicket.IResponseFilter#filter(java.lang.StringBuffer)
	 */
	public StringBuffer filter(StringBuffer responseBuffer)
	{
		int headIndex = responseBuffer.indexOf("<head>");
		int bodyIndex = responseBuffer.indexOf("</body>");
		long timeTaken = System.currentTimeMillis() - RequestCycle.get().getStartTime();
		if(headIndex != -1 && bodyIndex != -1)
		{

			StringBuffer endScript = new StringBuffer("\n<script>\nwindow.defaultStatus='Client time: ' + (new Date().getTime() - clientTimeVariable)/1000 +  's, Server time: ");
			endScript.append( ((double)timeTaken)/1000);
			endScript.append("s';\n</script>\n");
			responseBuffer.insert(bodyIndex-1, endScript);
			StringBuffer beginScript = new StringBuffer("\n<script>\nvar clientTimeVariable = new Date().getTime();\n</script>\n");
			responseBuffer.insert(headIndex+6, beginScript);
		}
		log.info( timeTaken + "ms server time taken for request " + RequestCycle.get().getRequest().getURL() + " response size: " + responseBuffer.length());
		return responseBuffer;
	}

}
