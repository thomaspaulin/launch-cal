package nz.paulin.spaceflight;

import com.google.api.services.calendar.model.Event;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a launch
 */
public class Launch {
    /**
     * The mission name
     */
    private final String missionName;
    /**
     * Launch site
     */
    private final String location;
    /**
     * When the launch window opens. null if unknown or still TBD
     */
    private final ZonedDateTime time;
    /**
     * Launch window in seconds
     */
    private final int window;
    /**
     * Description of the launch for a bit of extra flavour
     */
    private final String description;
    /**
     * The name of the launch vehicle
     */
    private final String launchVehicle;

    private Launch(Builder builder) {
        this.missionName = builder.missionName;
        this.location = builder.location;
        this.time = builder.time;
        this.window = builder.window;
        this.description = builder.description;
        this.launchVehicle = builder.launchVehicle;
    }

     String getMissionName() {
        return missionName;
    }

     String getLocation() {
        return location;
    }

     ZonedDateTime getTime() {
        return time;
    }

     int getWindow() {
        return window;
    }

     String getDescription() {
        return description;
    }

     String getLaunchVehicle() {
        return launchVehicle;
    }

    static String createSummary(Launch launch) {
        return launch.missionName;
    }

     boolean is(Event event) {
        return event.getSummary() != null && event.getSummary().equals(Launch.createSummary(this)) && location.equals(event.getLocation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Launch launch = (Launch) o;

        return missionName.equals(launch.missionName)
                && location.equals(launch.location)
                && launchVehicle.equals(launch.launchVehicle);
    }

    @Override
    public int hashCode() {
        int result = missionName.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + launchVehicle.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "[" + missionName + "] Launches at " + time + " from " + location + " on " + aOrAn(launchVehicle) + " " + launchVehicle + " with a " + window + " second launch window.";
    }

    private static String aOrAn(String nextWord) {
        nextWord = nextWord.toLowerCase();
        char c = nextWord.charAt(0);
        List<Character> vowels = Arrays.asList('a', 'e', 'i', 'o', 'u');
        if(vowels.contains(c)) {
            return "an";
        } else {
            return "a";
        }
    }

     @SuppressWarnings("UnusedReturnValue")
     static class Builder {
        private String missionName;
        private String location;
        private ZonedDateTime time;
        private int window;
        private String description;
        private String launchVehicle;

         Builder() {
            this.missionName = null;
            this.location = null;
            this.time = null;
            this.window = 0;
            this.description = null;
            this.launchVehicle = null;
        }

         Builder setMissionName(String missionName) {
            this.missionName = missionName;
            return this;
        }

         Builder setLocation(String location) {
            this.location = location;
            return this;
        }

         Builder setTime(ZonedDateTime time) {
            this.time = time;
            return this;
        }

         Builder setWindow(int window) {
            this.window = window;
            return this;
        }

         Builder setDescription(String description) {
            this.description = description;
            return this;
        }

         Builder setLaunchVehicle(String launchVehicle) {
            this.launchVehicle = launchVehicle;
            return this;
        }

         Launch build() {
            return new Launch(this);
        }
    }
}
