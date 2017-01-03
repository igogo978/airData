/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package air.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

/**
 *
 * @author igogo
 */
public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    enum AK {
        MP3, MSG
    }

    public static void main(String[] args) throws IOException, JavaLayerException {
        String siteStatus = null;
        String publishTime = null;
        Boolean shouldAudioPlay;

        Integer aqi = 0;
        FileHandler fh = new FileHandler("airData.log");
        logger.addHandler(fh);
        // configure the logger with handler and formatter 
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);

//        System.out.println(System.getProperty("user.dir"));
        String cwd = System.getProperty("user.dir");
        // the following statement is used to log any messages  
        //        logger.info("init the log");
        logger.info("working directory: " + cwd);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File("settings.json"));
        String epaURL = root.path("epaURL").asText();
        String targetSiteName = root.path("siteName").asText();
        shouldAudioPlay = root.path("audio").asBoolean();

//        System.out.println(shouldAudioPlay.toString());
        logger.info("觀測站台: " + targetSiteName);
        //epa 的資料 json 格式傳回
        String airData = getAirData(epaURL, logger);
        JsonNode rootAirData = mapper.readTree(airData);
        JsonNode targetSiteObj = null;
        for (JsonNode siteObj : rootAirData) {
            String siteName = siteObj.get("SiteName").asText();
            if (siteName.equals(targetSiteName)) {
                targetSiteObj = siteObj;

            }
        }
//        System.out.println(targetSiteObj.toString());
        //寫入json 資料到html 資料夾
        if (new File(cwd + "/html").exists()) {
            File airDataFile = new File(cwd + "/html/airData.json");
            if (airDataFile.createNewFile()) {
//                logger.info(cwd + "/html/airData.json");
                logger.info(airDataFile.getAbsolutePath());
            }
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(airDataFile), "utf-8"))) {

                writer.write(targetSiteObj.toString());
                logger.info("writing data to " +cwd + "/html/"
             );

            } catch (Exception e) {
                logger.info("I need to write html/airData.json");
            }
        } else {
            logger.info("html directory not exist.");
        }

        siteStatus = targetSiteObj.get("Status").asText();
        publishTime = targetSiteObj.get("PublishTime").asText();
        aqi = targetSiteObj.get("AQI").asInt();

        if (aqi == 0) {
            logger.info("無法找到觀測站");
        }
//        System.out.println(siteStatus);
        Map<AK, String> audiokey = new HashMap();
        switch (aqiLevel(aqi)) {
            case 1:
                //良好 green
                audiokey.put(AK.MP3, "./mp3/airGood.mp3");
                audiokey.put(AK.MSG, siteStatus);
                break;
            case 2:
                // "普通" yellow
                audiokey.put(AK.MP3, cwd + "/mp3/airModerate.mp3");
                audiokey.put(AK.MSG, siteStatus);
                break;
            case 3:
                // "對敏感族群不良" orange
                audiokey.put(AK.MP3, cwd + "/mp3/airUnhealthyForSensitiveGroups.mp3");
                audiokey.put(AK.MSG, siteStatus);
                break;
            case 4:
                // "對所有族群不良" red
                audiokey.put(AK.MP3, cwd + "/mp3/airUnhealthy.mp3");
                audiokey.put(AK.MSG, siteStatus);
                break;
            case 5:
                //                "非常不良" purple
                audiokey.put(AK.MP3, cwd + "/mp3/airHazardous.mp3");
                audiokey.put(AK.MSG, siteStatus);
                break;
            case 6:
                //                "有害" darkred
                audiokey.put(AK.MP3, cwd + "/mp3/airPurple.mp3");
                audiokey.put(AK.MSG, siteStatus);
                break;
            default:
                logger.info("unknow level");
                System.exit(0);
        }

        File audio = new File(audiokey.get(AK.MP3));
        if (shouldAudioPlay && audio.exists()) {
            logger.info("Now, it's going to play the mp3 file. " + audiokey.get(AK.MP3));
            FileInputStream fis = new FileInputStream(audio);
            Player playMP3 = new Player(fis);
            playMP3.play();
        }

    }

    public static Integer aqiLevel(Integer aqi) {
        Integer index = 0;
        if (aqi > 0 && aqi <= 50) {
            index = 1;
        }
        if (aqi >= 51 && aqi <= 100) {
            index = 2;
        }
        if (aqi >= 101 && aqi <= 150) {
            index = 3;
        }
        if (aqi >= 151 && aqi <= 200) {
            index = 4;
        }

        if (aqi >= 201 && aqi <= 300) {
            index = 5;
        }

        if (aqi >= 301 && aqi <= 500) {
            index = 6;
        }
        return index;
    }

    public static String getAirData(String epaURL, Logger logger) throws IOException {

        String jsonData = null;
        URL url = null;
        try {
            url = new URL(epaURL);
            InputStreamReader isr = new InputStreamReader(url.openStream(), "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            //List<String> lines = new ArrayList<>();
            String line;
            StringBuilder lines = new StringBuilder();
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                lines.append(line);
            }
            jsonData = lines.toString();
            logger.info("Great. getting data successfully from " + epaURL);

        } catch (MalformedURLException e) {
            logger.info(e.getMessage());
        }
        return jsonData;
    }
}
