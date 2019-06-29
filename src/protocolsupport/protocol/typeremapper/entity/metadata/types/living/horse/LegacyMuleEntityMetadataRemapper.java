package protocolsupport.protocol.typeremapper.entity.metadata.types.living.horse;

import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.typeremapper.entity.metadata.FirstDataWatcherUpdateObjectAddRemapper;
import protocolsupport.protocol.utils.ProtocolVersionsHelper;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectByte;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectVarInt;

public class LegacyMuleEntityMetadataRemapper extends CargoHorseEntityMetadataRemapper {

	public LegacyMuleEntityMetadataRemapper() {
		addRemap(new FirstDataWatcherUpdateObjectAddRemapper(14, new DataWatcherObjectVarInt(2)), ProtocolVersion.MINECRAFT_1_10);
		addRemap(new FirstDataWatcherUpdateObjectAddRemapper(13, new DataWatcherObjectVarInt(2)), ProtocolVersionsHelper.ALL_1_9);
		addRemap(new FirstDataWatcherUpdateObjectAddRemapper(19, new DataWatcherObjectByte((byte) 2)), ProtocolVersion.getAllBetween(ProtocolVersion.MINECRAFT_1_6_1, ProtocolVersion.MINECRAFT_1_8));
	}

}
