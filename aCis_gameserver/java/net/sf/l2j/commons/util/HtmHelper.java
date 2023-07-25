package net.sf.l2j.commons.util;

import java.util.StringTokenizer;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.util.htm.IntToken;
import net.sf.l2j.commons.util.htm.StringToken;

import net.sf.l2j.gameserver.model.item.DropData;

public class HtmHelper
{
	protected static final CLogger LOGGER = new CLogger(HtmHelper.class.getName());
	protected static final String CB_PATH = "data/html/CommunityBoard/";
	
	public boolean isTextLongerThan(String text, int maxLength)
	{
		//TO DO: implement that
		return false;
	}
	
	//if boolean = false, just remove tags
	public static String removeSection(String text, String begin, String end, boolean removeSection)
	{
		if (text == null)
		{
			return null;
		}
		
		if (!removeSection) 
		{
			text = text.replace(begin, "");
			text = text.replace(end, "");
			return text;
		}
		
		var beginIndex = text.indexOf(begin);
		var endIndex = text.indexOf(end) + end.length();
		
		var partBegin = text.substring(0, beginIndex);
		var partEnd = text.substring(endIndex);
		
		return partBegin + partEnd;
		
	}
	
	public static String replaceHtmTags(String text)
	{
		if (text == null)
		{
			return null;
		}
		
		text = text.replace(">", "&gt;");
		text = text.replace("<", "&lt;");
		//text = text.replace("\n", "<br1>");
		
		return text;
	}
	
	/*
	public static ClientTextEntry getSubstringByPixels(String text, int lenLimit)
	{
		var result = new ClientTextEntry(text);
		
		int len = 0;
		int textLength = 0;
		for (int i = 0; i < text.length(); i++)
		{
			textLength = len;
			
			var letter = text.charAt(i);
			if (!Config.CHARACTERS_PIXELS.containsKey(letter))
			{
				LOGGER.error("Unknown character lenght: " + letter);
			}
			else
			{
				var charLen = Config.CHARACTERS_PIXELS.get(letter) + 1; // 1 pixel space every symbol
				len = len + charLen;
			}
			 
			if (len >= lenLimit)
			{
				if (i > 0) i--;
				
				result.setShortenedText(text.substring(0, i));
				result.setIsCut(true);
				result.setLengthInPixels(textLength);
				return result;
			 }
		}
		
		
		
		result.setIsCut(false);
		result.setLengthInPixels(len);
		return result;
	}
	*/
	
	//because in Interlude we can use only one font type and size, we can calculate its length
	/*
	public static int inGameTextLength(String text)
	{
		if (text == null) 
		{
			return 0;
		}
		
		int len = 0;
		
		 for (int i = 0; i < text.length(); i++)
		 {
			 var letter = text.charAt(i);
			 if (!Config.CHARACTERS_PIXELS.containsKey(letter))
			 {
				 LOGGER.error("Unknown character lenght: " + letter);
			 }
			 else
			 {
				 var charLen = Config.CHARACTERS_PIXELS.get(letter) + 1; // 1 pixel space every symbol
				 len = len + charLen;
			 }
		 }
		 return len;
	}
	*/
	
	public static String GetSortIcon(SortType sortType)
	{
		switch (sortType)
		{
			case ASC:
				return "L2UI_CH3.shortcut_expand";
			case DESC:
				return "L2UI_CH3.shortcut_minimize";
			case NONE:
				return "L2UI_CH3.FrameCloseBtn";
		}
		
		return "";
	}
	

	public static String FormatLevel(int myLevel, int level)
	{
		if (myLevel<level)
		{
			return "<font color=cc3300>"+level+"</font>";
		}
		
		return "<font color=33cc33>"+level+"</font>";
	}
	

