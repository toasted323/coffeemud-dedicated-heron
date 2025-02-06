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
   Copyright 2005-2024 Bo Zimmerman

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
public class HelpList extends StdCommand
{
	public HelpList()
	{
	}

	private final String[] access=I(new String[]{"HELPLIST","HLIST"});
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
		if(CMLib.help().getHelpFile().size()==0)
		{
			mob.tell(L("No help is available."));
			return false;
		}
		if(helpStr.length()==0)
		{
			mob.tell(L("You must enter a search pattern.  Use 'TOPICS' or 'COMMANDS' for an unfiltered list."));
			return false;
		}
		final List<String> matches =
					CMLib.help().getHelpList(
					helpStr,
					CMLib.help().getHelpFile(),
					CMSecurity.isAllowed(mob,mob.location(),CMSecurity.SecFlag.AHELP)?CMLib.help().getArcHelpFile():null,
					mob);
		if((matches==null)||(matches.size()==0))
		{
			mob.tell(L("No help entries match '@x1'.\nEnter 'COMMANDS' for a command list, or 'TOPICS' for a complete list.",helpStr));
			Log.helpOut("Help",mob.Name()+" wanted help list match on "+helpStr);
		}
		else
		if(!mob.isMonster())
		{
			final String matchText = CMLib.lister().build4ColTable(mob,matches).toString();
			mob.session().getOutputFormatter().wraplessPrintln(L("^xHelp File Matches:^.^?\n\r^N@x1",matchText.replace('_',' ')));
		}
		return false;
	}

	@Override
	public boolean canBeOrdered()
	{
		return true;
	}

}

