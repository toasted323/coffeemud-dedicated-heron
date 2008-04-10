package com.planet_ink.coffee_mud.Libraries.interfaces;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;
import java.text.*;

/* 
   Copyright 2000-2008 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public interface TimeManager extends CMLibrary
{
	public final static long MILI_SECOND=1000;
	public final static long MILI_MINUTE=MILI_SECOND*60;
	public final static long MILI_HOUR=MILI_MINUTE*60;
	public final static long MILI_DAY=MILI_HOUR*24;
	public final static long MILI_WEEK=MILI_DAY*7;
	public final static long MILI_MONTH=MILI_DAY*30;
	public final static long MILI_YEAR=MILI_DAY*365;
    
    /**
     * Returns the numeric representation of the month
     * 
     * <br><br><b>Usage:</b> Month2MM("January");
     * @param Month The month name
     * @return String The number of the month as a string
     */ 
    public String month2MM(String Month);
    /**
     * Return the name of the month, given a number
     * 
     * <br><br><b>Usage:</b> String Mnth=ReturnMonthName(m,GiveShort).charStats();
     * @param Number Month number to convert
     * @param GiveShort Give abbreviation if true
     * @return String Month name
     */
    public String getMonthName(int Number, boolean GiveShort);
    
    /**
     * Converts a string of some form into a Calendar object.
     * 
     * <br><br><b>Usage:</b> Calendar.S2Date(GetRes(Results,"StartDateTime"));
     * @param TheDate The string to retrieve from
     * @return Calendar Calendar object
     */
    public long string2Millis(String TheDate);
    
    /**
     * Converts a string of some form into a Calendar object.
     *
     * <br><br><b>Usage:</b> Calendar.string2Date(GetRes(Results,"StartDateTime"));
     * @param TheDate The string to retrieve from
     * @return Calendar Calendar object
     */
    public Calendar string2Date(String TheDate);
    
    
    
    /**
     * Returns the regular Hours given the hours in the 
     * international format (military time)
     * 
     * <br><br><b>Usage:</b> ConvertHour(GetIn(req, "ENDHR"))
     * @param TheHour Hours in military format
     * @return String Hours in regular format
     **/
    public String convertHour(String TheHour);
    
    /**
     * Returns the AMPM stamp given the international Hours portion the Time
     * 
     * <br><br><b>Usage:</b> getAMPM(GetIn(req, "ENDHR"))
     * @param TheHour Hours in military format
     * @return String AM or PM stamp
     **/
    public String getAMPM(String TheHour);
    
    /**
     * Get the zone id given the timezone string
     * 
     * <br><br><b>Usage:</b> GetTheZoneID(MeetTZ.getRawOffset())+"\n";
     * @param theRawOffset The time zone's raw offset to convert
     * @return String The time zone ID
     */
    public String getTheIntZoneID(int theRawOffset);
    
    /**
     *  Returns the time zone of the given ID
     * 
     * <br><br><b>Usage:</b> MEETZN = GetTheTimeZone(ID);
     * @param theID The ID of the abbreviated time zone.
     * @return String The time zone name
     */
    public String getTheTimeZone(String theID);
    
    
    /**
     * Returns the month for a given date
     * 
     * <br><br><b>Usage:</b> String ENDMM=d2MMString();
     * @param time The time in miliseconds
     * @return String The month name
     **/
    public String date2MonthString(long time);

    /**
     * Returns the DD portion of a given date
     * 
     * <br><br><b>Usage:</b> String ENDDD=d2DDString();
     * @param time The time in miliseconds
     * @return String The day
     **/
    public String date2DayOfMonthString(long time);

    /**
    * Returns the YYYY portion of a given date
    * Returns the DD portion of a given date
    * 
    * <br><br><b>Usage:</b> String ENDYYYY=d2YYYYString();
     * @param time The time in miliseconds
    * @return String The year
    **/
    public String date2YYYYString(long time);

    /**
    * Returns the Hours portion of a given Time
    * 
    * <br><br><b>Usage:</b> String ENDHR=T2HRString();
    * @param time time used
    * @return String The hour
    **/
    public String date2HRString(long time);
    

    /**
    * Returns the Minutes portion of a given Time
    * 
    * <br><br><b>Usage:</b> String ENDMIN=T2MINString();
    * @param time The time in miliseconds
    * @return String The minutes
    **/
    public String date2MINString(long time);
    
    /**
     * format the date for an smtp message
     * 
     * <br><br><b>Usage:</b>  msgDateFormat(98374987234)
    * @param time The time in miliseconds
    * @return String The minutes
     */
     public String smtpDateFormat(long time); 
     
    /**
     *  Returns the time zone of the server
     * 
     * <br><br><b>Usage:</b> MEETZN = T2ZoneString();
     * @param time The time in miliseconds
     * @return String The time zone
     */
    public String date2ZoneString(long time);
    /**
     * Returns the Minutes portion of a given Time
     * 
     * <br><br><b>Usage:</b> String ST_AMPM=T2_AMPMString();
     * @param time The time in miliseconds
     * @return String AM or PM stamp
     **/
    public String date2AMPMString(long time);
    public String date2String(Calendar C);
    /**
     * Converts a given date into a string of form:
     * MM/DD/YYYY HH:MM AP
     * 
     * <br><br><b>Usage:</b> d2String()
     * @param time The time in miliseconds
     * @return String Formatted date/time
     */
    public String date2String(long time);
    /**
     * Converts a given date into a string of form:
     * MM/DD/YYYY HH:MM AP
     * 
     * <br><br><b>Usage:</b> d2SString()
     * @param time The time in miliseconds
     * @return String Formatted date/time
     */
    public String date2SecondsString(long time);
    /**
     * Converts a given date into a string of form:
     * MM/DD/YYYY
     * 
     * <br><br><b>Usage:</b> d2DString()
     * @param time The time in miliseconds
     * @return String Formatted date
     */
    public String date2DateString(long time);
    /**
     * Converts a given date into a string of form:
     * MM/DD/YY
     * 
     * <br><br><b>Usage:</b> d2D2String()
     * @param time The time in miliseconds
     * @return String Formatted date
     */
    public String date2Date2String(long time);
}
