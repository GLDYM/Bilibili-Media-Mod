package dev.polaris_light.bilibili_media.util;

import dev.polaris_light.bilibili_media.BiliBiliMedia;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BilibiliMediaUtil {
    private static Map<String, String> DATA;

    public static Path getDownloadPath(){
        var p = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles");
        if (!p.toFile().isDirectory() && !p.toFile().mkdir()) {
            throw new RuntimeException("文件夹创建失败");
        }
        return p;
    }

    public static URI getUri(String fileName){
        return URI.create("http://127.0.0.1:" + SimpleFileServer.PORT + "/download?file=" + fileName);
    }


    public static void loadJson(){
        var p = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").resolve("video.json");
        if(!p.toFile().isFile()){
            try {
                if(!p.toFile().createNewFile()){
                    throw new IOException("文件创建失败");
                }
                DATA = new LinkedHashMap<>();
                saveJson();
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            DATA = gson.fromJson(new String(Files.readAllBytes(p)), new TypeToken<Map<String, String>>() {}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveJson(){
        var p = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").resolve("video.json");
        if(!p.toFile().isFile()){
            try {
                if(!p.toFile().createNewFile()){
                    throw new IOException("文件创建失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.write(p, gson.toJson(DATA).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static String tryGetLocalFile(String url){
        String fileName = DATA.get(url);
        return fileName == null ? null : fileName;
    }

    public static void updateVideoFile(String url, String fileName){
        if (DATA.containsKey(url)) {
            DATA.remove(url);
        }
        DATA.put(url, fileName);
        saveJson();
    }

    public static void clearCache(int maxSize) {
        File dir = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            BiliBiliMedia.LOGGER.error("目录创建失败: {}", dir.getAbsolutePath());
            return;
        }

        List<String> invalidKeys = new ArrayList<>();
        for (Map.Entry<String, String> entry : DATA.entrySet()) {
            File file = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").resolve(entry.getValue()).toFile();
            if (!file.exists()) {
                invalidKeys.add(entry.getKey());
            }
        }

        for (String key : invalidKeys) {
            DATA.remove(key);
            BiliBiliMedia.LOGGER.info("移除失效缓存: {}", key);
        }

        while (DATA.size() > maxSize) {
            String oldestKey = DATA.keySet().iterator().next();
            String fileName = DATA.remove(oldestKey);

            File file = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").resolve(fileName).toFile();
            if (file.exists() && !file.delete()) {
                BiliBiliMedia.LOGGER.warn("无法删除文件: {}", file.getAbsolutePath());
            }
        }

        saveJson();
    }



    /** 递归删除文件夹 */
    private static void deleteDirectory(File dir) {
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    if (!f.delete()) {
                        BiliBiliMedia.LOGGER.warn("无法删除文件: {}", f.getAbsolutePath());
                    }
                }
            }
        }
        if (!dir.delete()) {
            BiliBiliMedia.LOGGER.warn("无法删除目录: {}", dir.getAbsolutePath());
        }
    }
}