	public static String FormatAsPercentage(int chance)
	{
		double result = (double)(chance * 100)/DropData.MAX_CHANCE;
		String displayNumber = String.format("%.3f", result);
		
		if (result <5)
		{
			return "<font color=CC4400>"+displayNumber+"%</font>";
		}
		if (result <25)
		{
			return "<font color=ff6600>"+displayNumber+"%</font>";
		}
		
		if (result <50)
		{
			return "<font color=FF9933>"+displayNumber+"%</font>";
		}
		if (result<75)
		{
			return "<font color=66ff99>"+displayNumber+"%</font>";
		}
		if (result<100)
		{
			return "<font color=009900>"+displayNumber+"%</font>";
		}

		if (result==100)
		{
			return "<font color=009933>"+displayNumber+"%</font>";
		}



		
		return String.format("%.3f", result);
	}
	
	public static String FormatAmount(int amount)
	{
		if (amount>=1000000)
		{
			return Math.round((double)amount/1000000)+"kk";
		}
		if (amount>=1000)
		{
			return Math.round((double)amount/1000)+"k";
		}
		return String.valueOf(amount);
	}
	
	
	public static String HighlightText(String text, String toHighlight, String colorAsRGB)
	{
		int start = text.toLowerCase().indexOf(toHighlight.toLowerCase());
		int end = start + toHighlight.length();
		
		return text.substring(0,start)+"<font color="+colorAsRGB+">"+text.substring(start,end)+"</font>"+text.substring(end);
	}
	
	public static String InsertAtIndex(String source, String toInsert, int index) {
	    String bagBegin = source.substring(0,index);
	    String bagEnd = source.substring(index);
	    return bagBegin + toInsert + bagEnd;
	}
	
	public static StringBuilder GeneratePagingHtml(int pagesCount, int page, String bypassPref, String bypassSuff)
	{
		return GeneratePagingHtml(pagesCount, page, bypassPref, bypassSuff, false);
	}
	
	public static StringBuilder GenerateCBPagingHtml(int pagesCount, int page, String bypassPref, String bypassSuff)
	{
		return GeneratePagingHtml(pagesCount, page, bypassPref, bypassSuff, true);
	}
	
	private static StringBuilder GeneratePagingHtml(int pagesCount, int page, String bypassPref, String bypassSuff, boolean isCB)
	{
		final StringBuilder sbPager = new StringBuilder();

		Pagination pagin = GetPagesNew(pagesCount,page,2);
		
		String bypassH = isCB ? "" : "-h ";
		
		if(pagin.ShowFirstPage)
		{
			sbPager.append("<td FIXWIDTH=30 align=right><a action=\"bypass " + bypassH +bypassPref+"1"+bypassSuff+"\">1</a></td><td FIXWIDTH=15>...</td>");
		}
		else
		{
			sbPager.append("<td FIXWIDTH=45 align=center>&nbsp;</td>");
			
		}
		
		for (int i = 0; i < pagin.AvailablePages.size();i++)
		{
			if (pagin.AvailablePages.get(i) == -1)// empty space
			{
				sbPager.append("<td FIXWIDTH=30 align=center>&nbsp;</td>");
			}
			else if(pagin.AvailablePages.get(i)==page)
			{
				sbPager.append("<td FIXWIDTH=30 align=center>"+pagin.AvailablePages.get(i)+"</td>");
			}
			else
			{
				sbPager.append("<td FIXWIDTH=30 align=center><a action=\"bypass "+ bypassH +bypassPref+pagin.AvailablePages.get(i)+bypassSuff+"\">"+pagin.AvailablePages.get(i)+"</a></td>");
			}
		}
		
		
		if(pagin.ShowLastPage)
		{
			sbPager.append("<td FIXWIDTH=15>...</td><td FIXWIDTH=30 align=left><a action=\"bypass "+ bypassH +bypassPref+pagesCount+bypassSuff+"\">"+pagesCount+"</a></td>");
		}
		else
		{
			sbPager.append("<td FIXWIDTH=45>&nbsp;</td>");
		}

		return sbPager;
	}
	
