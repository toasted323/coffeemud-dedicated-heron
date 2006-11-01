package com.planet_ink.coffee_mud.Abilities.Specializations;
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


/* 
   Copyright 2000-2006 Bo Zimmerman

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
public class Specialization_Natural extends Specialization_Weapon
{
	public String ID() { return "Specialization_Natural"; }
	public String name(){ return "Hand to hand combat";}
	public Specialization_Natural()
	{
		super();
		weaponType=Weapon.CLASS_NATURAL;
	}
	private final static String[] EXPERTISES={"UNARMEDSTRIKE","UNARMEDSLICE","UNARMEDPIERCE","UNARMEDBASH"};
	private final static String[] EXPERTISE_NAMES={"Unarmed Striking","Unarmed Slicing","Unarmed Piercing","Unarmed Bashing"};
	private final static String[] EXPERTISE_STATS={"DEX","STR","STR","STR"};
	private final static int[] EXPERTISE_LEVELS={24,27,27,27};
	private final int[] EXPERTISE_DAMAGE_TYPE={0,Weapon.TYPE_SLASHING,Weapon.TYPE_PIERCING,Weapon.TYPE_BASHING};
	protected String[] EXPERTISES(){return EXPERTISES;}
	protected String[] EXPERTISES_NAMES(){return EXPERTISE_NAMES;}
	protected String[] EXPERTISE_STATS(){return EXPERTISE_STATS;}
	protected int[] EXPERTISE_LEVELS(){return EXPERTISE_LEVELS;}
	protected int[] EXPERTISE_DAMAGE_TYPE(){return EXPERTISE_DAMAGE_TYPE;}


	public void executeMsg(Environmental myHost, CMMsg msg)
	{
		if((activated)
		&&(CMLib.dice().rollPercentage()<25)
		&&(affected instanceof MOB)
		&&(msg.amISource((MOB)affected))
		&&(msg.targetMinor()==CMMsg.TYP_WEAPONATTACK)
		&&((msg.tool()==null)))
			helpProficiency((MOB)affected);
	}


	public void affectEnvStats(Environmental affected, EnvStats affectableStats)
	{
		activated=false;
		super.affectEnvStats(affected,affectableStats);
		if((affected instanceof MOB)&&(((MOB)affected).fetchWieldedItem()==null))
		{
			activated=true;
			affectableStats.setAttackAdjustment(affectableStats.attackAdjustment()
					+(int)Math.round(15.0*(CMath.div(proficiency(),100.0)))
					+(10*(getXLevel((MOB)affected,0))));
		}
	}
}
