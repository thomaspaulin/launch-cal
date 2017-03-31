package nz.paulin.spaceflight;

import java.time.ZonedDateTime;

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

    public String getMissionName() {
        return missionName;
    }

    public String getLocation() {
        return location;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public int getWindow() {
        return window;
    }

    public String getDescription() {
        return description;
    }

    public String getLaunchVehicle() {
        return launchVehicle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Launch launch = (Launch) o;

        if (!missionName.equals(launch.missionName)) return false;
        if (!location.equals(launch.location)) return false;
        return launchVehicle.equals(launch.launchVehicle);
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
        return "[" + missionName + "] Launches at " + time + " from " + location + " on a " + launchVehicle + " with a " + window + " second launch window.";
    }

    public static class Builder {
        private String missionName;
        private String location;
        private ZonedDateTime time;
        private int window;
        private String description;
        private String launchVehicle;

        public Builder() {
            this.missionName = null;
            this.location = null;
            this.time = null;
            this.window = 0;
            this.description = null;
            this.launchVehicle = null;
        }

        public Builder setMissionName(String missionName) {
            this.missionName = missionName;
            return this;
        }

        public Builder setLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder setTime(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder setWindow(int window) {
            this.window = window;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setLaunchVehicle(String launchVehicle) {
            this.launchVehicle = launchVehicle;
            return this;
        }

        public Launch build() {
            return new Launch(this);
        }
    }
}
