package com.liu;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static net.minecraft.server.command.CommandManager.*;

public class Main implements ModInitializer {

	public static final String MOD_ID = "buildingtool";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Tool1 TOOL1 = new Tool1(new FabricItemSettings());
	public static final Tool2 TOOL2 = new Tool2(new FabricItemSettings());

	private static final String FOLDER = "./block_info/";
	private static final String DICTIONARY_FILE = "./dictionary.txt";
	private static final Map<String, Integer> DICTIONARY = new HashMap<>();

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "tool1"), TOOL1);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "tool2"), TOOL2);
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> dispatcher.register(literal("saveToFile")
						.then(argument("file", StringArgumentType.word())
								.executes(context -> {
									final LinkedList<ABlock> blocks = new LinkedList<>();
									final LinkedList<AnEntity> entities = new LinkedList<>();
									final String fileName = StringArgumentType.getString(context, "file");
									getBlocks(blocks, entities, context.getSource().getWorld(), context.getSource().getEntity());
									saveToFile(fileName, blocks, entities);
									return 1;
								}))));
		loadFile();
	}

	private void loadFile() {
		File file = new File(FOLDER);
		if (!file.exists()) {
			if (file.mkdir()) {
				LOGGER.info("Folder created");
			}
		}
		file = new File(DICTIONARY_FILE);
		if (!file.exists()) {
            try {
                if (file.createNewFile()) {
					LOGGER.info("Dictionary file created");
				}
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
		readDictionary();
	}

	private static void readDictionary() {
		try (BufferedReader reader = new BufferedReader(new FileReader(DICTIONARY_FILE))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split("%");
				DICTIONARY.put(split[0], Integer.parseInt(split[1]));
			}
		} catch (IOException e) {
			LOGGER.error(e.toString());
		}
	}

	private static void getBlocks(LinkedList<ABlock> blocks, LinkedList<AnEntity> entities, ServerWorld world, Entity player) {
		if (Tool.pos1 != null && Tool.pos2 != null) {
			for (int x = Math.min(Tool.pos1.getX(), Tool.pos2.getX()); x <= Math.max(Tool.pos1.getX(), Tool.pos2.getX()); x++) {
				for (int y = Math.min(Tool.pos1.getY(), Tool.pos2.getY()); y <= Math.max(Tool.pos1.getY(), Tool.pos2.getY()); y++) {
					for (int z = Math.min(Tool.pos1.getZ(), Tool.pos2.getZ()); z <= Math.max(Tool.pos1.getZ(), Tool.pos2.getZ()); z++) {
						BlockState state = world.getBlockState(new BlockPos(x, y, z));
						blocks.addLast(new ABlock(state.getBlock().getName().getString(), x, y, z, state.getProperties(), state));
					}
				}
			}
			for (Entity entity : world.getOtherEntities(player, new Box(Tool.pos1.getX(), Tool.pos1.getY(), Tool.pos1.getZ(), Tool.pos2.getX(), Tool.pos2.getY(), Tool.pos2.getZ()))) {
				NbtCompound compound = new NbtCompound();
				entity.writeNbt(compound);
				entities.addLast(new AnEntity(entity.getType().getName().getString(), entity.getX(), entity.getY(), entity.getZ(), compound));
			}
		}
	}

	private static void saveToFile(String fileName, LinkedList<ABlock> blocks, LinkedList<AnEntity> entities) {
		int preDictionarySize = DICTIONARY.size();
		try (FileWriter originalFileWriter = new FileWriter(FOLDER + fileName + ".original");
				FileWriter filterFileWriter = new FileWriter(FOLDER + fileName + ".filter");
				FileWriter translateFileWriter = new FileWriter(FOLDER + fileName + ".trans")) {
			originalFileWriter.write("Blocks:\n");
			filterFileWriter.write("Blocks:\n");
			translateFileWriter.write("Blocks:\n");
			for (ABlock block : blocks) {
				String blockPos = block.x + " " + block.y + " " + block.z + " ";
				String[] properties = getBlockProperty(block);
				originalFileWriter.write(blockPos + block.name + " [" + properties[0] + "]\n");
				if (!FILTER.contains(block.name)) {
					filterFileWriter.write(blockPos + block.name + " ");
					if (!DICTIONARY.containsKey(block.name)) {
						DICTIONARY.put(block.name, DICTIONARY.size());
					}
					translateFileWriter.write(blockPos + DICTIONARY.get(block.name) + " ");
					filterFileWriter.write("[" + properties[1] + "]\n");
					translateFileWriter.write("[" + properties[2] + "]\n");
				}
			}
			originalFileWriter.write("Entities:\n");
			filterFileWriter.write("Entities:\n");
			translateFileWriter.write("Entities:\n");
			for (AnEntity entity : entities) {
				String entityPos = entity.x + " " + entity.y + " " + entity.z + " ";
				String compound = entity.compound.toString() + "\n";
				originalFileWriter.write(entityPos + entity.name + " ");
				if (ENTITY_FILTER.contains(entity.name)) {
					filterFileWriter.write(entityPos + entity.name + " ");
					if (!DICTIONARY.containsKey(entity.name)) {
						DICTIONARY.put(entity.name, DICTIONARY.size());
					}
					translateFileWriter.write(entityPos + DICTIONARY.get(entity.name) + " ");
					filterFileWriter.write(compound);
					translateFileWriter.write(compound);
				}
				originalFileWriter.write(compound);
			}
		} catch (IOException e) {
			LOGGER.error(e.toString());
		}
		if (preDictionarySize != DICTIONARY.size()) {
			try (FileWriter writer = new FileWriter(DICTIONARY_FILE)) {
				for (Map.Entry<String, Integer> entry : DICTIONARY.entrySet()) {
					writer.write(entry.getKey() + "%" + entry.getValue() + "\n");
				}
			} catch (IOException e) {
				LOGGER.error(e.toString());
			}
		}
	}

	private static String[] getBlockProperty(ABlock block) {
		String[] result = new String[3];
		StringBuilder[] resultBuilder = new StringBuilder[]{new StringBuilder(), new StringBuilder(), new StringBuilder()};
		for (Property<?> property : block.property) {
			resultBuilder[0].append(property.getName()).append(":").append(block.state.get(property).toString()).append(",");
			if (RECORD.contains(property)) {
				resultBuilder[1].append(property.getName()).append(":").append(block.state.get(property).toString()).append(",");
				if (!DICTIONARY.containsKey(property.getName())) {
					DICTIONARY.put(property.getName(), DICTIONARY.size());
				}
				resultBuilder[2].append(property.getName()).append(":").append(block.state.get(property).toString()).append(",");
			}
		}
		for (int i = 0; i < 3; i++) {
			result[i] = resultBuilder[i].toString();
		}
		return result;
	}

	private static final Set<String> FILTER = Set.of(
			"Air"
	);

	private static final Set<String> ENTITY_FILTER = Set.of(
			"Painting", "Item Frame", "Armor Stand"
	);

	private static final Set<Property<?>> RECORD = Set.of(
			Properties.HORIZONTAL_FACING,
			Properties.BLOCK_HALF,
			Properties.DOOR_HINGE,
			Properties.DOUBLE_BLOCK_HALF,
			Properties.SLAB_TYPE,
			Properties.AXIS,
			Properties.BLOCK_FACE,
			Properties.INVERTED,
			Properties.CANDLES,
			Properties.LIT,
			Properties.ROTATION
	);

	private record ABlock(String name, int x, int y, int z, Collection<Property<?>> property, BlockState state) {}

	private record AnEntity(String name, double x, double y, double z, NbtCompound compound) {}
}