	 public static Pagination GetPagesNew(int totalPages, int currentPage, int additionalPages)
	 {
	 	Pagination pagin = new Pagination();
	 
    //TODO:	List<Integer> AVAIL_PAGES = new ArrayList<>();
        // calculate total, start and end pages eg total = 10,curr =  5, additionalPages = 1
    	// 1..4 5 6 .. 10
    	//    1 2 3 .. 10
    	
    	pagin.ShowFirstPage = totalPages > 0 && currentPage - additionalPages > 1; //eg. 3-1 = 2, show 1
    	pagin.ShowLastPage = (currentPage + additionalPages) < totalPages;//eg. 15 > 6 (we have 6th page, and 30 max, 30-15 = 15, from 15 will be displayed)
    	
    	pagin.EmptySpaceLeft = (additionalPages + 1) - currentPage;
    	pagin.EmptySpaceRight = (additionalPages + currentPage) - totalPages;
    	
    	for (int i = 0 ;i<pagin.EmptySpaceLeft;i++)
    	{
    		pagin.AvailablePages.add(-1);
    	}
    	
        int startPage = currentPage - additionalPages;
        int endPage = currentPage + additionalPages;
        if (startPage <= 0)
        {
            //endPage -= (startPage - 1);
        	pagin.EmptySpaceLeft = (-1* startPage)-1;
            startPage = 1;
        }
        if (endPage > totalPages)
        {
            endPage = totalPages;
        }

        for (int i = startPage; i <= endPage;i++)
        {
        	pagin.AvailablePages.add(i);
        }
        
    	for (int i = 0 ;i<pagin.EmptySpaceRight;i++)
    	{
    		pagin.AvailablePages.add(-1);
    	}
        
        return pagin;
    }
	 
	/***
	 * Returns number as formatted string with commas separating every 3 digits eg. 1,001,123,121
	 * @param price
	 * @return
	 */
	public static String GetFortmattedPrice(int price)
	{
		String priceAsText = Integer.toString(price);
	
		String numberWithCommas = "";
		
		int numLength = priceAsText.length();
	    for (int i=0; i<numLength; i++) { 
	        if ((numLength-i)%3 == 0 && i != 0) {
	        	numberWithCommas += ",";
	        }
	        numberWithCommas += priceAsText.charAt(i);
	    }
		return numberWithCommas;
	}
	 
	public static String GetEnchantHtml(int enchant)
	{
		if (enchant<=0)
		{
			return "";
		}
		return "<font color=FF6600>+"+enchant+"&nbsp;</font>";
	}	
	
	public static String toTimeString(long totalSeconds)
	{
		long day = totalSeconds / (24 * 3600);
		totalSeconds = totalSeconds % (24 * 3600);
		long hour = totalSeconds / 3600;
		totalSeconds %= 3600;
		long minutes = totalSeconds / 60 ;
		totalSeconds %= 60;
	 	long seconds = totalSeconds;
	 
	 	String returnText = "";
	 	if (day > 0)
	 	{
	 		returnText = day + " d " + hour + " h ";
	 	} 
	 	else if (hour > 0)
	 	{
	 		returnText = hour + " h ";
	 	} 
	 	returnText += (minutes + " min " + seconds + " sec");
	 	return returnText;
	}
	
	public static StringToken getNextString(StringTokenizer tokenizer)
	{
		var result = new StringToken();
		
		if (!tokenizer.hasMoreTokens())
		{
			result.addError("There is no more tokens");
			return result;
		}
		var nextToken = tokenizer.nextToken();
		
		if (nextToken.isBlank())
		{
			result.addError("String is empty");
			return result;
		}
		result.setValue(nextToken);
		return result;
	}
	
	public static IntToken getNextInt(StringTokenizer tokenizer)
	{
		var result = new IntToken();
		
		if (!tokenizer.hasMoreTokens())
		{
			result.addError("There is no more tokens");
			return result;
		}
		
		var nextToken = tokenizer.nextToken();
		var intValue = 0;
		
		try
		{
			intValue = Integer.parseInt(nextToken);
		}
		catch (NumberFormatException e)
		{
			result.addError("Value is not number");
			return result;
		}

		result.setValue(intValue);
		return result;
	}
	 
	private static class SingletonHolder
	{
		protected static final HtmHelper _instance = new HtmHelper();
	}
	
	public static HtmHelper getInstance()
	{
		return SingletonHolder._instance;
	}
}