package nz.paulin.spaceflight;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Parses the launch schedule on http://www.spaceflightinsider.com and then saves the launches with times into the given calendar.
 */
public class LaunchCal {
    private static final Logger logger = LogManager.getLogger(LaunchCal.class);

    public static void main(String[] args) {
        System.getProperty("log4j.configurationFile", "log4j2.xml");
        try {
            setPropertiesFromFile("properties.txt");

            String calendarId = System.getProperty("calendar.id");

            logger.info("Check properties loaded...");
            testProperties();
            logger.info("Getting calendar...");
            Calendar calendar = getCalendarService();
            logger.info("Testing calendar credentials...");
            testCalendarCredentials(calendar, calendarId);
            logger.info("Parsing launches...");
            List<Launch> launches = Parser.parseLaunches(new URL("http://www.spaceflightinsider.com/launch-schedule/"));
            logger.info("Adding launches to calendar...");
            addLaunchesToCalendar(launches, calendar, calendarId);
            logger.info("Done.");
            logger.info("------------------------------------------");
        } catch (Exception e) {
            final String from = System.getProperty("notifier.sender");
            final String to = System.getProperty("notifier.recipient");
            File todaysLogFile = new File("logs", getLogFileName());
            logger.debug(Notifier.getExceptionDescription(e));
            logger.info("An error occurred, notifying humans...");
            Notifier.sendEmail(from, to, "Error thrown by launch-cal", e, Collections.singletonList(todaysLogFile));
            logger.info("Done.");
            logger.info("------------------------------------------");
        }
    }

