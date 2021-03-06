package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import protocolsupport.api.ProtocolVersion;
import protocolsupport.listeners.InternalPluginMessageRequest;
import protocolsupport.listeners.internal.ChunkUpdateRequest;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleChunk;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.tile.TileEntityRemapper;
import protocolsupport.protocol.typeremapper.block.LegacyBlockData;
import protocolsupport.protocol.typeremapper.chunk.ChunkTransformerBB;
import protocolsupport.protocol.typeremapper.chunk.ChunkTransformerPE;
import protocolsupport.protocol.typeremapper.chunk.EmptyChunk;
import protocolsupport.protocol.typeremapper.pe.PEDataValues;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.utils.types.ChunkCoord;
import protocolsupport.protocol.utils.types.TileEntity;
import protocolsupport.utils.recyclable.RecyclableArrayList;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableEmptyList;

import java.util.function.Consumer;

public class Chunk extends MiddleChunk {

	public Chunk(ConnectionImpl connection) {
		super(connection);
	}

	private final ChunkTransformerBB transformer = new ChunkTransformerPE(
		LegacyBlockData.REGISTRY.getTable(connection.getVersion()),
		TileEntityRemapper.getRemapper(connection.getVersion()),
		connection.getCache().getTileCache(),
		PEDataValues.BIOME.getTable(connection.getVersion())
	);

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		if (full || (bitmask == 0xFFFF)) { //Only send full or 'full' chunks to PE.
			RecyclableArrayList<ClientBoundPacketData> packets = RecyclableArrayList.create();
			ProtocolVersion version = connection.getVersion();
			cache.getPEChunkMapCache().markSent(chunk);
			transformer.loadData(chunk, data, bitmask, cache.getAttributesCache().hasSkyLightInCurrentDimension(), full, tiles);
			ClientBoundPacketData chunkpacket = ClientBoundPacketData.create(PEPacketIDs.CHUNK_DATA);
			PositionSerializer.writePEChunkCoord(chunkpacket, chunk);
			Consumer<ByteBuf> chunkDataProducer = chunkdata -> {
				transformer.writeLegacyData(chunkdata);
				chunkdata.writeByte(0); //borders
				for (TileEntity tile : transformer.remapAndGetTiles()) {
					ItemStackSerializer.writeTag(chunkdata, true, version, tile.getNBT());
				}
			};
			if (version.isAfterOrEq(ProtocolVersion.MINECRAFT_PE_1_12)) {
				ByteBuf tmpBuf = Unpooled.buffer(512);
				chunkDataProducer.accept(tmpBuf);
				VarNumberSerializer.writeVarInt(chunkpacket, tmpBuf.readUnsignedByte());
				chunkpacket.writeByte(0);
				ArraySerializer.writeVarIntByteArray(chunkpacket, tmpBuf);

			} else {
				ArraySerializer.writeVarIntByteArray(chunkpacket, chunkDataProducer);
			}
			packets.add(chunkpacket);
			return packets;
		} else { //Request a full chunk.
			InternalPluginMessageRequest.receivePluginMessageRequest(connection, new ChunkUpdateRequest(chunk));
			return RecyclableEmptyList.get();
		}
	}

	public static void addFakeChunks(RecyclableCollection<ClientBoundPacketData> packets, ChunkCoord coord, ProtocolVersion version) {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				packets.add(createEmptyChunk(new ChunkCoord(coord.getX() + x, coord.getZ() + z), version));
			}
		}
	}

	public static void writeEmptyChunk(ByteBuf out, ChunkCoord chunk, ProtocolVersion version) {
		PositionSerializer.writePEChunkCoord(out, chunk);
		if (version.isAfterOrEq(ProtocolVersion.MINECRAFT_PE_1_12)) {
			out.writeBytes(EmptyChunk.getPEChunkData112());
		} else {
			out.writeBytes(EmptyChunk.getPEChunkData());
		}
	}

	public static ClientBoundPacketData createEmptyChunk(ChunkCoord chunk, ProtocolVersion version) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.CHUNK_DATA);
		writeEmptyChunk(serializer, chunk, version);
		return serializer;
	}

	public static void writeChunkPublisherUpdate(ByteBuf out, int x, int y, int z) {
		VarNumberSerializer.writeSVarInt(out, x);
		VarNumberSerializer.writeVarInt(out, y);
		VarNumberSerializer.writeSVarInt(out, z);
		VarNumberSerializer.writeVarInt(out, 512); //radius, gets clamped by client
	}

	public static ClientBoundPacketData createChunkPublisherUpdate(int x, int y, int z) {
		ClientBoundPacketData networkChunkUpdate = ClientBoundPacketData.create(PEPacketIDs.NETWORK_CHUNK_PUBLISHER_UPDATE_PACKET);
		writeChunkPublisherUpdate(networkChunkUpdate, x, y, z);
		return networkChunkUpdate;
	}

}