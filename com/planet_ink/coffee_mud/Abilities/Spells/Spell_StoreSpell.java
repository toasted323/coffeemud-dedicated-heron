package com.planet_ink.coffee_mud.Abilities.Spells;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.Abilities.StdAbility;
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
public class Spell_StoreSpell extends Spell
{
	public String ID() { return "Spell_StoreSpell"; }
    public String Name(){return "Store Spell";}
	public String name(){
        if((affected!=null)&&(CMLib.flags().isInTheGame(affected,true)))
        {
            if(spellName.length()==0)
            {
                spellName="unknown";
                int x=text().indexOf("/");
                Ability A=null;
                if(x>0)
                {
                    A=CMClass.getAbility(text().substring(0,x));
                    if(A!=null)  spellName=A.name();
                }
            }
            return "Store Spell: "+spellName;
        }
        return Name();
    }
	protected int canTargetCode(){return CAN_ITEMS;}
	public int classificationCode(){return Ability.ACODE_SPELL|Ability.DOMAIN_ENCHANTMENT;}
	protected int overrideMana(){return overridemana;}
	public String spellName="";
	protected int overridemana=-1;

	public void waveIfAble(MOB mob,
						   Environmental afftarget,
						   String message,
						   Item me)
	{
		if((mob.isMine(me))&&(!me.amWearingAt(Item.IN_INVENTORY))&&(message!=null))
		{
			Environmental target=null;
			if((mob.location()!=null))
				target=afftarget;
			String name=CMStrings.removeColors(me.name().toUpperCase());
			if(name.startsWith("A ")) name=name.substring(2).trim();
			if(name.startsWith("AN ")) name=name.substring(3).trim();
			if(name.startsWith("THE ")) name=name.substring(4).trim();
			if(name.startsWith("SOME ")) name=name.substring(5).trim();
			int x=message.toUpperCase().indexOf(name);
			if(x>=0)
			{
				message=message.substring(x+name.length());
				int y=message.indexOf("'");
				if(y>=0) message=message.substring(0,y);
				message=message.trim();
				x=text().indexOf("/");
				int charges=0;
				Ability A=null;
				if(x>0){
					charges=CMath.s_int(text().substring(x+1));
					A=CMClass.getAbility(text().substring(0,x));
				}
				if(A==null)
					mob.tell("Something seems wrong with "+me.name()+".");
				else
				if(charges<=0)
				{
					mob.tell(me.name()+" seems spent.");
					me.delEffect(this);
				}
				else
				{
					setMiscText(A.ID()+"/"+(charges-1));
					A=(Ability)A.newInstance();
					Vector V=new Vector();
					if(target!=null)
						V.addElement(target.name());
					V.addElement(message);
					mob.location().show(mob,null,CMMsg.MSG_OK_VISUAL,me.name()+" glows brightly.");
					A.invoke(mob, V, target, true,0);
				}
			}
		}
	}

	public void executeMsg(Environmental myHost, CMMsg msg)
	{
		MOB mob=msg.source();

		switch(msg.targetMinor())
		{
		case CMMsg.TYP_WAND_USE:
			if((msg.amITarget(affected))&&(affected instanceof Item))
				waveIfAble(mob,msg.tool(),msg.targetMessage(),(Item)affected);
			break;
		case CMMsg.TYP_SPEAK:
			if(msg.sourceMinor()==CMMsg.TYP_SPEAK)
				msg.addTrailerMsg(CMClass.getMsg(msg.source(),affected,msg.target(),CMMsg.NO_EFFECT,null,CMMsg.MASK_ALWAYS|CMMsg.TYP_WAND_USE,msg.targetMessage(),CMMsg.NO_EFFECT,null));
			break;
		default:
			break;
		}
		super.executeMsg(myHost,msg);
	}

	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto, int asLevel)
	{
		if(commands.size()<2)
		{
			mob.tell("Store which spell onto what?");
			return false;
		}
		Environmental target=mob.location().fetchFromMOBRoomFavorsItems(mob,null,(String)commands.lastElement(),Item.WORNREQ_UNWORNONLY);
		if((target==null)||(!CMLib.flags().canBeSeenBy(target,mob)))
		{
			mob.tell("You don't see '"+((String)commands.lastElement())+"' here.");
			return false;
		}
		if(!(target instanceof Item))
		{
			mob.tell("You can't enchant '"+target.name()+"'.");
			return false;
		}

		Item item=(Item)target;

		commands.removeElementAt(commands.size()-1);

		String spellName=CMParms.combine(commands,0).trim();
		Spell wandThis=null;
		for(int a=0;a<mob.numAbilities();a++)
		{
			Ability A=mob.fetchAbility(a);
			if((A!=null)
			&&(A instanceof Spell)
			&&((!A.savable())||(CMLib.ableMapper().qualifiesByLevel(mob,A)))
			&&(A.name().toUpperCase().startsWith(spellName.toUpperCase()))
			&&(!A.ID().equals(this.ID())))
				wandThis=(Spell)A;
		}
		if(wandThis==null)
		{
			mob.tell("You don't know how to enchant anything with '"+spellName+"'.");
			return false;
		}
        if((CMLib.ableMapper().lowestQualifyingLevel(wandThis.ID())>24)
        ||(((StdAbility)wandThis).usageCost(null,true)[0]>45))
		{
			mob.tell("That spell is too powerful to store.");
			return false;
		}
		Ability A=item.fetchEffect(ID());
		if((A!=null)&&(A.text().length()>0)&&(!A.text().startsWith(wandThis.ID()+"/")))
		{
			mob.tell("'"+item.name()+"' already has a different spell stored in it.");
			return false;
		}
		else
		if(A==null)
		{
			A=(Ability)copyOf();
			A.setMiscText(wandThis.ID()+"/0");
		}
		int charges=0;
		int x=A.text().indexOf("/");
		if(x>=0) charges=CMath.s_int(A.text().substring(x+1));
		overridemana=-1;
		int mana=usageCost(mob,true)[0]+wandThis.usageCost(mob,true)[0];
		if(mana>mob.maxState().getMana())
			mana=mob.maxState().getMana();
		overridemana=mana;

		// lose all the mana!
		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;
		overridemana=-1;

		boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			setMiscText(wandThis.ID());
			CMMsg msg=CMClass.getMsg(mob,target,this,verbalCastCode(mob,target,auto),"^S<S-NAME> move(s) <S-HIS-HER> fingers around <T-NAMESELF>, incanting softly.^?");
			if(mob.location().okMessage(mob,msg))
			{
				mob.location().send(mob,msg);
				if(target.fetchEffect(ID())==null)
				{
					A.setInvoker(mob);
					target.addNonUninvokableEffect(A);
				}
				A.setMiscText(wandThis.ID()+"/"+(charges+1));
				mob.location().show(mob,target,null,CMMsg.MSG_OK_VISUAL,"<T-NAME> glow(s) softly.");
			}

		}
		else
			beneficialWordsFizzle(mob,target,"<S-NAME> move(s) <S-HIS-HER> fingers around <T-NAMESELF>, incanting softly, and looking very frustrated.");


		// return whether it worked
		return success;
	}
}