    private static File getFile(String fileName) throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream(fileName);
        File tmp;
        FileOutputStream tmpOs = null;
        try {
            tmp = File.createTempFile("xml", "tmp");
            tmpOs = new FileOutputStream(tmp);
            int len;
            byte[] b = new byte[4096];
            while ((len = is.read(b)) != -1) {
                tmpOs.write(b, 0, len);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ignored) {}
            try {
                if(tmpOs != null) {
                    tmpOs.close();
                }
            } catch (Exception ignored) {}
        }
        return tmp;
    }

    private static String getLogFileName() {
        LocalDate date = LocalDate.now();
        String base = "launch-cal";
        String twoDigitMonth = (date.getMonthValue() < 10 ? "0"+date.getMonthValue() : ""+date.getMonthValue());
        String twoDigitDay = (date.getDayOfMonth() < 10 ? "0"+date.getDayOfMonth() : ""+date.getMonthValue());
        return base + "-" + date.getYear() + "-" + twoDigitMonth + "-" + twoDigitDay + ".log";
    }

    @SuppressWarnings("SameParameterValue")
    static void setPropertiesFromFile(String filename) throws URISyntaxException, IOException {
        FileInputStream propFile = new FileInputStream(getFile(filename));
        Properties p = new Properties(System.getProperties());
        p.load(propFile);
        System.setProperties(p);
    }

    private static void testProperties() {
        String calendarId = System.getProperty("calendar.id");
        String sender = System.getProperty("notifier.sender");
        String senderPassword = System.getProperty("notifier.sender.password");
        String recipient = System.getProperty("notifier.recipient");
        if(calendarId == null || sender == null || senderPassword == null || recipient == null) {
            throw new IllegalStateException("A required property was not set. Their values were: " +
                    "calendar.id = " + calendarId +
                    "notifier.sender = " + sender +
                    "notifier.sender.password = [redacted]" +
                    "notifier.recipient = " + recipient);
        }
        logger.info("Looks good.");
    }

    /**
     * Add launches to calendar or update those that are already in the calendar
     */
    private static void addLaunchesToCalendar(List<Launch> scheduledLaunches, Calendar calendar, String calendarId) throws IOException {
        List<Event> calendarEvents = getCalendarEvents(calendar, calendarId);

        for (Launch scheduledLaunch : scheduledLaunches) {
            if(scheduledLaunch != null) {
                boolean launchInCalendar = isInCalendar(scheduledLaunch, calendarEvents);
                if (launchInCalendar) {
                    logger.debug("Launch is in calendar.");
                } else {
                    logger.debug("Launch not found in calendar.");
                }

                ZonedDateTime time = scheduledLaunch.getTime();
                int window = scheduledLaunch.getWindow();

                // start
                EventDateTime start = new EventDateTime()
                        .setDateTime(new DateTime(new Date(time.toInstant().toEpochMilli()), TimeZone.getTimeZone(time.getZone())))
                        .setTimeZone("UTC");

                // end
                EventDateTime end = new EventDateTime()
                        .setDateTime(new DateTime(new Date(time.toInstant().toEpochMilli() +
                                (window > 0 ?  window * 1000 : 30*60*1000)), TimeZone.getTimeZone(time.getZone())))
                        .setTimeZone("UTC");

                // set source
                Event.Source source = new Event.Source()
                        .setTitle("Launch Cal")
                        .setUrl("https://github.com/thomaspaulin/launch-cal/");

                // creator
                final Event.Creator creator = new Event.Creator()
                        .setDisplayName(System.getProperty("calendar.creator.name"))
                        .setEmail(System.getProperty("calendar.creator.email"));

                // create event
                String description = scheduledLaunch.getDescription() + "<br><br>See <a href=\"http://www.spaceflightinsider.com/launch-schedule/\">http://www.spaceflightinsider.com/launch-schedule/</a> " +
                        "for more information.";

                final Event launchEvent = new Event()
                        .setSummary(Launch.createSummary(scheduledLaunch))
                        .setStart(start)
                        .setEnd(end)
                        .setLocation(scheduledLaunch.getLocation())
                        .setSource(source)
                        .setCreator(creator)
                        .setGuestsCanInviteOthers(true)
                        .setGuestsCanModify(false)
                        .setGuestsCanSeeOtherGuests(false)
                        .setDescription(description);

                if (!launchInCalendar) {
                    logger.debug("Inserting launch " + launchEvent + " into calendar.");
                    calendar.events()
                            .insert(calendarId, launchEvent)
                            .setSendNotifications(false)
                            .execute();
                } else {
                    // need to look up events in calendar to find which one to update
                    Event matchingEvent = getMatchingLaunchEvent(scheduledLaunch, calendarEvents);
                    if (matchingEvent != null) {
                        logger.debug("Updating calendar event where ID is " + matchingEvent.getId());
                        deleteDuplicateLaunches(matchingEvent.getId(), scheduledLaunch, calendar, calendarId);
                        calendar.events()
                                .update(calendarId, matchingEvent.getId(), launchEvent)
                                .setSendNotifications(false)
                                .execute();
                    }
                }
            }
        }
    }

    private static List<Event> getCalendarEvents(Calendar calendar, String calendarId) throws IOException {
        Events events = calendar.events().list(calendarId)
                .setTimeMin(new DateTime(new Date(Instant.now().minusSeconds(172800).toEpochMilli())))     // look back 2 days
                .setTimeMax(new DateTime(new Date(Instant.now().plusSeconds(31536000).toEpochMilli())))    // look forwards 365 days
                .setTimeZone("UTC")
                .execute();
        return events.getItems();
    }

    private static void testCalendarCredentials(Calendar calendar, String calendarId) throws IOException {
        logger.debug("Using calendar ID of " + calendarId);
        calendar.events().list(calendarId)
                .setTimeMin(new DateTime(new Date()))
                .setTimeMax(new DateTime(new Date()))
                .execute();
        logger.info("Looks good.");
    }

    /**
     * This is being run from within a loop. Shitty performance
     */
    private static Event getMatchingLaunchEvent(Launch launch, List<Event> events) {
        for (Event event : events) {
            if(launch.is(event)) return event;
        }
        return null;
    }

    /**
     * Check there are no other launches that should be considered equal
     */
    private static void deleteDuplicateLaunches(final String eventId, Launch launch, Calendar calendar, String calendarId) throws IOException {
        List<Event> calendarEvents = getCalendarEvents(calendar, calendarId);
        if(eventId == null) throw new IllegalArgumentException("Event ID should not be null");
        for (Event calendarEvent : calendarEvents) {
            if(launch.is(calendarEvent) && !calendarEvent.getId().equals(eventId)) {
                logger.debug("Deleting duplicate event " + calendarEvent);
                calendar.events().delete(calendarId, calendarEvent.getId()).execute();
            }
        }
    }

    private static boolean isInCalendar(Launch launch, List<Event> events) {
        logger.debug("Checking if launch " + launch + " is in calendar.");
        return getMatchingLaunchEvent(launch, events) != null;
    }

    //================================================================================================================//
    // Google calendar boilerplate
    //================================================================================================================//

    private static final String APPLICATION_NAME =
            "Space Launch Parser";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/launch_cal_cred.json");

//    /** Global instance of the {@link FileDataStoreFactory}. */
//    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static com.google.api.services.calendar.Calendar getCalendarService() throws IOException {
        Credential credential = authorize();
        return new com.google.api.services.calendar.Calendar.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException if the secret can't be loaded for some reason
     */
    private static Credential authorize() throws IOException {

        GoogleCredential credential = GoogleCredential.fromStream(ClassLoader.getSystemResourceAsStream("client_secret.json"))
                .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));

        logger.info("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

}
