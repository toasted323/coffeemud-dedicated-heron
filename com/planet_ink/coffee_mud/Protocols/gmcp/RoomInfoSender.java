package com.planet_ink.coffee_mud.Protocols.gmcp;

import com.planet_ink.coffee_mud.Locales.interfaces.Room;
import com.planet_ink.coffee_mud.MOBS.interfaces.MOB;
import com.planet_ink.coffee_mud.core.CMLib;
import com.planet_ink.coffee_mud.core.CMath;
import com.planet_ink.coffee_mud.core.Directions;
import com.planet_ink.coffee_mud.core.MiniJSON;

public class RoomInfoSender {
	public String sendRoomInfo(MOB mob) {
		if (mob == null) return null;
		Room room = mob.location();
		if (room == null) return null;

		StringBuilder doc = new StringBuilder("room.info {");
		String roomID = CMLib.map().getExtendedRoomID(room);
		String domType = (room.domainType() & Room.INDOORS) == 0
				? Room.DOMAIN_OUTDOOR_DESCS[room.domainType()]
				: Room.DOMAIN_INDOORS_DESCS[CMath.unsetb(room.domainType(), Room.INDOORS)];

		doc.append("\"num\":").append(CMath.abs(roomID.hashCode())).append(",")
				.append("\"id\":\"").append(roomID).append("\",")
				.append("\"name\":\"").append(MiniJSON.toJSONString(room.displayText(mob))).append("\",")
				.append("\"zone\":\"").append(MiniJSON.toJSONString(room.getArea().name())).append("\",")
				.append("\"desc\":\"").append(MiniJSON.toJSONString(room.description(mob))).append("\",")
				.append("\"terrain\":\"").append(domType.toLowerCase()).append("\",")
				.append("\"details\":\"\",")
				.append("\"exits\":{");

		boolean comma = false;
		for (int d = 0; d < Directions.NUM_DIRECTIONS(); d++) {
			Room R2 = room.getRoomInDir(d);
			if ((R2 != null) && (room.getExitInDir(d) != null)) {
				String room2ID = CMLib.map().getExtendedRoomID(R2);
				if (room2ID.length() > 0) {
					if (comma) doc.append(",");
					comma = true;
					doc.append("\"").append(CMLib.directions().getDirectionChar(d)).append("\":")
							.append(CMath.abs(room2ID.hashCode()));
				}
			}
		}
		doc.append("},\"coord\":{\"id\":0,\"x\":-1,\"y\":-1,\"cont\":0}");
		doc.append("}");
		return doc.toString();
	}
}
