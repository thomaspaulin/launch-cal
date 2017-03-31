package nz.paulin.spaceflight;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Parses the launch schedule on http://www.spaceflightinsider.com and then saves the launches with times into the given calendar.
 */
public class LaunchCal {
    private static String CALENDAR_ID = null;

    private static final Logger logger = LogManager.getLogger(LaunchCal.class);
    //todo fix date parsing
    //todo calendar entry
    //todo update existing entries correctly when information become available
    //todo convert location into a format that can be shown on Google Maps

    public static void main(String[] args) {
        System.getProperty("log4j.configurationFile", "log4j2.xml");
        try {
            URL propUrl = LaunchCal.class.getResource("properties.txt");
            FileInputStream propFile =   new FileInputStream(new File(propUrl.toURI()));
            Properties p = new Properties(System.getProperties());
            p.load(propFile);
            System.setProperties(p);

            CALENDAR_ID = System.getProperty("calendar.id") + "@group.calendar.google.com";

//            logger.info("Getting calendar...");
//            Calendar calendar = getCalendarService();
            logger.info("Parsing launches...");
            List<Launch> launches = Parser.parseLaunches(new URL("http://www.spaceflightinsider.com/launch-schedule/"));
            logger.info("Adding launches to calendar...");
//            addLaunchesToCalendar(launches, calendar);
            logger.info("Done.");
            logger.info("------------------------------------------");
        } catch (Exception e) {
            final String from = System.getProperty("notifier.sender");
            final String to = System.getProperty("notifier.recipient");
            File todaysLogFile = new File("logs", "launch-cal.log");
            logger.info("An error occurred, notifying...");
            Notifier.sendEmail(from, to, "Error thrown by launch-cal", e, Collections.singletonList(todaysLogFile));
            logger.info("Done.");
            logger.info("------------------------------------------");
        }
    }

//    /**
//     * Add launches to calendar or update those that are already in the calendar
//     */
//    static void addLaunchesToCalendar(List<Launch> scheduledLaunches, Calendar calendar) throws IOException {
//        List<Event> calendarEvents = getCalendarEvents(calendar);
//
//        for (Launch scheduledLaunch : scheduledLaunches) {
//            if(scheduledLaunch != null) {
//                boolean launchInCalendar = isLaunchInCalendar(scheduledLaunch, calendarEvents);
//                if (launchInCalendar) {
//                    logger.debug("Launch is in calendar.");
//                } else {
//                    logger.debug("Launch not found in calendar.");
//                }
//
//                LaunchWindow launchWindow = scheduledLaunch.getLaunchWindow();
//
//                // start
//                EventDateTime start = new EventDateTime()
//                        .setDateTime(new DateTime(new Date(launchWindow.start.toEpochSecond() * 1000)))
//                        .setTimeZone("UTC");
//
//                // end
//                EventDateTime end = new EventDateTime()
//                        .setDateTime(new DateTime(new Date(launchWindow.end != null ?
//                                launchWindow.end.toEpochSecond() * 1000 :
//                                launchWindow.start.toEpochSecond() * 1000 + 1800)))
//                        .setTimeZone("UTC");
//
//                // create event
//                Event launchEvent = new Event()
//                        .setSummary(scheduledLaunch.getLaunchVehicle() + " Launch - " + scheduledLaunch.getMission())
//                        .setStart(start)
//                        .setEnd(end)
//                        .setLocation(scheduledLaunch.getLaunchSite())
//                        .setDescription(scheduledLaunch.getDescription());
//
//                if (!launchInCalendar) {
//                    logger.debug("Inserting launch " + launchEvent + " into calendar.");
//                    calendar.events().insert(CALENDAR_ID, launchEvent).setSendNotifications(true).execute();
//                } else {
//                    // need to look up events in calendar to find which one to update
//                    Event matchingEvent = getMatchingLaunchEvent(scheduledLaunch, calendarEvents);
//                    if (matchingEvent != null) {
//                        logger.debug("Updating calendar event where ID is " + matchingEvent.getId());
//                        deleteDuplicateLaunches(matchingEvent.getId(), scheduledLaunch, calendar);
//                        calendar.events().update(CALENDAR_ID, matchingEvent.getId(), launchEvent).setSendNotifications(true).execute();
//                    }
//                }
//            }
//        }
//    }
//
//    private static List<Event> getCalendarEvents(Calendar calendar) throws IOException {
//        Events events = calendar.events().list(CALENDAR_ID)
//                .setTimeMin(new DateTime(new Date(Instant.now().minus(172800, ChronoUnit.SECONDS).toEpochMilli())))     // look back 2 days
//                .setTimeMax(new DateTime(new Date(Instant.now().plus(31536000, ChronoUnit.SECONDS).toEpochMilli())))    // look forwards 365 days
//                .setTimeZone("UTC")
//                .execute();
//        return events.getItems();
//    }
//
//    /**
//     * This is being run from within a loop. Shitty performance
//     */
//    private static Event getMatchingLaunchEvent(Launch launch, List<Event> events) {
//        for (Event event : events) {
//            if(launch.equalsEvent(event)) return event;
//        }
//        return null;
//    }
//
//    /**
//     * Check there are no other launches that should be considered equal
//     */
//    private static void deleteDuplicateLaunches(final String eventId, Launch launch, Calendar calendar) throws IOException {
//        List<Event> calendarEvents = getCalendarEvents(calendar);
//        if(eventId == null) throw new IllegalArgumentException("Event ID should not be null");
//        for (Event calendarEvent : calendarEvents) {
//            if(launch.equalsEvent(calendarEvent) && !calendarEvent.getId().equals(eventId)) {
//                logger.debug("Deleting duplicate event " + calendarEvent);
//                calendar.events().delete(CALENDAR_ID, calendarEvent.getId()).execute();
//            }
//        }
//    }
//
//    private static boolean isLaunchInCalendar(Launch launch, List<Event> events) {
//        logger.debug("Checking if launch " + launch + " is in calendar.");
//        return getMatchingLaunchEvent(launch, events) != null;
//    }

    //================================================================================================================//
    // Google calendar boilerplate
    //================================================================================================================//

    private static final String APPLICATION_NAME =
            "Space Launch Parser";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/spaceflight_parser.json");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static com.google.api.services.calendar.Calendar getCalendarService() throws IOException {
        Credential credential = authorize();
        return new com.google.api.services.calendar.Calendar.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        GoogleCredential credential = GoogleCredential.fromStream(ClassLoader.getSystemResourceAsStream("private.json"))
                .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));

        System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

}
