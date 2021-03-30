import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class WeatherForecastApplication {

    private final double latitude = 60.044729;
    private final double longitude = 30.373789;
    private final String apiKey = "acdec43da9d0463d5ec4d26070622070";
    private final String requestUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=" + latitude + "&lon=" +
            longitude + "&units=metric&exclude=minutely,hourly,alerts" + "&appid=" + apiKey;

    public static void main(String[] args) {
        new WeatherForecastApplication().run();
    }

    public void run() {
        List<Weather> weatherList = parseUrl();
        forecast(weatherList);
    }

    private List<Weather> parseUrl() {
        List<Weather> weatherList = new ArrayList<>();
        try {
            System.out.println("Connecting to https://api.openweathermap.org ...");
            Document document = Jsoup.connect(requestUrl)
                    .header("Content-Type", "application/json")
                    .ignoreContentType(true)
                    .get();

            System.out.println("Parsing data...");
            Element body = document.body();

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(body.ownText());

            JSONObject current = (JSONObject) jsonObject.get("current");
            Weather currentWeather = new Weather(
                    0.0,
                    0.0,
                    Long.parseLong(current.get("pressure").toString()),
                    Long.parseLong(current.get("dt").toString())
            );
            weatherList.add(currentWeather);

            JSONArray dailyList = (JSONArray) jsonObject.get("daily");
            dailyList.stream().forEach(element -> {
                JSONObject daily = (JSONObject) element;
                JSONObject temp = (JSONObject) daily.get("temp");

                Weather weather = new Weather(
                        Double.parseDouble(temp.get("night").toString()),
                        Double.parseDouble(temp.get("morn").toString()),
                        Long.parseLong(daily.get("pressure").toString()),
                        Long.parseLong(daily.get("dt").toString())
                );
                weatherList.add(weather);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return weatherList;
    }

    private void forecast(List<Weather> list) {
        Weather dayWithMaxPressure = list.stream().limit(6)
                .max(Comparator.comparingLong(Weather::getPressure)).orElse(new Weather());

        Weather dayWithMinTempDiff = list.stream().skip(1)
                .min(Comparator.comparingDouble(Weather::getTemperatureDifference)).orElse(new Weather());

        System.out.format("\nMax pressure = %d hPa, date = %s",
                dayWithMaxPressure.getPressure(), dayWithMaxPressure.getDate());

        System.out.format("\nMin temperature difference= %.2fC, night= %.2fC, morning= %.2fC, date: %s\n",
                dayWithMinTempDiff.getTemperatureDifference(), dayWithMinTempDiff.getNightTemperature(),
                dayWithMinTempDiff.getMorningTemperature(), dayWithMinTempDiff.getDate());
    }

    static class Weather {
        private double nightTemperature;
        private double morningTemperature;
        private long pressure;
        private long timestamp;

        public Weather() {
        }

        public Weather(double nightTemperature, double morningTemperature, long pressure, long timestamp) {
            this.nightTemperature = nightTemperature;
            this.morningTemperature = morningTemperature;
            this.pressure = pressure;
            this.timestamp = timestamp;
        }

        public double getNightTemperature() {
            return nightTemperature;
        }

        public double getMorningTemperature() {
            return morningTemperature;
        }

        public long getPressure() {
            return pressure;
        }

        public double getTemperatureDifference() {
            return Math.abs(morningTemperature - nightTemperature);
        }

        public String getDate() {
            DateFormat format = new SimpleDateFormat("dd-MM-yyyy");
            Date date = new Date(timestamp * 1000);
            return format.format(date);
        }
    }
}