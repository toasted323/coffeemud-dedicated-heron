package com.planet_ink.coffee_mud.Protocols.gmcp;

import com.planet_ink.coffee_mud.Locales.interfaces.Room;
import com.planet_ink.coffee_mud.MOBS.interfaces.MOB;

public class RoomInfoSender {
	public String sendRoomInfo(MOB mob) {
		if (mob == null) return null;
		Room room = mob.location();
		if (room == null) return null;

		return new RoomInfoBuilder(room, mob).build();
	}
}
