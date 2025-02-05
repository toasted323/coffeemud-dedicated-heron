package com.planet_ink.coffee_mud.Commands;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2004-2024 Bo Zimmerman

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
public class AHelp extends StdCommand
{
	public AHelp()
	{
	}

	private final String[] access=I(new String[]{"ARCHELP","AHELP"});
	@Override
	public String[] getAccessWords()
	{
		return access;
	}

	@Override
	public boolean execute(final MOB mob, final List<String> commands, final int metaFlags)
		throws java.io.IOException
	{
		final String helpStr=CMParms.combine(commands,1);
		if(CMLib.help().getArcHelpFile().size()==0)
		{
			mob.tell(L("No archon help is available."));
			return false;
		}
		StringBuffer thisTag=null;
		if(helpStr.length()==0)
		{
			thisTag=Resources.getFileResource("help/arc_help.txt",true);
			if((thisTag!=null)&&(helpStr.equalsIgnoreCase("more")))
			{
				StringBuffer theRest=(StringBuffer)Resources.getResource("arc_help.therest");
				if(theRest==null)
				{
					final List<String> ableIV=new ArrayList<String>();
					theRest=new StringBuffer("");

					for(final Enumeration<Ability> a=CMClass.abilities();a.hasMoreElements();)
					{
						final Ability A=a.nextElement();
						if((A!=null)&&((A.classificationCode()&Ability.ALL_ACODES)==Ability.ACODE_PROPERTY))
							ableIV.add(A.ID());
					}
					if(ableIV.size()>0)
					{
						theRest.append("\n\rProperties:\n\r");
						theRest.append(CMLib.lister().build4ColTable(mob,ableIV));
					}

					ableIV.clear();
					for(final Enumeration<Ability> a=CMClass.abilities();a.hasMoreElements();)
					{
						final Ability A=a.nextElement();
						if((A!=null)&&((A.classificationCode()&Ability.ALL_ACODES)==Ability.ACODE_DISEASE))
							ableIV.add(A.ID());
					}
					if(ableIV.size()>0)
					{
						theRest.append("\n\rDiseases:\n\r");
						theRest.append(CMLib.lister().build4ColTable(mob,ableIV));
					}

					ableIV.clear();
					for(final Enumeration<Ability> a=CMClass.abilities();a.hasMoreElements();)
					{
						final Ability A=a.nextElement();
						if((A!=null)&&((A.classificationCode()&Ability.ALL_ACODES)==Ability.ACODE_POISON))
							ableIV.add(A.ID());
					}
					if(ableIV.size()>0)
					{
						theRest.append("\n\rPoisons:\n\r");
						theRest.append(CMLib.lister().build4ColTable(mob,ableIV));
					}

					ableIV.clear();
					for(final Enumeration<Ability> a=CMClass.abilities();a.hasMoreElements();)
					{
						final Ability A=a.nextElement();
						if((A!=null)&&((A.classificationCode()&Ability.ALL_ACODES)==Ability.ACODE_SUPERPOWER))
							ableIV.add(A.ID());
					}
					if(ableIV.size()>0)
					{
						theRest.append("\n\rSuper Powers:\n\r");
						theRest.append(CMLib.lister().build4ColTable(mob,ableIV));
					}

					ableIV.clear();
					for(final Enumeration<Ability> a=CMClass.abilities();a.hasMoreElements();)
					{
						final Ability A=a.nextElement();
						if((A!=null)&&((A.classificationCode()&Ability.ALL_ACODES)==Ability.ACODE_TECH))
							ableIV.add(A.ID());
					}
					if(ableIV.size()>0)
					{
						theRest.append("\n\rTech Skills:\n\r");
						theRest.append(CMLib.lister().build4ColTable(mob,ableIV));
					}

					ableIV.clear();
					for(final Enumeration<Behavior> b=CMClass.behaviors();b.hasMoreElements();)
					{
						final Behavior B=b.nextElement();
						if(B!=null)
							ableIV.add(B.ID());
					}
					if(ableIV.size()>0)
					{
						theRest.append("\n\r\n\rBehaviors:\n\r");
						theRest.append(CMLib.lister().build4ColTable(mob,ableIV));
					}
					Resources.submitResource("arc_help.therest",theRest);
				}
				thisTag=new StringBuffer(thisTag.toString());
				thisTag.append(theRest);
			}
		}
		else
		{
			final Pair<String,String> textP = CMLib.help().getHelpMatch(helpStr,CMLib.help().getArcHelpFile(),mob, 0);
			if((textP != null) && (textP.second != null))
			{
				thisTag=new StringBuffer(textP.second.toString());
				final List<String> seeAlso = CMLib.help().getSeeAlsoHelpOn(mob, CMLib.help().getArcHelpFile(), helpStr, textP.first, textP.second, 5);
				if(seeAlso.size()>0)
				{
					final String alsoHelpStr = CMLib.english().toEnglishStringList(seeAlso);
					thisTag.append("\n\rSee also help on: "+alsoHelpStr);
				}
			}
		}
		if(thisTag==null)
		{
			mob.tell(L("No archon help is available on @x1 .\n\rEnter 'COMMANDS' for a command list, or 'TOPICS' for a complete list.",helpStr));
			Log.errOut("Help: "+mob.Name()+" wanted archon help on "+helpStr);
		}
		else
		if(!mob.isMonster())
			mob.session().getOutputFormatter().wraplessPrintln(thisTag.toString());
		return false;
	}

	@Override
	public boolean canBeOrdered()
	{
		return true;
	}

	@Override
	public boolean securityCheck(final MOB mob)
	{
		return CMSecurity.isAllowed(mob,mob.location(),CMSecurity.SecFlag.AHELP);
	}

}
