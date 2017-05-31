package nz.paulin.spaceflight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;


class Parser {
    private static final Logger logger = LogManager.getLogger(Parser.class);

    private static final int TIMEOUT = 20000;

//    private static final DateTimeFormatter SECONDS_PRESENT_FORMATTER = DateTimeFormatter.ofPattern("MMM d yyyy h:mm:ss a z");
//    private static final  DateTimeFormatter SECONDS_LESS_FORMATTER = DateTimeFormatter.ofPattern("MMM d yyyy h:mm a z");
    private static final DateTimeFormatter ZULU_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssVV");

    static List<Launch> parseLaunches(URL url) throws IOException, ParseException {
        return parseLaunches(Jsoup.parse(url, TIMEOUT));
    }

    static List<Launch> parseLaunches(String html) throws ParseException {
        return parseLaunches(Jsoup.parse(html));
    }

    private static List<Launch> parseLaunches(Document document) throws ParseException {
        List<Launch> launches = new LinkedList<>();
        Elements launchTableElements = document.select("table.launchcalendar");
        for (Element launchTableElement : launchTableElements) {
            Launch parsedLaunch = parseLaunch(launchTableElement);
            if(parsedLaunch != null) {
                launches.add(parsedLaunch);
            }
        }
        return launches;
    }

    private static Launch parseLaunch(Element launchTableElement) throws ParseException {
        try {
            Elements rows = launchTableElement.select("tr");
            Launch.Builder builder = new Launch.Builder();

            String firstRowText = rows.get(0).text();
            if(firstRowText.contains("NET")) {
                // time has not been confirmed yet
                return null;
            }
            String[] dateAndMissionTokens = firstRowText.split(" ");
            int dayOfMonth = -1;
            String abbreviatedMonth = dateAndMissionTokens[0];
            try {
                dayOfMonth = Integer.parseInt(dateAndMissionTokens[1]);
            } catch (NumberFormatException ignored) {
            }
            if (dayOfMonth == -1) {
                // don't even both trying to parse it if there's no date yet
                return null;
            }

            StringBuilder missionNameBuilder = new StringBuilder();
            for (int i = 2; i < dateAndMissionTokens.length; i++) {
                missionNameBuilder.append(dateAndMissionTokens[i]).append(" ");
            }
            builder.setMissionName(missionNameBuilder.toString().trim());

            // format looks something like this:
            // SOYUZ-2 (ARIANESPACE)
            //
            // Guiana Space Centre ELS
            // 11:47:52 AM GFT (UTC-3)
            // 1 second
            String[] tokens = rows.get(1).text()
                    .replace("Location", "\n")
                    .replace("Time", "\n")
                    .replace("Window", "\n")
                    .split("[\n]");
            builder.setLaunchVehicle(tokens[0].trim());
            builder.setLocation(tokens[1].trim());

            // time format USED to look like:
            // 11:47:52 AM GFT (UTC-3)
            // 1:13 PM ALMT (UTC+6)
            // but NOW looks like this:
            // 8:45 PM GFT / 2017-06-01 23:45:00Z
            String timeStr = tokens[2].trim();
            if("TBD".equals(timeStr)) {
                return null;
            } else {
                String s = abbreviatedMonth + " " + dayOfMonth + " " + LocalDate.now().getYear() + " " + timeStr;
//                s = s.substring(0, s.indexOf("(")-1); // this is for a time when the date and time string had the time zone in parentheses which is no longer
                ZonedDateTime time;
                try {
                    // Date time should be in the format of '8:45 PM GFT / 2017-06-01 23:45:00Z' but trim it down to
                    // `2017-06-01 23:45:00'
                    s = s.substring(s.indexOf("/") + 1, s.length()).trim();
                } catch (IndexOutOfBoundsException e) {
                    logger.debug("IndexOutOfBoundsException when parsing the line '" + s + "'", e);
                }
                try {
                    time = ZonedDateTime.parse(s, ZULU_TIME_FORMATTER);
                } catch (DateTimeParseException e) {
                    logger.debug("Unknown datetime format. Could it have gone back to the old form?");
                    return null;
                }
                builder.setTime(time);
            }


            int window = 0;
            int descriptionIndex = 4;
            if(tokens.length == 4) {
                descriptionIndex = 5;
                String[] windowTokens = tokens[3].trim().split("\\s+");
                // window format looks like:
                // 2 hours, 49 minutes
                // 30 minutes
                // 1 second
                // or missing entirely
                if (!(windowTokens.length == 2 || windowTokens.length == 4 || windowTokens.length == 6)) {
                    throw new ParseException("Unexpected number of tokens, expected either 2, 4, or 6");
                }
                try {
                    for (int i = 0; i < windowTokens.length; i += 2) {
                        int amt = Integer.parseInt(windowTokens[i]);
                        String units = windowTokens[i + 1];
                        if (units.startsWith("minute")) {
                            amt *= 60;
                        } else if (units.startsWith("hour")) {
                            amt *= 3600;
                        } else if (units.startsWith("second")) {
                            amt *= 1;
                        } else {
                            throw new ParseException("Unknown units: " + units);
                        }
                        window += amt;
                    }
                } catch (NumberFormatException e) {
                    throw new ParseException("Unable to parse window duration", e);
                }
            }
            builder.setWindow(window);

            // index 4 for no window, 5 when window present
            builder.setDescription(rows.get(descriptionIndex).text().trim());

            Launch l = builder.build();
            logger.info("Produced launch object: " + l);
            return l;
        } catch (Exception e) {
            throw new ParseException("An error occurred when parsing", e);
        }
    }
}
