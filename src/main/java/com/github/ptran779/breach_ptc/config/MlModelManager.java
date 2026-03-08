package com.github.ptran779.breach_ptc.config;

import com.github.ptran779.breach_ptc.BreachPtc;
import com.github.ptran779.breach_ptc.ai.api.DataManager;
import com.github.ptran779.email.ML;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MlModelManager {
//  public static final Path MODEL_DIR = Path.of("config/breach_ptc/brainmodel/");//fixme put this somewhere else
//  public static final Path DATA_DIR = Path.of("config/breach_ptc/data/");
//  public static final Path EXT_DATA_DIR = Path.of("config/breach_ptc/externaldata/");
	public static Path MODEL_DIR;
	public static Path DATA_DIR;
	public static Path EXT_DATA_DIR;
  private static long lastGameTick = 0;
  private static final Map<UUID, MLUnit> mLib = new HashMap<>();

  // Unit handler so everything compact and in one place
  public static class MLUnit {
    public long lastAccess;
    public int inSize = 0;
    public int outSize = 0;

    @Nullable
    public ML model;
    public ML model2;  // use this to store tmp model that was used for training
    public DataManager dataManager;

    // for empty model
    public MLUnit(long lastAccess) {
      this.lastAccess = lastAccess;
    }
  }

	public static void initializePaths(MinecraftServer server) {
		Path worldPath = server.getWorldPath(LevelResource.ROOT).resolve("breach_ptc");

		MODEL_DIR = worldPath.resolve("brainmodel");
		DATA_DIR = worldPath.resolve("data");
		EXT_DATA_DIR = worldPath.resolve("externaldata");

		// Create the directories if they don't exist
		try {
			Files.createDirectories(MODEL_DIR);
			Files.createDirectories(DATA_DIR);
			Files.createDirectories(EXT_DATA_DIR);
		} catch (IOException e) {
			// Log the error—if this fails, the ML units will crash on save/load
			e.printStackTrace();
		}
	}

  public static MLUnit getMUnit(UUID uuid, long gameTick) {
    MLUnit mItem = mLib.get(uuid);
    if (mItem == null) {
      mItem = loadOrCreateModelUnit(uuid, gameTick);
      mLib.put(uuid, mItem);
    }
    else {mItem.lastAccess = gameTick;}

    // do some cleanup if needed
    if (gameTick - lastGameTick > 24000) {
      cleanCache(gameTick);
      lastGameTick = gameTick;
    }
    return mItem;
  }

  public static void writeModelUnit(UUID uuid) {
    try {
      MLUnit mItem = mLib.get(uuid);
      Path file = MODEL_DIR.resolve(uuid.toString() + ".bin");
      if (mItem == null) return; // nothing to save
      if (mItem.model == null) {return;}
      byte[] mData = mItem.model.diskSerialize();
      byte[] dData = null;
      if (mItem.dataManager != null) {dData = mItem.dataManager.diskSerialize();}

      if (mData.length != 0) {
        Files.createDirectories(file.getParent());
        Files.write(file, mData);
	      BreachPtc.LOGGER.info("[{} Debug] Saved ML model for {}", BreachPtc.MOD_ID, uuid);
      } else {
        if (Files.deleteIfExists(file)) {
	        BreachPtc.LOGGER.info("[{} Debug] Purged empty ML model for {}", BreachPtc.MOD_ID, uuid);
        }
      }
      // write data
      Path file2 = DATA_DIR.resolve(uuid.toString() + ".bin");
      if (dData != null && dData.length != 0){
        Files.createDirectories(file2.getParent());
        Files.write(file2, dData); // write bytes to file
	      BreachPtc.LOGGER.info("[{} Debug] Saved ML Data for {}", BreachPtc.MOD_ID, uuid);
      } else {
        if (Files.deleteIfExists(file2)) {
	        BreachPtc.LOGGER.info("[{} Debug] Purged empty ML Data for {}", BreachPtc.MOD_ID, uuid);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static MLUnit loadOrCreateModelUnit(UUID uuid, long gameTick) {
    Path modelf = MODEL_DIR.resolve(uuid.toString() + ".bin");
    MLUnit mItem = new MLUnit(gameTick);
    mLib.put(uuid, mItem);                       // add to the library
    if (Files.exists(modelf)) {
	    BreachPtc.LOGGER.info("[AeisOps] Loading ML model from file {}", uuid);
      try {
        byte[] datMB = Files.readAllBytes(modelf);    // <-- read the file
//        MLUnit mItem = new MLUnit(gameTick);
        ML model = ML.diskDeserialize(datMB);            // <-- give the bytes to your static method
        if (model != null) {
          mItem.model = model;
          mItem.inSize = model.getInsize();
          mItem.outSize = model.getOutsize();
          // load dataManaging
          Path dataf = DATA_DIR.resolve(uuid.toString() + ".bin");
          if (Files.exists(dataf)) {
            byte[] datDB = Files.readAllBytes(dataf);
            DataManager datM = DataManager.diskDeserialize(datDB);
            if (datM != null) {
	            BreachPtc.LOGGER.info("[AeisOps] Loading ML data from file {}", uuid);
              mItem.dataManager = datM;
            }
          }
        };

        // set MLSystem IO
        return mItem;
      } catch (Exception e) {
        e.printStackTrace();
        return mItem;  //crash me // create new model here
      }
    }
    return mItem;  // need null
  }

  private static void cleanCache(long gameTick) {
    mLib.entrySet().removeIf(e -> {
      if (gameTick - e.getValue().lastAccess > 24000) {
        writeModelUnit(e.getKey()); // persist to disk before removing
        return true;           // remove from the cache
      }
      return false;
    });
  }

  public static void cleanAll() {
    BreachPtc.LOGGER.info("Cleaning up ML models ");
    for (UUID uuid : mLib.keySet()) {
	    BreachPtc.LOGGER.info("Saving ML model for {}", uuid);
      writeModelUnit(uuid);  // persist each model
    }
    mLib.clear();         // remove all from memory
  }

  public static List<String> getAvailableCsvs() {
    // 1. Create directory if it doesn't exist
    if (!Files.exists(EXT_DATA_DIR)) {
      try {
        Files.createDirectories(EXT_DATA_DIR);
      } catch (IOException e) {
        e.printStackTrace();
        return new ArrayList<>();
      }
    }

    // 2. Scan for .csv files
    try (Stream<Path> stream = Files.list(EXT_DATA_DIR)) {
      return stream
          .filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .sorted()
          .collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace(); // Log error if permission denied, etc.
      return new ArrayList<>();
    }
  }

  public static String importData(MLUnit currentUnit, String fileName) {
    long startTime = System.currentTimeMillis();
    java.io.File file = MlModelManager.EXT_DATA_DIR.resolve(fileName).toFile();
    if (!file.exists()) {
      return "File not found";
    }

    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
      String header = br.readLine();
      if (header == null) {
        return "File has no header";
      }

      int fileInSize = -1;
      int fileOutSize = -1;

      String[] parts = header.split(",");
      for (String p : parts) {
        p = p.trim().toUpperCase();
        if (p.startsWith("IN:")) {
          fileInSize = Integer.parseInt(p.substring(3));
        } else if (p.startsWith("OUT:")) {
          fileOutSize = Integer.parseInt(p.substring(4));
        }
      }
      if (fileInSize != currentUnit.inSize || fileOutSize != currentUnit.outSize) {
        return "Import Rejected: File config (IN:" + fileInSize + ", OUT:" + fileOutSize +
            ") does not match Brain (IN:" + currentUnit.inSize + ", OUT:" + currentUnit.outSize + ")";
      }
	    br.readLine();  // skip 2nd header
      if (currentUnit.dataManager != null) {
        currentUnit.dataManager.readCsvBody(br, currentUnit.inSize);
        // Calculate time take for import
        long duration = System.currentTimeMillis() - startTime;
        return "Import successful (" + duration + "ms)";
      } else {
        return "There was no data manager to hold the data";
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "Import Crash / Error Somewhere";
    }
  }

  public static @NotNull String exportData(MLUnit currentUnit, String fileName, String header) {
    if (currentUnit.dataManager == null || currentUnit.dataManager.getRawDat().isEmpty()) {
      return "No data to export";
    }
    // 1. Resolve Path
    File file = MlModelManager.EXT_DATA_DIR.resolve(fileName).toFile();
    // Ensure parent folder exists fixme might need to check for all?
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }

    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
      // 2. WRITE HEADER
      // Format: HEADER_IGNORE,IN:12,OUT:4
      bw.write("HEADER_EXPORT,IN:" + currentUnit.inSize + ",OUT:" + currentUnit.outSize);
      bw.newLine();
	    // 2. Human/Python Column Header (The long ass string)
	    bw.write(header);
	    bw.newLine();
      // 3. WRITE BODY
      // rawDat is List<List<itemUnit>>, where each inner List is one "Game Sequence"
      int gameID = 0;
      for (java.util.List<DataManager.itemUnit> sequence : currentUnit.dataManager.getRawDat()) {
        for (DataManager.itemUnit unit : sequence) {
          StringBuilder sb = new StringBuilder();
          // Column 0: Game ID
          sb.append(gameID).append(",");
          // Column 1..N: Inputs
          for (float val : unit.input()) {
            sb.append(val).append(",");
          }
          // Column N+1: Action
          sb.append(unit.action()).append(",");
          // Column N+2: Score
          sb.append(unit.score());
          bw.write(sb.toString());
          bw.newLine();
        }
        // Increment ID for the next sequence/list
        gameID++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "Failed to export data";
    }
    return "Export successful";
  }
}
