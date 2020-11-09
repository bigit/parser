package rk.miller.aliparser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ZipyParser {

    public static final String URL = "https://gpsfront.aliexpress.com/getRecommendingResults.do?widget_id=5547572&platform=pc&limit=10&phase=1";
    public static final String FILE_NAME = "result.csv";

    public static void main(String[] args) {

        ArrayNode jsonResult = new ArrayNode(null);
        String postback = "";
        int i = 0;

        while (i <= 10) {
            String response = ZipyParser.getData(URL + "&offset=" + i * 10+postback);
            try {
                JsonNode jsonResponse = new ObjectMapper().readTree(response);
                if (postback.isEmpty()) {
                    postback = "&postback="+jsonResponse.get("postback").asText();
                }
                ArrayNode jsonNode = jsonResponse.withArray("results");
                jsonResult.addAll(jsonNode);
                i++;
            } catch (JsonProcessingException e) {
                System.out.println("Invalid json object");
            }
        }

        CsvSchema.Builder builder = CsvSchema.builder();
        jsonResult.get(0).fieldNames().forEachRemaining(builder::addColumn);
        CsvSchema schema = builder.build().withColumnSeparator(';').withHeader();

        CsvMapper mapper = new CsvMapper();
        try( FileWriter fileWriter = new FileWriter(FILE_NAME, false)){
            mapper.writerFor(JsonNode.class)
                    .with(schema)
                    .writeValuesAsArray(fileWriter)
                    .writeAll(jsonResult)
                    .flush();
        }
        catch (IOException e) {
            System.out.printf("Unable write data to file. Reason: %s%n", e.getMessage());
        }
    }

    static String getData(String sourceUrl) {
        try {
            URL url = new URL(sourceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String readLine;
            StringBuilder buffer = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((readLine = br.readLine()) != null) {
                buffer.append(readLine);
            }
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
