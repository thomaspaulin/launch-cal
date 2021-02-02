package nz.paulin.spaceflight;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Parses the launch schedule on http://www.spaceflightinsider.com and then saves the launches with times into the given calendar.
 */
public class LaunchCal implements RequestHandler<Map<String,String>, String> {

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Handling request\n");
        try {

            String calendarId = System.getenv("CALENDAR_ID");
            String serviceAccountEmail = System.getenv("SERVICE_ACCOUNT_EMAIL");

            logger.log("Getting calendar\n");
            Calendar calendar = getCalendarService(serviceAccountEmail);
            logger.log("Testing calendar credentials\n");
            testCalendarCredentials(calendar, calendarId, logger);
            logger.log("Parsing launches\n");
            List<Launch> launches = Parser.parseLaunches(new URL("http://www.spaceflightinsider.com/launch-schedule/"), logger);
            logger.log("Adding launches to calendar\n");
            addLaunchesToCalendar(launches, calendar, calendarId, logger);

            logger.log("Request handled, going to bed now\n");
            return "Done";
        } catch (Exception e) {
            final String from = System.getenv("NOTIFIER_SENDER");
            final String to = System.getenv("NOTIFIER_RECIPIENT");
            logger.log(Notifier.getExceptionDescription(e));
            logger.log("An error occurred, notifying humans...");
            Notifier.sendEmail(from, to, "Error thrown by launch-cal", e, Collections.emptyList());
            logger.log("Done.");
            logger.log("------------------------------------------");
            return "Done";
        }
    }

    private static File getFile(String fileName) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
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

    /**
     * Add launches to calendar or update those that are already in the calendar
     */
    private static void addLaunchesToCalendar(List<Launch> scheduledLaunches, Calendar calendar, String calendarId, LambdaLogger logger) throws IOException {
        List<Event> calendarEvents = getCalendarEvents(calendar, calendarId);

        for (Launch scheduledLaunch : scheduledLaunches) {
            if(scheduledLaunch != null) {
                boolean launchInCalendar = isInCalendar(scheduledLaunch, calendarEvents);
                if (launchInCalendar) {
                    logger.log("Launch is in calendar.\n");
                } else {
                    logger.log("Launch not found in calendar.\n");
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
                        .setUrl("https://launch-cal.thomaspaulin.me");

                // creator
//                final Event.Creator creator = new Event.Creator()
//                        .setDisplayName(System.getProperty("calendar.creator.name"))
//                        .setEmail(System.getProperty("calendar.creator.email"));

                // create event
                String description = scheduledLaunch.getDescription() + "<br><br>See <a href=\"http://www.spaceflightinsider.com/launch-schedule/\">http://www.spaceflightinsider.com/launch-schedule/</a> " +
                        "for more information.";

                final Event launchEvent = new Event()
                        .setSummary(Launch.createSummary(scheduledLaunch))
                        .setStart(start)
                        .setEnd(end)
                        .setLocation(scheduledLaunch.getLocation())
                        .setSource(source)
//                        .setCreator(creator)
                        .setGuestsCanInviteOthers(true)
                        .setGuestsCanModify(false)
                        .setGuestsCanSeeOtherGuests(false)
                        .setDescription(description);

                if (!launchInCalendar) {
                    calendar.events()
                            .insert(calendarId, launchEvent)
                            .setSendNotifications(false)
                            .execute();
                } else {
                    // need to look up events in calendar to find which one to update
                    Event matchingEvent = getMatchingLaunchEvent(scheduledLaunch, calendarEvents);
                    if (matchingEvent != null) {
                        deleteDuplicateLaunches(matchingEvent.getId(), scheduledLaunch, calendar, calendarId, logger);
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

    private static void testCalendarCredentials(Calendar calendar, String calendarId, LambdaLogger logger) throws IOException {
        logger.log(String.format("Using calendar ID of %s%n", calendarId));
        calendar.events().list(calendarId)
                .setTimeMin(new DateTime(new Date()))
                .setTimeMax(new DateTime(new Date()))
                .execute();
    }

    /**
     * This is being run from within a loop. Poor performance
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
    private static void deleteDuplicateLaunches(final String eventId, Launch launch, Calendar calendar, String calendarId, LambdaLogger logger) throws IOException {
        List<Event> calendarEvents = getCalendarEvents(calendar, calendarId);
        if(eventId == null) throw new IllegalArgumentException("Event ID should not be null");
        for (Event calendarEvent : calendarEvents) {
            if(launch.is(calendarEvent) && !calendarEvent.getId().equals(eventId)) {
                logger.log(String.format("Deleting duplicate event %s%n", calendarEvent));
                calendar.events().delete(calendarId, calendarEvent.getId()).execute();
            }
        }
    }

    private static boolean isInCalendar(Launch launch, List<Event> events) {
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

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static com.google.api.services.calendar.Calendar getCalendarService(String serviceAccountEmail) throws IOException, GeneralSecurityException, URISyntaxException {
        Credential credential = authorize(serviceAccountEmail);
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
    private static Credential authorize(String serviceAccountEmail) throws IOException, GeneralSecurityException {
        final File p12File = getFile("launch-cal.p12");

        final GoogleCredential.Builder credentialBuilder = new GoogleCredential.Builder();
        final GoogleCredential credential = credentialBuilder
                .setServiceAccountId(serviceAccountEmail)
                .setServiceAccountPrivateKeyFromP12File(p12File)
//                .setServiceAccountUser("Launch Cal Service Account")
//                .setServiceAccountPrivateKeyId("cdc6bfc801b112587eca26e1a044f160d54e9cb89f031058ed9d6f55d28d8ca95bb8cabfc888cd5e991bf90020eef0378581e0d923c8d17e031883d0")
                // https://developers.google.com/identity/protocols/oauth2/scopes#calendar
                .setServiceAccountScopes(Collections.singletonList(CalendarScopes.CALENDAR))
                .setJsonFactory(Utils.getDefaultJsonFactory())
                .setTransport(Utils.getDefaultTransport())
                .build();

        System.out.println(String.format("Credentials saved to %s", DATA_STORE_DIR.getAbsolutePath()));
        return credential;
    }

}
